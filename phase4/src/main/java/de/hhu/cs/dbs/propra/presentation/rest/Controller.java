package de.hhu.cs.dbs.propra.presentation.rest;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.sqlite.SQLiteException;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.sql.*;
import java.util.*;


@Path("/")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public class Controller {
	@Inject
	private DataSource dataSource;

	@Context
	private SecurityContext securityContext;

	@Context
	private UriInfo uriInfo;


	@Path("kunden")
	@GET
	public List<Map<String, Object>> getKunde(@QueryParam("beitragsbefreit") Boolean beitragsbefreit,
	                                          @QueryParam("guthaben") Double guthaben) throws SQLException {
		ArrayList<Object> params= new ArrayList<>(Arrays.asList(beitragsbefreit, guthaben));
		Connection connection = dataSource.getConnection();
		String sql = "SELECT Kunde.ROWID, * FROM Kunde JOIN Nutzer USING (Email) "
				+ "WHERE Befreiung = ? AND Guthaben >= ? "
				+ "OR (? IS NULL AND Guthaben >= ?) "
				+ "OR (Befreiung = ? AND ? IS NULL) "
				+ "OR (? IS NULL AND ? IS NULL);";

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, beitragsbefreit);
		preparedStatement.setObject(2, guthaben);
		preparedStatement.setObject(3, beitragsbefreit);
		preparedStatement.setObject(4, guthaben);
		preparedStatement.setObject(5, beitragsbefreit);
		preparedStatement.setObject(6, guthaben);
		preparedStatement.setObject(7, beitragsbefreit);
		preparedStatement.setObject(8, guthaben);

		ResultSet resultSet = preparedStatement.executeQuery();

		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;

		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("kundenid", resultSet.getObject(1));
			entity.put("email", resultSet.getObject(2));
			entity.put("guthaben", resultSet.getObject(3));
			entity.put("befreiung", resultSet.getBoolean(4));
			entity.put("vorname", resultSet.getObject(6));
			entity.put("nachname", resultSet.getObject(7));
			entity.put("geburtsdatum", (resultSet.getString(8)
					.substring(0, resultSet.getString(8).length() - 9)));
			entities.add(entity);
		}

		resultSet.close();
		connection.close();

		return entities;
	}


	@Path("kunden")
	@POST
	public Response postKunde(@FormDataParam("email") String email,
	                          @FormDataParam("passwort") String passwort,
	                          @FormDataParam("vorname") String vorname,
	                          @FormDataParam("nachname") String nachname,
	                          @FormDataParam("geburtsdatum") String geburtsdatum,
	                          @FormDataParam("guthaben") Double guthaben,
	                          @FormDataParam("beitragsbefreit") Boolean beitragsbefreit,
	                          @FormDataParam("adresseid") Integer adresseid) throws SQLException {

		if (email == null) return Response.status(400).entity("Email darf nicht NULL sein").build();
		if (passwort == null) return Response.status(400).entity("Passwort darf nicht NULL sein").build();
		if (vorname == null) return Response.status(400).entity("Vorname darf nicht NULL sein").build();
		if (nachname == null) return Response.status(400).entity("Nachname darf nicht NULL sein").build();
		if (geburtsdatum == null) return Response.status(400).entity("Geburtsdatum darf nicht NULL sein").build();
		if (guthaben == null) return Response.status(400).entity("Guthaben darf nicht NULL sein").build();
		if (beitragsbefreit == null) return Response.status(400).entity("Beitragsbefreit darf nicht NULL sein").build();
		if (adresseid == null) return Response.status(400).entity("AdresseID darf nicht NULL sein").build();

		Connection connection = dataSource.getConnection();
		final boolean oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);


		String sqlInsert1= "INSERT INTO Nutzer (Email, Name, Nachname, Geburtsdatum, Passwort) VALUES  (?,?,?,?,?);";
		String sqlInsert2=	"INSERT INTO Kunde (Email, Guthaben, Befreiung, Adresse_id) VALUES (?,?,?,?) ";

		PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert1);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, email);
		preparedStatement.setObject(2, vorname);
		preparedStatement.setObject(3, nachname);
		preparedStatement.setObject(4, (geburtsdatum + " 00:00:00"));
		preparedStatement.setObject(5, passwort);

		PreparedStatement preparedStatement2 = connection.prepareStatement(sqlInsert2);
		preparedStatement2.closeOnCompletion();
		preparedStatement2.setObject(1, email);
		preparedStatement2.setObject(2, guthaben);
		preparedStatement2.setObject(3, beitragsbefreit);
		preparedStatement2.setObject(4, adresseid);
		String id = null;

		try{
			preparedStatement.executeUpdate();
			preparedStatement2.executeUpdate();
			ResultSet generatedKeys = preparedStatement2.getGeneratedKeys();

			if (generatedKeys.next()) {
				id = generatedKeys.getString(1);
			}

			generatedKeys.close();

			return Response.created(uriInfo.getAbsolutePathBuilder()
					.path(id)
					.build())
					.build();

		} catch (SQLiteException e) {
			connection.rollback();

			return Response.status(400).entity("Prüfe die Eingabe, irgendwas scheint falsch zu sein").build();
		} finally {
			connection.commit();
			connection.setAutoCommit(oldAutoCommit);
			connection.close();
		}

	}


	@Path("bibliothekare")
	@GET
	public List<Map<String, Object>> getBiblio(@QueryParam("telefonnummer") String telefonnummer) throws SQLException {
		Connection connection = dataSource.getConnection();
		String sql = "SELECT Bibliothekar.ROWID ,* FROM Bibliothekar JOIN Nutzer USING (Email) " +
				"WHERE Telefonnummer LIKE ? OR ? IS NULL;";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, ("%" + telefonnummer + "%"));
		preparedStatement.setObject(2, telefonnummer);
		ResultSet resultSet = preparedStatement.executeQuery();
		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;
		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("bibliothekarid", resultSet.getObject(1));
			entity.put("telefonnummer", resultSet.getObject(3));
			entity.put("email", resultSet.getObject(2));
			entity.put("passwort", resultSet.getObject(7));
			entity.put("vorname", resultSet.getObject(4));
			entity.put("nachname", resultSet.getObject(5));
			entity.put("geburtsdatum", resultSet.getString(6)
					.substring(0, resultSet.getString(6).length() - 9));
			entities.add(entity);
		}
		resultSet.close();
		connection.close();
		return entities;
	}


	@Path("bibliothekare")
	@POST
	public Response postBiblio(@FormDataParam("email") String email,
	                          @FormDataParam("passwort") String passwort,
	                          @FormDataParam("vorname") String vorname,
	                          @FormDataParam("nachname") String nachname,
	                          @FormDataParam("geburtsdatum") String geburtsdatum,
	                          @FormDataParam("telefonnummer") String telefonnummer) throws SQLException {

		if (email == null) return Response.status(400).entity("Email darf nicht NULL sein").build();
		if (passwort == null) return Response.status(400).entity("Passwort darf nicht NULL sein").build();
		if (vorname == null) return Response.status(400).entity("Vorname darf nicht NULL sein").build();
		if (nachname == null) return Response.status(400).entity("Nachname darf nicht NULL sein").build();
		if (geburtsdatum == null) return Response.status(400).entity("Geburtsdatum darf nicht NULL sein").build();
		if (telefonnummer == null) return Response.status(400).entity("Telefonnummer darf nicht NULL sein").build();

		Connection connection = dataSource.getConnection();
		final boolean oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);

		String sqlInsert1= "INSERT INTO Nutzer (Email, Name, Nachname, Geburtsdatum, Passwort) VALUES  (?,?,?,?,?);";
		String sqlInsert2=	"INSERT INTO Bibliothekar (Email, Telefonnummer) VALUES (?,?)";

		PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert1);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, email);
		preparedStatement.setObject(2, vorname);
		preparedStatement.setObject(3, nachname);
		preparedStatement.setObject(4, (geburtsdatum + " 00:00:00"));
		preparedStatement.setObject(5, passwort);

		PreparedStatement preparedStatement2 = connection.prepareStatement(sqlInsert2);
		preparedStatement2.closeOnCompletion();
		preparedStatement2.setObject(1, email);
		preparedStatement2.setObject(2, telefonnummer);

		try{
			preparedStatement.executeUpdate();
			preparedStatement2.executeUpdate();

		} catch (SQLiteException e){
			connection.rollback();

			return Response.status(400).entity("Prüfe die Eingabe, irgendwas scheint falsch zu sein").build();
		} finally {
			connection.commit();
			connection.setAutoCommit(oldAutoCommit);
			connection.close();
		}

		return Response.created(uriInfo.getAbsolutePathBuilder()
						.queryParam("telefonnummer",telefonnummer)
						.build())
						.build();
	}


	@Path("genres")
	@GET
	public List<Map<String, Object>> getGenre(@QueryParam("bezeichnung") String bezeichnung) throws SQLException {
		Connection connection = dataSource.getConnection();

		String sql = "SELECT Genre.ROWID, * FROM Genre WHERE Bezeichnung=? OR ? IS NULL";

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, bezeichnung);
		preparedStatement.setObject(2, bezeichnung);
		ResultSet resultSet = preparedStatement.executeQuery();

		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;

		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("genreid", resultSet.getObject(1));
			entity.put("bezeichnung", resultSet.getObject(2));
			entities.add(entity);
		}
		resultSet.close();
		connection.close();
		return entities;
	}


	@Path("artikel")
	@GET
	public List<Map<String, Object>> getArtikel(@QueryParam("isbn") String isbn,
	                                            @QueryParam("bezeichnung") String bezeichnung,
	                                            @QueryParam("beschreibung") String beschreibung,
	                                            @QueryParam("coverbild") String coverbild,
	                                            @QueryParam("erscheinungsdatum") String erscheinungsdatum)
		throws SQLException {
		if (erscheinungsdatum !=null) {
			erscheinungsdatum += " 00:00:00";
		}

		Connection connection = dataSource.getConnection();
		String sql = "SELECT Artikel.ROWID, * FROM Artikel WHERE 1=1"; // 1=1 um AND setzen zu koennen

		String ISBN = null;
		if (isbn != null) {
			sql += " AND ISBN LIKE ? COLLATE NOCASE";
			ISBN = "%" + isbn + "%";
		}

		String BEZEICHNUNG = null;
		if(bezeichnung != null) {
			sql += " AND Bezeichnung LIKE ? COLLATE NOCASE";
			BEZEICHNUNG = "%" + bezeichnung + "%";
		}

		String BESCHREIBUNG = null;
		if (beschreibung != null) {
			sql += " AND Beschreibung LIKE ? COLLATE NOCASE";
			BESCHREIBUNG = "%" + beschreibung + "%";
		}

		String COVERBILD = null;
		if (coverbild != null) {
			sql += " AND Coverbild LIKE ? COLLATE NOCASE";
			COVERBILD = "%" + coverbild + "%";
		}

		if (erscheinungsdatum != null) {
			sql += " AND Erscheinungsdatum >= ?";
		}

		ArrayList<Object> params= new ArrayList<>(Arrays
				.asList(ISBN, BESCHREIBUNG, BEZEICHNUNG, COVERBILD, erscheinungsdatum));

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();

		int index = 1;
		for (Object param : params) {
			if (param != null) {
				preparedStatement.setObject(index, param);
				index++;
			}
		}
		ResultSet resultSet = preparedStatement.executeQuery();

		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;

		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("artikelid", resultSet.getObject(1));
			entity.put("isbn", resultSet.getObject(2));
			entity.put("bezeichnung", resultSet.getObject(3));
			entity.put("beschreibung", resultSet.getObject(4));
			entity.put("coverbild", resultSet.getObject(5));
			entity.put("erscheinungsdatum", resultSet.getString(6)
					.substring(0, resultSet.getString(6).length() - 9));
			entities.add(entity);
		}

		resultSet.close();
		connection.close();

		return entities;
	}


	@Path("exemplare")
	@GET
	public List<Map<String, Object>> getExemplarAusleihe(@QueryParam("preis") Double preis,
	                                                     @QueryParam("ausgeliehen") Boolean ausgeliehen)
														throws SQLException {
		Connection connection = dataSource.getConnection();

		String sql = "SELECT Exemplar.ROWID, Ausleihe.ROWID, Anschaffungspreis "
				+ "FROM Exemplar LEFT JOIN Ausleihe USING (Exemplar_id) "
				+ "WHERE Anschaffungspreis >= ?  AND Zurueckgegeben = ? "
				+ "OR (Anschaffungspreis >= ? AND ? IS NULL) "
				+ "OR (? IS NULL AND Zurueckgegeben = ?) "
				+ "OR (? IS NULL AND ? IS NULL)";

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, preis);
		preparedStatement.setObject(2, ausgeliehen);
		preparedStatement.setObject(3, preis);
		preparedStatement.setObject(4, ausgeliehen);
		preparedStatement.setObject(5, preis);
		preparedStatement.setObject(6, ausgeliehen);
		preparedStatement.setObject(7, preis);
		preparedStatement.setObject(8, ausgeliehen);
		ResultSet resultSet = preparedStatement.executeQuery();

		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;

		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("exemplarid", resultSet.getObject(1));
			entity.put("artikelid", resultSet.getObject(2));
			entity.put("preis", resultSet.getObject(3));

			entities.add(entity);
		}

		resultSet.close();
		connection.close();

		return entities;
	}


	@Path("mitarbeiter")
	@GET
	public Response getMitarbeiter() {
		return Response.status(301).location(URI.create("/bibliothekare")).build();
	}


	@Path("autoren")
	@GET
	public List<Map<String, Object>> getExemplar(@QueryParam("vorname") String vorname,
	                                             @QueryParam("nachname") String nachname)
												throws SQLException {
		Connection connection = dataSource.getConnection();

		String sql = "SELECT ROWID, * "
				+ "FROM Autor "
				+ "WHERE Vorname = ?  COLLATE NOCASE AND Nachname = ? COLLATE NOCASE "
				+ "OR (Vorname = ? COLLATE NOCASE AND ? IS NULL) "
				+ "OR (? IS NULL AND Nachname = ? COLLATE NOCASE) "
				+ "OR (? IS NULL AND ? IS NULL)";

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, vorname);
		preparedStatement.setObject(2, nachname);
		preparedStatement.setObject(3, vorname);
		preparedStatement.setObject(4, nachname);
		preparedStatement.setObject(5, vorname);
		preparedStatement.setObject(6, nachname);
		preparedStatement.setObject(7, vorname);
		preparedStatement.setObject(8, nachname);
		ResultSet resultSet = preparedStatement.executeQuery();

		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;

		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("autorid", resultSet.getObject(1)); //TODO Was macht getRowId und getRow?
			entity.put("vorname", resultSet.getObject(3));
			entity.put("nachname", resultSet.getObject(4));
			entities.add(entity);
		}

		resultSet.close();
		connection.close();

		return entities;
	}


	@Path("adressen")
	@GET
	public List<Map<String, Object>> getAdressen(@QueryParam("hausnummer") String hausnummer,
	                                             @QueryParam("strasse") String strasse,
	                                             @QueryParam("plz") String plz,
	                                             @QueryParam("stadt") String stadt) throws SQLException {

		Connection connection = dataSource.getConnection();
		String sql = "SELECT Adresse.ROWID, * FROM Adresse WHERE 1=1"; // 1 um AND setzen zu koennen, hat anscheinend keinen Einfluss in SQL

		String HAUSNUMMER = null;
		if (hausnummer != null) {
			sql += " AND Hausnummer LIKE ? COLLATE NOCASE";
			HAUSNUMMER = "%" + hausnummer + "%";
		}

		String STRASSE = null;
		if(strasse != null) {
			sql += " AND Strasse LIKE ? COLLATE NOCASE";
			STRASSE = "%" + strasse + "%";
		}

		String PLZ = null;
		if (plz != null) {
			sql += " AND PLZ LIKE ? COLLATE NOCASE";
			PLZ = "%" + plz + "%";
		}

		String STADT = null;
		if (stadt != null) {
			sql += " AND Stadt LIKE ? COLLATE NOCASE";
			STADT = "%" + stadt + "%";
		}

		ArrayList<Object> params= new ArrayList<>(Arrays
				.asList(HAUSNUMMER, STRASSE, PLZ, STADT));

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();

		int index = 1;
		for (Object param : params) {
			if (param != null) {
				preparedStatement.setObject(index, param);
				index++;
			}
		}
		ResultSet resultSet = preparedStatement.executeQuery();

		List<Map<String, Object>> entities = new ArrayList<>();
		Map<String, Object> entity;

		while (resultSet.next()) {
			entity = new LinkedHashMap<>();
			entity.put("adresseid", resultSet.getObject(1));
			entity.put("hausnummer", resultSet.getObject(3));
			entity.put("strasse", resultSet.getObject(4));
			entity.put("stadt", resultSet.getObject(5));
			entity.put("plz", resultSet.getObject(6));

			entities.add(entity);
		}

		resultSet.close();
		connection.close();

		return entities;
	}


}