package security.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class AccessScope {
    private final boolean unrestricted;
    private final Set<Integer> allowedRouteIds;

    private AccessScope(boolean unrestricted, Set<Integer> allowedRouteIds) {
        this.unrestricted = unrestricted;
        Set<Integer> copy = new LinkedHashSet<Integer>();
        if (allowedRouteIds != null) {
            for (Integer routeId : allowedRouteIds) {
                if (routeId != null) {
                    copy.add(routeId);
                }
            }
        }
        this.allowedRouteIds = Collections.unmodifiableSet(copy);
    }

    public static AccessScope unrestricted() {
        return new AccessScope(true, Collections.<Integer>emptySet());
    }

    public static AccessScope restrictedToRoutes(Set<Integer> routeIds) {
        return new AccessScope(false, routeIds);
    }

    public static AccessScope noMonitoringAccess() {
        return new AccessScope(false, Collections.<Integer>emptySet());
    }

    public boolean canViewAllRoutes() {
        return unrestricted;
    }

    public boolean canViewRoute(Integer routeId) {
        return unrestricted || (routeId != null && allowedRouteIds.contains(routeId));
    }

    public Set<Integer> getAllowedRouteIds() {
        return allowedRouteIds;
    }

    public boolean isEmpty() {
        return !unrestricted && allowedRouteIds.isEmpty();
    }
}
