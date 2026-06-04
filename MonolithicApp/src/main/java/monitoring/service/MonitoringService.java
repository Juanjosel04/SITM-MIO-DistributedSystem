package monitoring.service;

import core.model.BusPosition;
import core.model.PipelineSummary;
import core.model.Route;
import core.observer.events.SystemEvent;
import core.observer.events.SystemEventType;
import core.observer.notifications.NotificationLevel;
import core.observer.observers.SystemEventListener;
import events.model.Alert;
import events.model.OperationalEvent;
import monitoring.model.AlertPanelModel;
import monitoring.model.MonitoringMetric;
import monitoring.model.RealtimeMapState;
import shared.utils.RouteDisplayService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MonitoringService implements SystemEventListener {
    private final RealtimeMapState state = new RealtimeMapState();
    private final RouteDisplayService routeDisplayService = new RouteDisplayService();
    private final List<MonitoringStateListener> stateListeners = new ArrayList<MonitoringStateListener>();

    public RealtimeMapState getState() {
        return state;
    }

    public String getRouteDisplayName(int routeId) {
        return routeDisplayService.getDisplayName(routeId);
    }

    public String getRouteFilterLabel(int routeId) {
        return routeDisplayService.getFilterLabel(routeId);
    }

    public String getRouteFullDisplayName(int routeId) {
        return routeDisplayService.getFullDisplayName(routeId);
    }

    public List<Integer> getCatalogRouteIds() {
        return routeDisplayService.getRouteIds();
    }

    public synchronized void addStateListener(MonitoringStateListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    public List<MonitoringMetric> getMetrics() {
        List<MonitoringMetric> metrics = new ArrayList<MonitoringMetric>();
        metrics.add(new MonitoringMetric("Routes", String.valueOf(state.getRoutesLoaded()), "Loaded routes"));
        metrics.add(new MonitoringMetric("Processed", String.valueOf(state.getDatagramsProcessed()), "Datagrams"));
        metrics.add(new MonitoringMetric("Invalid", String.valueOf(state.getDatagramsInvalid()), "Rejected"));
        metrics.add(new MonitoringMetric("Active Buses", String.valueOf(state.getActiveBusCount()), "Live units"));
        metrics.add(new MonitoringMetric("Positions", String.valueOf(state.getPositionsUpdated()), "Updates"));
        metrics.add(new MonitoringMetric("Alerts", String.valueOf(state.getBasicAlertCount()), "Basic alerts"));
        metrics.add(new MonitoringMetric("Events", String.valueOf(state.getOperationalEventCount()), "Recent"));
        return metrics;
    }

    @Override
    public void onSystemEvent(SystemEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }

        if (SystemEventType.STREAM_STARTED == event.getEventType()) {
            state.setStreamStatus("Streaming");
        } else if (SystemEventType.ROUTE_CATALOG_LOADED == event.getEventType()) {
            updateRouteCatalog(event.getPayload());
        } else if (SystemEventType.ROUTES_LOADED == event.getEventType()) {
            updateRoutesLoaded(event.getPayload());
        } else if (SystemEventType.BUS_POSITION_UPDATED == event.getEventType()) {
            updateBusPosition(event.getPayload());
        } else if (SystemEventType.DATAGRAM_PROCESSED == event.getEventType()) {
            state.incrementDatagramsProcessed();
        } else if (SystemEventType.DATAGRAM_INVALID == event.getEventType()) {
            state.incrementDatagramsInvalid();
            addAlert("Invalid datagram", event.getMessage(), NotificationLevel.WARNING);
        } else if (SystemEventType.PROCESSING_ERROR == event.getEventType()) {
            addAlert("Processing error", event.getMessage(), NotificationLevel.ERROR);
        } else if (SystemEventType.STREAM_FINISHED == event.getEventType()) {
            state.setStreamStatus("Finished");
            addFinishedAlert(event.getPayload());
        } else if (SystemEventType.BASIC_ALERT_CREATED == event.getEventType()) {
            addAlert("Alert", event.getMessage(), NotificationLevel.INFO);
        } else if (SystemEventType.OPERATIONAL_EVENT_CREATED == event.getEventType()) {
            addOperationalEvent(event.getPayload());
        } else if (SystemEventType.ALERT_CREATED == event.getEventType()) {
            addFormalAlert(event.getPayload());
        } else if (SystemEventType.DRIVER_EVENT_SENT == event.getEventType()) {
            state.setStreamStatus("Driver event received");
        } else if (SystemEventType.EVENT_PROCESSING_ERROR == event.getEventType()) {
            addAlert("Event processing error", event.getMessage(), NotificationLevel.ERROR);
        }

        notifyStateListeners();
    }

    private void updateRoutesLoaded(Object payload) {
        if (payload instanceof Integer) {
            state.setRoutesLoaded(((Integer) payload).intValue());
        }
    }

    private void updateRouteCatalog(Object payload) {
        if (!(payload instanceof List)) {
            return;
        }
        List<Route> routes = new ArrayList<Route>();
        for (Object value : (List<?>) payload) {
            if (value instanceof Route) {
                routes.add((Route) value);
            }
        }
        routeDisplayService.replaceRoutes(routes);
    }

    private void updateBusPosition(Object payload) {
        if (payload instanceof BusPosition) {
            state.updateBusPosition((BusPosition) payload);
        }
    }

    private void addFinishedAlert(Object payload) {
        if (payload instanceof PipelineSummary) {
            state.setLastPipelineSummary((PipelineSummary) payload);
        }
        if (payload instanceof PipelineSummary && ((PipelineSummary) payload).getErrors() > 0) {
            addAlert("Stream finished", "Finished with processing errors.", NotificationLevel.WARNING);
        } else {
            addAlert("Stream finished", "Processing completed.", NotificationLevel.SUCCESS);
        }
    }

    private void addOperationalEvent(Object payload) {
        if (payload instanceof OperationalEvent) {
            state.addOperationalEvent((OperationalEvent) payload);
        }
    }

    private void addFormalAlert(Object payload) {
        if (payload instanceof Alert) {
            Alert alert = (Alert) payload;
            state.addAlert(new AlertPanelModel(alert.getTitle(), alert.getMessage(),
                    mapPriority(alert), alert.getCreatedAt()));
        }
    }

    private void addAlert(String title, String message, NotificationLevel level) {
        state.addAlert(new AlertPanelModel(title, message == null ? "" : message, level, LocalDateTime.now()));
    }

    private NotificationLevel mapPriority(Alert alert) {
        if (alert.getPriority() == null) {
            return NotificationLevel.INFO;
        }
        String priority = alert.getPriority().name();
        if ("CRITICAL".equals(priority) || "HIGH".equals(priority)) {
            return NotificationLevel.ERROR;
        }
        if ("MEDIUM".equals(priority)) {
            return NotificationLevel.WARNING;
        }
        return NotificationLevel.INFO;
    }

    private void notifyStateListeners() {
        List<MonitoringStateListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<MonitoringStateListener>(stateListeners);
        }
        for (MonitoringStateListener listener : snapshot) {
            listener.onMonitoringStateChanged();
        }
    }
}
