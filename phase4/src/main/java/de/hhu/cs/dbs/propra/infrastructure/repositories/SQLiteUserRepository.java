package de.hhu.cs.dbs.propra.infrastructure.repositories;

import de.hhu.cs.dbs.propra.domain.model.Role;
import de.hhu.cs.dbs.propra.domain.model.User;
import de.hhu.cs.dbs.propra.domain.model.UserRepository;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SQLiteUserRepository implements UserRepository {
    @Inject
    private DataSource dataSource;

    @Override
    public Optional<User> findByName(String name) { //Email nicht Name
        Optional<User> user = Optional.empty();
        try (Connection connection = dataSource.getConnection()) {
            //String sql = "SELECT ?, 'USER';"; // TODO: Dem Benutzer 'name' die entsprechenden Berechtigungen geben (siehe enum Role),
                                            // um diese per RolesAllowed-Annotation zu nutzen.
                                             // Ein Ergebnistupel besteht aus E-Mail-Adresse bzw. Benutzernamen und Berechtigung.

            String sql ="" +
                        "SELECT Email, 'BIBLIOTHEKAR' FROM Bibliothekar WHERE Email = ? UNION " +
                        "SELECT Email, 'KUNDE' FROM Kunde WHERE Email = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.closeOnCompletion();
            preparedStatement.setObject(1, name);
            preparedStatement.setObject(2, name);
            ResultSet resultSet = preparedStatement.executeQuery();
            user = Optional.ofNullable(extractUser(resultSet));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return user;
    }

    public long countByNameAndPassword(String name, String password) {
        try (Connection connection = dataSource.getConnection()) {
          //  String sql = "SELECT count(*) FROM (SELECT ?, ?);"; // TODO: Die Anzahl der Benutzer mit 'name' als E-Mail-Adresse bzw. Benutzernamen und
                                                                // 'password' als Passwort zurueckgeben.

            String sql = "SELECT count (*) FROM Nutzer WHERE Email = ? AND Passwort = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.closeOnCompletion();
            preparedStatement.setObject(1, name);
            preparedStatement.setObject(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return 0;
    }

    private User extractUser(ResultSet resultSet) {
        try {
            if (!resultSet.next()) return null;
            String name = resultSet.getString(1);
            Set<Role> roles = new HashSet<>();
            do {
                roles.add(Role.valueOf(resultSet.getString(2)));
            } while (resultSet.next());
            return new User(name, roles);
        } catch (SQLException exception) {
            return null;
        }
    }
}
