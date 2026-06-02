package security.repository;

import persistence.connection.DatabaseConnectionManager;
import shared.exceptions.DatabaseConnectionException;
import shared.exceptions.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public class ControllerAssignmentRepository {
    private final DatabaseConnectionManager connectionManager;

    public ControllerAssignmentRepository(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Set<Integer> findAssignedRouteIdsForController(Long controllerUserId) throws RepositoryException {
        String sql = "SELECT DISTINCT zra.route_id " +
                "FROM controller_zone_assignments cza " +
                "JOIN zone_route_assignments zra ON zra.zone_id = cza.zone_id " +
                "WHERE cza.controller_user_id = ? " +
                "ORDER BY zra.route_id";
        Set<Integer> routeIds = new LinkedHashSet<Integer>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionManager.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setLong(1, controllerUserId.longValue());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                routeIds.add(Integer.valueOf(resultSet.getInt("route_id")));
            }
            return routeIds;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load controller route assignments", exception);
        } catch (DatabaseConnectionException exception) {
            throw new RepositoryException("Could not connect to load controller route assignments", exception);
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
