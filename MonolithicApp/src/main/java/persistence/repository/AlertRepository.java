package persistence.repository;

import events.model.Alert;
import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AlertRepository {
    private final DatabaseConnectionManager connectionManager;

    public AlertRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Alert save(Alert alert) throws RepositoryException {
        String sql = "INSERT INTO alerts(event_id, priority, title, message, acknowledged) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            if (alert.getEventId() <= 0L) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, alert.getEventId());
            }
            statement.setString(2, alert.getPriority().name());
            statement.setString(3, alert.getTitle());
            statement.setString(4, alert.getMessage());
            statement.setBoolean(5, alert.isAcknowledged());
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                alert.setId(resultSet.getLong("id"));
            }
            return alert;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save alert", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to save alert", exception);
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
