package persistence.repository;

import core.model.Bus;
import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BusRepository {
    private final DatabaseConnectionManager connectionManager;

    public BusRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Bus saveOrUpdate(Bus bus) throws RepositoryException {
        String sql = "INSERT INTO buses(code, plate, status, route_id) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (code) DO UPDATE SET route_id = EXCLUDED.route_id, status = EXCLUDED.status " +
                "RETURNING id";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, bus.getCode());
            statement.setString(2, bus.getPlate());
            statement.setString(3, bus.getStatus());
            statement.setInt(4, bus.getRouteId());
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                bus.setId(resultSet.getLong("id"));
            }
            return bus;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save bus", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to save bus", exception);
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
