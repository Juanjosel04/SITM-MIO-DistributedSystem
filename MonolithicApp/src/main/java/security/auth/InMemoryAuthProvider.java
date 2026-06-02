package security.auth;

import security.model.UserCredentials;
import security.roles.UserRole;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryAuthProvider {
    private final Map<String, UserCredentials> usersByNationalId = new LinkedHashMap<String, UserCredentials>();
    private final Map<String, Set<Integer>> controllerRoutesByNationalId = new LinkedHashMap<String, Set<Integer>>();

    public InMemoryAuthProvider() {
        addUser(1L, "1001234567", "Administrador General", "DEMO:admin123", UserRole.ADMIN);
        addUser(2L, "2001234501", "Controlador Operacional 01", "DEMO:controller123", UserRole.CONTROLLER);
        addUser(3L, "3001234567", "Conductor Demo 01", "DEMO:driver123", UserRole.DRIVER);
        controllerRoutesByNationalId.put("2001234501", routeSet(304, 1472, 2241, 2274));
    }

    public Optional<UserCredentials> findByNationalId(String nationalId) {
        if (nationalId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByNationalId.get(nationalId.trim()));
    }

    public Set<Integer> findAssignedRouteIds(String nationalId) {
        if (nationalId == null) {
            return Collections.emptySet();
        }
        Set<Integer> routeIds = controllerRoutesByNationalId.get(nationalId.trim());
        if (routeIds == null) {
            return Collections.emptySet();
        }
        return routeIds;
    }

    private void addUser(Long id, String nationalId, String fullName, String passwordHash, UserRole role) {
        usersByNationalId.put(nationalId, new UserCredentials(id, nationalId, fullName, passwordHash, role, "ACTIVE"));
    }

    private Set<Integer> routeSet(Integer... routeIds) {
        return Collections.unmodifiableSet(new LinkedHashSet<Integer>(Arrays.asList(routeIds)));
    }
}
