package persistence.repository;

import core.model.Route;
import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RouteRepository {
    private final DatabaseConnectionManager connectionManager;

    public RouteRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void save(Route route) throws RepositoryException {
        String sql = "INSERT INTO routes(id, short_name, description, status) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET short_name = EXCLUDED.short_name, " +
                "description = EXCLUDED.description, status = EXCLUDED.status";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setInt(1, route.getId());
            statement.setString(2, route.getShortName());
            statement.setString(3, route.getDescription());
            statement.setString(4, route.getStatus());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save route", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to save route", exception);
        } finally {
            close(statement);
            connectionManager.close(connection);
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
