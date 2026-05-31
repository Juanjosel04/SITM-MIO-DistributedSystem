package persistence.repository;

import events.model.OperationalEvent;
import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class EventRepository {
    private final DatabaseConnectionManager connectionManager;

    public EventRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public OperationalEvent save(OperationalEvent event) throws RepositoryException {
        String sql = "INSERT INTO operational_events(bus_id, bus_code, route_id, event_code, event_type, priority, " +
                "description, source_type, status, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            if (event.getBusId() <= 0L) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, event.getBusId());
            }
            statement.setString(2, event.getBusCode());
            if (event.getRouteId() <= 0) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, event.getRouteId());
            }
            statement.setString(4, event.getEventCode());
            statement.setString(5, event.getEventType().name());
            statement.setString(6, event.getPriority().name());
            statement.setString(7, event.getDescription());
            statement.setString(8, event.getSourceType().name());
            statement.setString(9, event.getStatus().name());
            statement.setTimestamp(10, Timestamp.valueOf(event.getTimestamp()));
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                event.setId(resultSet.getLong("id"));
            }
            return event;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save operational event", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to save operational event", exception);
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
