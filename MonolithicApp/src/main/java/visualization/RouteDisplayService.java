package visualization;

import domain.Route;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RouteDisplayService {
    private final Map<Integer, Route> routesById;

    public RouteDisplayService(List<Route> routes) {
        Map<Integer, Route> routeMap = new HashMap<Integer, Route>();
        if (routes != null) {
            for (Route route : routes) {
                routeMap.put(Integer.valueOf(route.getId()), route);
            }
        }
        this.routesById = Collections.unmodifiableMap(routeMap);
    }

    public String labelFor(int routeId) {
        Route route = routesById.get(Integer.valueOf(routeId));
        if (route == null) {
            return "Route " + routeId;
        }
        String shortName = route.getShortName();
        if (shortName != null && !shortName.trim().isEmpty()) {
            return shortName.trim();
        }
        return "Route " + routeId;
    }

    public String descriptionFor(int routeId) {
        Route route = routesById.get(Integer.valueOf(routeId));
        if (route == null || route.getDescription().isEmpty()) {
            return labelFor(routeId);
        }
        return route.getDescription();
    }
}
