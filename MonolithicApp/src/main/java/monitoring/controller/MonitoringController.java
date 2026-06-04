package monitoring.controller;

import monitoring.model.AlertPanelModel;
import monitoring.model.BusMarker;
import monitoring.model.MonitoringMetric;
import core.model.PipelineSummary;
import core.model.BusPosition;
import events.model.OperationalEvent;
import monitoring.service.MonitoringService;
import monitoring.service.MonitoringStateListener;
import security.model.AccessScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonitoringController {
    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    public void addStateListener(MonitoringStateListener listener) {
        monitoringService.addStateListener(listener);
    }

    public List<MonitoringMetric> getMetrics() {
        return monitoringService.getMetrics();
    }

    public List<BusMarker> getCurrentBuses() {
        return monitoringService.getState().getCurrentBuses();
    }

    public List<BusMarker> getCurrentBuses(AccessScope scope) {
        if (scope == null) {
            return Collections.emptyList();
        }
        if (scope.canViewAllRoutes()) {
            return getCurrentBuses();
        }
        List<BusMarker> filtered = new ArrayList<BusMarker>();
        for (BusMarker bus : getCurrentBuses()) {
            if (bus != null && scope.canViewRoute(Integer.valueOf(bus.getRouteId()))) {
                filtered.add(bus);
            }
        }
        return filtered;
    }

    public List<BusPosition> getPositionHistory() {
        return monitoringService.getState().getPositionHistory();
    }

    public List<AlertPanelModel> getAlerts() {
        return monitoringService.getState().getAlerts();
    }

    public List<AlertPanelModel> getAlerts(AccessScope scope) {
        if (scope == null) {
            return Collections.emptyList();
        }
        if (scope.canViewAllRoutes()) {
            return getAlerts();
        }
        return Collections.emptyList();
    }

    public String getStreamStatus() {
        return monitoringService.getState().getStreamStatus();
    }

    public List<OperationalEvent> getRecentEvents() {
        return monitoringService.getState().getRecentEvents();
    }

    public List<OperationalEvent> getRecentEvents(AccessScope scope) {
        if (scope == null) {
            return Collections.emptyList();
        }
        if (scope.canViewAllRoutes()) {
            return getRecentEvents();
        }
        List<OperationalEvent> filtered = new ArrayList<OperationalEvent>();
        for (OperationalEvent event : getRecentEvents()) {
            if (event != null && scope.canViewRoute(Integer.valueOf(event.getRouteId()))) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    public int getRoutesLoaded() {
        return monitoringService.getState().getRoutesLoaded();
    }

    public List<Integer> getCatalogRouteIds() {
        return monitoringService.getCatalogRouteIds();
    }

    public String getRouteDisplayName(int routeId) {
        return monitoringService.getRouteDisplayName(routeId);
    }

    public String getRouteFilterLabel(int routeId) {
        return monitoringService.getRouteFilterLabel(routeId);
    }

    public String getRouteFullDisplayName(int routeId) {
        return monitoringService.getRouteFullDisplayName(routeId);
    }

    public int getPositionsUpdated() {
        return monitoringService.getState().getPositionsUpdated();
    }

    public int getDatagramsProcessed() {
        return monitoringService.getState().getDatagramsProcessed();
    }

    public int getDatagramsInvalid() {
        return monitoringService.getState().getDatagramsInvalid();
    }

    public PipelineSummary getLastPipelineSummary() {
        return monitoringService.getState().getLastPipelineSummary();
    }
}
