package persistence.connection;

import shared.exceptions.DatabaseConnectionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {
    private final DatabaseConfig config;

    public DatabaseConnectionManager(DatabaseConfig config) {
        this.config = config;
    }

    public Connection getConnection() throws DatabaseConnectionException {
        try {
            Class.forName(config.getDriverClassName());
            return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
        } catch (ClassNotFoundException exception) {
            throw new DatabaseConnectionException("PostgreSQL driver was not found", exception);
        } catch (SQLException exception) {
            throw new DatabaseConnectionException("Could not open PostgreSQL connection", exception);
        }
    }

    public boolean canConnect() {
        Connection connection = null;
        try {
            connection = getConnection();
            return true;
        } catch (DatabaseConnectionException exception) {
            return false;
        } finally {
            close(connection);
        }
    }

    public void close(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
