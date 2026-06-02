package security.permissions;

import security.auth.InMemoryAuthProvider;
import security.model.AccessScope;
import security.model.AuthenticatedUser;
import security.repository.ControllerAssignmentRepository;
import shared.exceptions.RepositoryException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ControllerAssignmentService {
    private final ControllerAssignmentRepository assignmentRepository;
    private final InMemoryAuthProvider inMemoryAuthProvider;

    public ControllerAssignmentService(ControllerAssignmentRepository assignmentRepository,
                                       InMemoryAuthProvider inMemoryAuthProvider) {
        this.assignmentRepository = assignmentRepository;
        this.inMemoryAuthProvider = inMemoryAuthProvider;
    }

    public AccessScope buildAccessScope(AuthenticatedUser user) {
        if (user == null) {
            return AccessScope.noMonitoringAccess();
        }
        if (user.isAdmin()) {
            return AccessScope.unrestricted();
        }
        if (user.isDriver()) {
            return AccessScope.noMonitoringAccess();
        }
        if (user.isController()) {
            return AccessScope.restrictedToRoutes(getAssignedRouteIds(user));
        }
        return AccessScope.noMonitoringAccess();
    }

    public Set<Integer> getAssignedRouteIds(AuthenticatedUser user) {
        if (user == null || !user.isController()) {
            return Collections.emptySet();
        }
        Set<Integer> routeIds = loadAssignedRoutesFromRepository(user);
        if (!routeIds.isEmpty()) {
            return routeIds;
        }
        return inMemoryAuthProvider.findAssignedRouteIds(user.getNationalId());
    }

    private Set<Integer> loadAssignedRoutesFromRepository(AuthenticatedUser user) {
        if (assignmentRepository == null || user.getId() == null) {
            return Collections.emptySet();
        }
        try {
            return new LinkedHashSet<Integer>(assignmentRepository.findAssignedRouteIdsForController(user.getId()));
        } catch (RepositoryException exception) {
            return Collections.emptySet();
        }
    }
}
