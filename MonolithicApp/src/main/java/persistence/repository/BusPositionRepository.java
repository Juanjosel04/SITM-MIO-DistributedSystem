package persistence.repository;

import core.model.BusPosition;
import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BusPositionRepository {
    private final DatabaseConnectionManager connectionManager;

    public BusPositionRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void save(BusPosition position) throws RepositoryException {
        String sql = "INSERT INTO bus_positions(bus_id, bus_code, route_id, latitude, longitude, speed, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setLong(1, position.getBusId());
            statement.setString(2, position.getBusCode());
            statement.setInt(3, position.getRouteId());
            statement.setDouble(4, position.getLatitude());
            statement.setDouble(5, position.getLongitude());
            if (position.getSpeed() == null) {
                statement.setNull(6, java.sql.Types.DOUBLE);
            } else {
                statement.setDouble(6, position.getSpeed().doubleValue());
            }
            statement.setTimestamp(7, Timestamp.valueOf(position.getTimestamp()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save bus position", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to save bus position", exception);
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
