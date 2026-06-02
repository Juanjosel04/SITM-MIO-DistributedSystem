package shared.utils;

import core.model.Route;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RouteDisplayService {
    private final Map<Integer, Route> routesById = new LinkedHashMap<Integer, Route>();

    public synchronized void replaceRoutes(List<Route> routes) {
        routesById.clear();
        if (routes == null) {
            return;
        }
        for (Route route : routes) {
            if (route != null) {
                routesById.put(Integer.valueOf(route.getId()), route);
            }
        }
    }

    public synchronized String getShortName(int routeId) {
        Route route = routesById.get(Integer.valueOf(routeId));
        if (route == null || isBlank(route.getShortName())) {
            return null;
        }
        return route.getShortName().trim();
    }

    public synchronized String getDescription(int routeId) {
        Route route = routesById.get(Integer.valueOf(routeId));
        if (route == null || isBlank(route.getDescription()) || "NA".equalsIgnoreCase(route.getDescription().trim())) {
            return null;
        }
        return route.getDescription().trim();
    }

    public synchronized String getDisplayName(int routeId) {
        String shortName = getShortName(routeId);
        return shortName == null ? fallback(routeId) : shortName;
    }

    public synchronized String getFilterLabel(int routeId) {
        String shortName = getShortName(routeId);
        return shortName == null ? fallback(routeId) : shortName + " (" + routeId + ")";
    }

    public synchronized String getFullDisplayName(int routeId) {
        String shortName = getShortName(routeId);
        if (shortName == null) {
            return fallback(routeId);
        }
        String description = getDescription(routeId);
        return description == null ? shortName : shortName + " - " + description;
    }

    private String fallback(int routeId) {
        return "Route " + routeId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
