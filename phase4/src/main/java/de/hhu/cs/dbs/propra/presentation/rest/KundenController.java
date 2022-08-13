package de.hhu.cs.dbs.propra.presentation.rest;


import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@Path("/")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public class KundenController {

    @Inject
    private DataSource dataSource;

    @Context
    private SecurityContext securityContext;

    @Context
    private UriInfo uriInfo;




    @Path("ausleihen")
    @RolesAllowed({"KUNDE"})
    @POST
    public Response postAusleihe(@FormDataParam("exemplarid") Integer exemplarid,
                                 @FormDataParam("zurueckgegeben") Boolean zurueckgegeben,
                                 @FormDataParam("beginn") String beginn,
                                 @FormDataParam("ende") String ende) throws SQLException {
        if (exemplarid == null) return Response.status(400).entity("ExemplarID darf nicht NULL sein").build();
        if (zurueckgegeben == null) return Response.status(400).entity("Zurueckgegeben darf nicht NULL sein").build();
        if (beginn == null) return Response.status(400).entity("Beginn darf nicht NULL sein").build();
        if (ende == null) return Response.status(400).entity("Ende darf nicht NULL sein").build();

        Connection connection = dataSource.getConnection();

        //Checkt ob es die ID überhaupt gibt
        String checkID = "SELECT Exemplar_id FROM Exemplar WHERE Exemplar_id = ?";
        PreparedStatement preparedStatementCheck = connection.prepareStatement(checkID);
        preparedStatementCheck.closeOnCompletion();
        preparedStatementCheck.setObject(1, exemplarid);
        ResultSet resultSet = preparedStatementCheck.executeQuery();
        if(!resultSet.next()) {
            resultSet.close();
            connection.close();
            return Response.status(400).entity("Die ExemplarID existiert nicht").build();
        }
        resultSet.close();


        //Sucht die höhste Id und erhöht diese um eins, um mit keiner anderen Id zu kolladieren
        String getAusleiheId = "SELECT Ausleihe_id FROM Ausleihe ORDER BY Ausleihe_id DESC LIMIT 1";
        PreparedStatement preparedStatement = connection.prepareStatement(getAusleiheId);
        preparedStatement.closeOnCompletion();
        ResultSet resultSet2=  preparedStatement.executeQuery();

        Integer ausleiheId = 1;//falls keine ID existiert

        if (resultSet2.next()) {
            ausleiheId = resultSet2.getInt("Ausleihe_id") + 1;//erhöht die ID um eins
        }

        String sql = "INSERT INTO Ausleihe (Ausleihe_id, Ausleihbeginn, Ausleihende, Zurueckgegeben, Email, Exemplar_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement preparedStatement2 = connection.prepareStatement(sql);
        preparedStatement2.closeOnCompletion();
        preparedStatement2.setObject(1, ausleiheId);
        preparedStatement2.setObject(2, (beginn + " 00:00:00"));
        preparedStatement2.setObject(3, (ende + " 00:00:00"));
        preparedStatement2.setObject(4, zurueckgegeben);
        preparedStatement2.setObject(5, securityContext.getUserPrincipal().getName());
        preparedStatement2.setObject(6, exemplarid);
        preparedStatement2.executeUpdate();

        connection.close();

        return Response.created(uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(ausleiheId))
                .build())
                .build();

    }

    @Path("ausleihen")
    @RolesAllowed({"KUNDE"})
    @GET
    public List<Map<String, Object>> getAusleihe(@QueryParam("zurueckgegeben") Boolean zurueckgegeben,
                                @QueryParam("beginn") String beginn) throws SQLException {
        Connection connection = dataSource.getConnection();

        String sql = "SELECT * FROM Ausleihe"
                + " WHERE (Zurueckgegeben = ? AND Ausleihbeginn <= ? AND Email = ?)"
                + " OR (? IS NULL AND Ausleihbeginn <= ? AND Email = ?)"
                + " OR (Zurueckgegeben = ? AND ? IS NULL AND Email = ?)"
                + " OR (? IS NULL AND ? IS NULL AND Email = ?);";


        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.closeOnCompletion();
        preparedStatement.setObject(1, zurueckgegeben);
        preparedStatement.setObject(2, beginn + " 00:00:00");
        preparedStatement.setObject(3, securityContext.getUserPrincipal().getName());
        preparedStatement.setObject(4, zurueckgegeben);
        preparedStatement.setObject(5, beginn + " 00:00:00");
        preparedStatement.setObject(6, securityContext.getUserPrincipal().getName());
        preparedStatement.setObject(7, zurueckgegeben);
        preparedStatement.setObject(8, beginn + " 00:00:00");
        preparedStatement.setObject(9, securityContext.getUserPrincipal().getName());
        preparedStatement.setObject(10, zurueckgegeben);
        preparedStatement.setObject(11, beginn + " 00:00:00");
        preparedStatement.setObject(12, securityContext.getUserPrincipal().getName());


        ResultSet resultSet = preparedStatement.executeQuery();

        List<Map<String, Object>> entities = new ArrayList<>();
        Map<String, Object> entity;

        while (resultSet.next()) {
            entity = new LinkedHashMap<>();
            entity.put("ausleiheid", resultSet.getObject(1));
            entity.put("exemplarid", resultSet.getObject(6));
            entity.put("zurueckgegeben", resultSet.getBoolean(4));
            entity.put("beginn", (resultSet.getString(2)
                    .substring(0, resultSet.getString(2).length() - 9)));
            entity.put("ende", (resultSet.getString(3)
                    .substring(0, resultSet.getString(3).length() - 9)));
            entities.add(entity);
        }

        resultSet.close();
        connection.close();

        return entities;
    }


    @Path("adressen")
    @RolesAllowed({"KUNDE"})
    @PATCH
    public Response patchAdresse(@QueryParam("hausnummer") String hausnummer,
                                 @QueryParam("strasse") String strasse,
                                 @QueryParam("stadt") String stadt,
                                 @QueryParam("plz") String plz)throws SQLException {
        Connection connection = dataSource.getConnection();

        String getAddressID = "SELECT Adresse_id FROM Kunde WHERE Email = ?;";
        PreparedStatement preparedStatementCheck = connection.prepareStatement(getAddressID);
        preparedStatementCheck.closeOnCompletion();
        preparedStatementCheck.setObject(1,  securityContext.getUserPrincipal().getName());
        ResultSet resultSet = preparedStatementCheck.executeQuery();

        Integer adressID= resultSet.getInt(1);

        resultSet.close();

        if(hausnummer==null && strasse==null && stadt==null && plz==null) {
            return Response.status(400).entity("Min. ein Parameter uebergeben, ausser der ID").build();
        }


        if (hausnummer != null) {
            patchAdresseHelper("Hausnummer", adressID, hausnummer);
        }

        if(strasse != null) {
            patchAdresseHelper("Strasse", adressID, strasse);
        }

        if (stadt != null) {
            patchAdresseHelper("Stadt", adressID, stadt);
        }

        if (plz != null) {
            patchAdresseHelper("PLZ", adressID, plz);
        }


        return Response.status(Response.Status.NO_CONTENT).build();
    }



    private void patchAdresseHelper(String string, Integer id, Object param) throws SQLException {
        Connection connection = dataSource.getConnection();

        String sql = "UPDATE Adresse SET ";

        sql += string + " = ?";
        sql += " WHERE Adresse_id = ?;";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.closeOnCompletion();
        preparedStatement.setObject(1, param);
        preparedStatement.setObject(2, id);

        preparedStatement.executeUpdate();

        connection.close();

    }


}
