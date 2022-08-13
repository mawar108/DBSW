package de.hhu.cs.dbs.propra.presentation.rest;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.sqlite.SQLiteException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.*;




@Path("/")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public class BibliothekarController {


	@Inject
	private DataSource dataSource;

	@Context
	private SecurityContext securityContext;

	@Context
	private UriInfo uriInfo;


	@Path("genres")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@POST
	public Response postGenres(@FormDataParam("bezeichnung") String bezeichnung) throws SQLException {
		if (bezeichnung == null) return Response.status(400).entity("Bezeichnung darf nicht NULL sein").build();

		Connection connection = dataSource.getConnection();

		String sqlInsert = "INSERT INTO Genre (Bezeichnung) VALUES (?);";

		PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert);
		preparedStatement.setObject(1, bezeichnung);

		try {
			preparedStatement.executeUpdate();
		} catch(SQLiteException e){ //Eigentlich unmöglich hier hin zu kommen (?)
		connection.close();

		return Response.status(400).entity("Irgendwas stimmt mit der Eingabe nicht").build();
		}

		connection.close();

		return Response.created(uriInfo.getAbsolutePathBuilder()
				.queryParam("bezeichnung",bezeichnung)
				.build())
				.build();
	}


	@Path("artikel")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@POST
	public Response postArtikel(@FormDataParam("autorid") Integer autorid,
	                            @FormDataParam("genreid") Integer genreid,
	                            @FormDataParam("mediumid") Integer mediumid,
	                            @FormDataParam("isbn") String isbn,
	                            @FormDataParam("erscheinungsdatum") String erscheinungsdatum,
	                            @FormDataParam("beschreibung") String beschreibung,
	                            @FormDataParam("bezeichnung") String bezeichnung,
	                            @FormDataParam("coverbild") String coverbild) throws SQLException {
		if (autorid == null) return Response.status(400).entity("AutoriD darf nicht NULL sein").build();
		if (genreid == null) return Response.status(400).entity("GenreID darf nicht NULL sein").build();
		if (mediumid == null) return Response.status(400).entity("MediumID darf nicht NULL sein").build();
		if (isbn == null) return Response.status(400).entity("ISBN darf nicht NULL sein").build();
		if (erscheinungsdatum == null) return Response.status(400).entity("Erscheinungsdatum darf nicht NULL sein").build();
		if (beschreibung == null) return Response.status(400).entity("Beschreibung darf nicht NULL sein").build();
		if (bezeichnung == null) return Response.status(400).entity("Bezeichnung darf nicht NULL sein").build();

		Connection connection = dataSource.getConnection();
		final boolean oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);

		//"Wandelt" die GenreRowID in eine Bezeichnung um
		String sql = "SELECT Bezeichnung FROM Genre WHERE Genre.ROWID = ? COLLATE NOCASE";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, genreid);
		ResultSet resultSet = preparedStatement.executeQuery();
		String genreBezeichnung = null;
		if (resultSet.next()) {
			genreBezeichnung = resultSet.getString("Bezeichnung");
		}


		//"Wandelt" die MediumRowID in ein Medium um
		String getBezeichnung = "SELECT Medium FROM Medium WHERE Medium.ROWID = ? COLLATE NOCASE";
		PreparedStatement preparedStatement2 = connection.prepareStatement(getBezeichnung);
		preparedStatement2.closeOnCompletion();
		preparedStatement2.setObject(1, mediumid);
		ResultSet resultSet2=  preparedStatement2.executeQuery();
		String medium = null;
		if (resultSet2.next()) {
			medium = resultSet2.getString("Medium");
		}

		//Sql Insert Befehle:
		String sqlInsert = "INSERT INTO Artikel(ISBN, Bezeichnung, Beschreibung, Coverbild, Erscheinungsdatum, Medium) "
				+ "VALUES (?,?,?,?,?,?);";

		String sqlInsert2 = "INSERT INTO gehoert (Bezeichnung, ISBN) VALUES (?,?);";

		String sqlInsert3 = "INSERT INTO verfasst (ISBN, Autor_id) VALUES (?,?);";

		//SqlInsert
		PreparedStatement preparedStatementInsert = connection.prepareStatement(sqlInsert);
		preparedStatementInsert.closeOnCompletion();
		preparedStatementInsert.setObject(1, isbn);
		preparedStatementInsert.setObject(2, bezeichnung);
		preparedStatementInsert.setObject(3, beschreibung);
		preparedStatementInsert.setObject(4, coverbild);
		preparedStatementInsert.setObject(5, (erscheinungsdatum + " 00:00:00"));
		preparedStatementInsert.setObject(6, medium);

		//SqlInsert2
		PreparedStatement preparedStatementInsert2 = connection.prepareStatement(sqlInsert2);
		preparedStatementInsert2.closeOnCompletion();
		preparedStatementInsert2.setObject(1, genreBezeichnung);
		preparedStatementInsert2.setObject(2, isbn);

		//SqlInsert3
		PreparedStatement preparedStatementInsert3 = connection.prepareStatement(sqlInsert3);
		preparedStatementInsert3.closeOnCompletion();
		preparedStatementInsert3.setObject(1, isbn);
		preparedStatementInsert3.setObject(2, autorid);


		try {
			preparedStatementInsert.executeUpdate();
			preparedStatementInsert2.executeUpdate();
			preparedStatementInsert3.executeUpdate();

		} catch(SQLiteException e){
			connection.rollback();

			return Response.status(400).entity("Irgendwas stimmt mit der Eingabe nicht").build();
		} finally {
			connection.commit();
			connection.setAutoCommit(oldAutoCommit);
			connection.close();
		}

		return Response.created(uriInfo.getAbsolutePathBuilder()
				.queryParam("isbn",isbn)
				.build())
				.build();
	}



	@Path("exemplare")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@POST
	public Response postExemplare(@FormDataParam("artikelid") Integer artikelid,
	                            @FormDataParam("preis") Double preis,
	                            @FormDataParam("regal") Integer regalnummer,
	                            @FormDataParam("etage") Integer etage) throws SQLException {
		if (artikelid == null) return Response.status(400).entity("ArtikelID darf nicht NULL sein").build();
		if (preis == null) return Response.status(400).entity("Preis darf nicht NULL sein").build();
		if (regalnummer == null) return Response.status(400).entity("Regalnummer darf nicht NULL sein").build();
		if (etage == null) return Response.status(400).entity("Etage darf nicht NULL sein").build();

		Connection connection = dataSource.getConnection();
		final boolean oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);

		//"Wandelt" die ArtikelId in eine ISBN um
		String sql = "SELECT ISBN FROM Artikel WHERE Artikel.ROWID = ?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, artikelid);
		ResultSet resultSet = preparedStatement.executeQuery();
		String isbn = null;
		if (resultSet.next()) {
			isbn = resultSet.getString("ISBN");
		}


		//Sucht die höhste Id und erhöht diese um eins, um mit keiner anderen Id zu kolladieren
		String getExempalarId = "SELECT Exemplar_id FROM Exemplar ORDER BY Exemplar_id DESC LIMIT 1";
		PreparedStatement preparedStatement2 = connection.prepareStatement(getExempalarId);
		preparedStatement2.closeOnCompletion();
		ResultSet resultSet2=  preparedStatement2.executeQuery();

		Integer exemplarId = 1;//falls keine ID existiert

		if (resultSet2.next()) {
			exemplarId = resultSet2.getInt("Exemplar_id") + 1;//erhöht die ID um eins
		}

		//holt die StandortId heraus wenn diese da ist, wenn nicht wird eine neue erstellt RowID = StandortID!
		String getStandortid = "SELECT Standort_id FROM Standort ORDER BY Standort_id DESC LIMIT 1;";
		PreparedStatement preparedStatement3 = connection.prepareStatement(getStandortid);
		preparedStatement3.closeOnCompletion();
		ResultSet resultSet3=  preparedStatement3.executeQuery();


		int standortId = 1;//falls keine ID exestiert

		if (resultSet3.next()) {
			standortId = resultSet3.getInt("Standort_id") + 1; //erhöht die ID um eins
		}

		String sqlInsertStandort = "INSERT INTO Standort (Standort_id, Etage, Regalnummer) VALUES (?,?,?)";
		PreparedStatement preparedStatementInsert = connection.prepareStatement(sqlInsertStandort);
		preparedStatementInsert.closeOnCompletion();
		preparedStatementInsert.setObject(1, standortId);
		preparedStatementInsert.setObject(2, etage);
		preparedStatementInsert.setObject(3, regalnummer);


		try {
			preparedStatementInsert.executeUpdate();
			ResultSet generatedKeys = preparedStatementInsert.getGeneratedKeys();

			if (generatedKeys.next()) {
				standortId = generatedKeys.getInt(1);
			}

			String sqlInsert = "INSERT INTO Exemplar (Exemplar_id, Anschaffungspreis, Standort_id, ISBN)"
					+ " VALUES (?,?,?,?);";
			PreparedStatement preparedStatementInsert2 = connection.prepareStatement(sqlInsert);
			preparedStatementInsert2.closeOnCompletion();
			preparedStatementInsert2.setObject(1, exemplarId);
			preparedStatementInsert2.setObject(2, preis);
			preparedStatementInsert2.setObject(3, standortId);
			preparedStatementInsert2.setObject(4, isbn);

			preparedStatementInsert2.executeUpdate();

		} catch (SQLiteException e) {
			connection.rollback();

			return Response.status(400).entity("Irgendwas stimmt mit der Eingabe nicht").build();
		} finally {
			connection.commit();
			connection.setAutoCommit(oldAutoCommit);
			connection.close();

		}

		return Response.created(uriInfo.getAbsolutePathBuilder()
				.path(String.valueOf(exemplarId))
				.build())
				.build();
	}


	@Path("autoren")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@POST
	public Response postAutor(@FormDataParam("vorname") String vorname,
	                          @FormDataParam("nachname") String nachanme) throws SQLException {
		if (vorname == null) return Response.status(400).entity("Vorname darf nicht NULL sein").build();
		if (nachanme == null) return Response.status(400).entity("Nachname darf nicht NULL sein").build();

		Connection connection = dataSource.getConnection();

		//Sucht die höhste Id und erhöht diese um eins, um mit keiner anderen Id zu kolladieren
		String getAutorId = "SELECT Autor_id FROM Autor ORDER BY Autor_id DESC LIMIT 1";
		PreparedStatement preparedStatement = connection.prepareStatement(getAutorId);
		preparedStatement.closeOnCompletion();
		ResultSet resultSet=  preparedStatement.executeQuery();

		Integer autorId = 1;//falls keine ID existiert

		if (resultSet.next()) {
			autorId = resultSet.getInt("Autor_id") + 1;//erhöht die ID um eins
		}

		String sqlInsert = "INSERT INTO Autor (Autor_id, Vorname, Nachname) VALUES (?,?,?);";

		PreparedStatement preparedStatement2 = connection.prepareStatement(sqlInsert);
		preparedStatement2.closeOnCompletion();
		preparedStatement2.setObject(1, autorId);
		preparedStatement2.setObject(2, vorname);
		preparedStatement2.setObject(3, nachanme);

		try {
			preparedStatement2.executeUpdate();
		} catch(SQLiteException e){ //TODO auch bei einem doppelten Eintrag?
			connection.close();

			return Response.status(400).entity("Irgendwas stimmt mit der Eingabe nicht").build();
		}

		connection.close();

		return Response.created(uriInfo.getAbsolutePathBuilder()
				.queryParam("vorname", vorname)
				.queryParam("nachanme", nachanme)
				.build())
				.build();
	}


	@Path("exemplare/{exemplarid}")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@DELETE
	public Response deleteExemplar(@PathParam("exemplarid") Integer exemplarid) throws SQLException {
		Connection connection = dataSource.getConnection();

		String checkID = "SELECT Exemplar_id FROM Exemplar WHERE Exemplar_id = ?";
		PreparedStatement preparedStatementCheck = connection.prepareStatement(checkID);
		preparedStatementCheck.closeOnCompletion();
		preparedStatementCheck.setObject(1, exemplarid);
		ResultSet resultSet = preparedStatementCheck.executeQuery();
		if(!resultSet.next()) {
			resultSet.close();
			connection.close();
			return Response.status(404).entity("Die ID existiert nicht").build();
		}
		resultSet.close();

		String sqlInsert = "DELETE FROM Exemplar WHERE Exemplar_id = ?;";

		PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, exemplarid);

		try {
			preparedStatement.executeUpdate();
		} catch(SQLiteException e){
			connection.close();

			return Response.status(404).entity("Der übergebene Parameter ist fehlerhaft").build();
		}

		connection.close();

		return Response.status(Response.Status.NO_CONTENT).build();
	}


	@Path("artikel/{artikelid}")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@DELETE
	public Response deleteArtikel(@PathParam("artikelid") Integer artikelid) throws SQLException {
		Connection connection = dataSource.getConnection();

		String checkID = "SELECT Artikel.ROWID FROM Artikel WHERE Artikel.ROWID = ?";
		PreparedStatement preparedStatementCheck = connection.prepareStatement(checkID);
		preparedStatementCheck.closeOnCompletion();
		preparedStatementCheck.setObject(1, artikelid);
		ResultSet resultSetCheck = preparedStatementCheck.executeQuery();
		if(!resultSetCheck.next()) {
			resultSetCheck.close();
			connection.close();
			return Response.status(404).entity("Die ID existiert nicht").build();
		}
		resultSetCheck.close();

		//ExemplarIDs werden rausgesucht die mit den ArtikelID des Param übereinstimmen
		String sqlGet = "SELECT Exemplar_id "
				+ "FROM Artikel JOIN Exemplar USING (ISBN) "
				+ "WHERE Artikel.ROWID = ?;";

		PreparedStatement preparedStatement = connection.prepareStatement(sqlGet);
		preparedStatement.setObject(1, artikelid);

		try {
			ResultSet resultSet = preparedStatement.executeQuery();

			while(resultSet.next()){
				int exemplarID = resultSet.getInt(1);

				//Löschung der Exemplare
				String sqlDelete = "DELETE FROM Exemplar WHERE Exemplar_id = ?;";

				PreparedStatement preparedStatement1 = connection.prepareStatement(sqlDelete);
				preparedStatement1.setObject(1, exemplarID);
				preparedStatement1.executeUpdate();
			}

			//Löschung des Artikels
			String sqlDelete2 = "DELETE FROM Artikel WHERE Artikel.ROWID = ?;";

			PreparedStatement preparedStatement2 = connection.prepareStatement(sqlDelete2);
			preparedStatement2.setObject(1, artikelid);
			preparedStatement2.executeUpdate();

		} catch (SQLiteException e) {
			connection.close();

			return Response.status(404).entity("Der übergebene Parameter ist fehlerhaft").build();
		}

		connection.close();

		return Response.status(Response.Status.NO_CONTENT).build();
	}


	@Path("autoren/{autorid}")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@DELETE
	public Response deleteAutor(@PathParam("autorid") Integer autorid) throws SQLException {
		Connection connection = dataSource.getConnection();

		String checkID = "SELECT Autor_id FROM Autor WHERE Autor_id = ?";
		PreparedStatement preparedStatementCheck = connection.prepareStatement(checkID);
		preparedStatementCheck.closeOnCompletion();
		preparedStatementCheck.setObject(1, autorid);
		ResultSet resultSetCheck = preparedStatementCheck.executeQuery();
		if(!resultSetCheck.next()) {
			resultSetCheck.close();
			connection.close();
			return Response.status(404).entity("Die ID existiert nicht").build();
			//return Response.status(200)
		}
		resultSetCheck.close();


		String sql = "SELECT A.ROWID FROM Autor JOIN verfasst v on Autor.Autor_id = v.Autor_id "
				+ "JOIN Artikel A on v.ISBN = A.ISBN WHERE Autor.Autor_id = ?;";

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, autorid);

		try {

			ResultSet artikelIDs = preparedStatement.executeQuery();

			while (artikelIDs.next()) {
				Integer aID = artikelIDs.getInt(1);
				deleteArtikel(aID);
			}

			String sqlDelete = "DELETE FROM Autor WHERE Autor_id = ?;";
			PreparedStatement preparedStatement2 = connection.prepareStatement(sqlDelete);
			preparedStatement2.closeOnCompletion();
			preparedStatement2.setObject(1, autorid);
			preparedStatement2.executeUpdate();

		} catch (SQLiteException e) {
			connection.close();

			return Response.status(404).entity("Der übergebene Parameter ist fehlerhaft").build();
		}

		connection.close();

		return Response.status(Response.Status.NO_CONTENT).build();
	}


	@Path("ausleihen/{ausleiheid}")
	@RolesAllowed({"BIBLIOTHEKAR"})
	@PATCH
	public Response patchAusleihe(@PathParam("ausleiheid") Integer ausleiheid,
	                              @QueryParam("zurueckgegeben") Boolean zurueckgegeben,
	                              @QueryParam("beginn") String beginn,
	                              @QueryParam("ende") String ende) throws SQLException {

		if (zurueckgegeben==null && beginn==null && ende==null) {
			return Response.status(400).entity("Min. ein Parameter uebergeben ausser der ID").build();
		}

		Connection connection = dataSource.getConnection();

		String checkID = "SELECT ausleihe_id FROM Ausleihe WHERE ausleihe_id = ?;";
		PreparedStatement preparedStatementCheck = connection.prepareStatement(checkID);
		preparedStatementCheck.closeOnCompletion();
		preparedStatementCheck.setObject(1, ausleiheid);
		ResultSet resultSet = preparedStatementCheck.executeQuery();
		if(!resultSet.next()) {
			resultSet.close();
			connection.close();
			System.out.println("test");
			return Response.status(400).entity("Die ID existiert nicht").build();
			//return Response.status(200)
		}
		resultSet.close();


		if (zurueckgegeben != null) {
			patchAusleiheHelper("Zurueckgegeben", ausleiheid, zurueckgegeben);
		}

		if (beginn != null) {
			patchAusleiheHelper("Ausleihbeginn", ausleiheid, (beginn +" 00:00:00"));
		}

		if (ende != null) {
			patchAusleiheHelper("Ausleihende", ausleiheid, (ende +" 00:00:00"));
		}

		connection.close();

		return Response.status(Response.Status.NO_CONTENT).build();
	}


	private void patchAusleiheHelper(String string, Integer id, Object param) throws SQLException {
		Connection connection = dataSource.getConnection();

		String sql = "UPDATE Ausleihe SET ";

		sql += string + " = ?";
		sql += " WHERE Ausleihe_id = ?;";

		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.closeOnCompletion();
		preparedStatement.setObject(1, param);
		preparedStatement.setObject(2, id);

		preparedStatement.executeUpdate();

		connection.close();

	}

}
