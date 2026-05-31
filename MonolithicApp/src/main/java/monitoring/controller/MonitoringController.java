package monitoring.controller;

import monitoring.model.AlertPanelModel;
import monitoring.model.BusMarker;
import monitoring.model.MonitoringMetric;
import events.model.OperationalEvent;
import monitoring.service.MonitoringService;
import monitoring.service.MonitoringStateListener;

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

    public List<AlertPanelModel> getAlerts() {
        return monitoringService.getState().getAlerts();
    }

    public String getStreamStatus() {
        return monitoringService.getState().getStreamStatus();
    }

    public List<OperationalEvent> getRecentEvents() {
        return monitoringService.getState().getRecentEvents();
    }
}
