package security.repository;

import persistence.connection.DatabaseConnectionManager;
import security.model.UserCredentials;
import security.roles.UserRole;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepository {
    private final DatabaseConnectionManager connectionManager;

    public UserRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Optional<UserCredentials> findByNationalId(String nationalId) throws RepositoryException {
        String sql = "SELECT id, national_id, full_name, password_hash, role, status " +
                "FROM system_users WHERE national_id = ? LIMIT 1";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, nationalId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(new UserCredentials(
                    Long.valueOf(resultSet.getLong("id")),
                    resultSet.getString("national_id"),
                    resultSet.getString("full_name"),
                    resultSet.getString("password_hash"),
                    UserRole.fromDatabaseValue(resultSet.getString("role")),
                    resultSet.getString("status")));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load user credentials", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to load user credentials", exception);
        } catch (IllegalArgumentException exception) {
            throw new RepositoryException("User role stored in database is invalid", exception);
        } finally {
            close(resultSet);
            close(statement);
            connectionManager.close(connection);
        }
    }

    private void close(ResultSet resultSet) {
        if (resultSet == null) {
            return;
        }
        try {
            resultSet.close();
        } catch (SQLException ignored) {
        }
    }

    private void close(PreparedStatement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (SQLException ignored) {
        }
    }
}
