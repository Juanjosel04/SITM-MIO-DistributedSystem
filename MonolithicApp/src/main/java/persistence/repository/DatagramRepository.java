package persistence.repository;

import core.model.Bus;
import core.model.Datagram;
import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DatagramRepository {
    private final DatabaseConnectionManager connectionManager;

    public DatagramRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void save(Datagram datagram, Bus bus) throws RepositoryException {
        String sql = "INSERT INTO datagrams(source_datagram_id, bus_id, bus_code, route_id, latitude, longitude, " +
                "speed, event_code, timestamp, raw_payload) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, datagram.getId());
            statement.setLong(2, bus.getId());
            statement.setString(3, datagram.getBusCode());
            statement.setInt(4, datagram.getRouteId());
            statement.setDouble(5, datagram.getLatitude());
            statement.setDouble(6, datagram.getLongitude());
            if (datagram.getSpeed() == null) {
                statement.setNull(7, java.sql.Types.DOUBLE);
            } else {
                statement.setDouble(7, datagram.getSpeed().doubleValue());
            }
            statement.setString(8, datagram.getEventCode());
            statement.setTimestamp(9, Timestamp.valueOf(datagram.getTimestamp()));
            statement.setString(10, datagram.getRawPayload());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save datagram", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to save datagram", exception);
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
