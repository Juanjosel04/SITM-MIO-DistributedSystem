package monitoring.model;

import core.model.BusPosition;
import core.model.PipelineSummary;
import events.model.OperationalEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RealtimeMapState {
    private final Map<String, BusMarker> currentBusPositions = new LinkedHashMap<String, BusMarker>();
    private final List<BusPosition> positionHistory = new ArrayList<BusPosition>();
    private final List<AlertPanelModel> alerts = new ArrayList<AlertPanelModel>();
    private final List<OperationalEvent> recentEvents = new ArrayList<OperationalEvent>();
    private int routesLoaded;
    private int datagramsProcessed;
    private int datagramsInvalid;
    private int positionsUpdated;
    private int alertsCreated;
    private String streamStatus = "Idle";
    private LocalDateTime lastUpdate;
    private PipelineSummary lastPipelineSummary;

    public synchronized void updateBusPosition(BusPosition position) {
        BusMarker marker = new BusMarker(position.getBusCode(), position.getRouteId(), position.getLatitude(),
                position.getLongitude(), position.getSpeed(), position.getTimestamp(), "ACTIVE");
        currentBusPositions.put(position.getBusCode(), marker);
        positionHistory.add(copyPosition(position));
        positionsUpdated++;
        lastUpdate = LocalDateTime.now();
    }

    public synchronized List<BusMarker> getCurrentBuses() {
        return new ArrayList<BusMarker>(currentBusPositions.values());
    }

    public synchronized List<BusPosition> getPositionHistory() {
        List<BusPosition> copy = new ArrayList<BusPosition>();
        for (BusPosition position : positionHistory) {
            copy.add(copyPosition(position));
        }
        return copy;
    }

    public synchronized void addAlert(AlertPanelModel alert) {
        alerts.add(0, alert);
        alertsCreated++;
        if (alerts.size() > 8) {
            alerts.remove(alerts.size() - 1);
        }
        lastUpdate = LocalDateTime.now();
    }

    public synchronized void addOperationalEvent(OperationalEvent event) {
        recentEvents.add(0, event);
        if (recentEvents.size() > 10) {
            recentEvents.remove(recentEvents.size() - 1);
        }
        lastUpdate = LocalDateTime.now();
    }

    public synchronized List<OperationalEvent> getRecentEvents() {
        return new ArrayList<OperationalEvent>(recentEvents);
    }

    public synchronized List<AlertPanelModel> getAlerts() {
        return new ArrayList<AlertPanelModel>(alerts);
    }

    public synchronized int getActiveBusCount() {
        return currentBusPositions.size();
    }

    public synchronized int getRoutesLoaded() {
        return routesLoaded;
    }

    public synchronized void setRoutesLoaded(int routesLoaded) {
        this.routesLoaded = routesLoaded;
        lastUpdate = LocalDateTime.now();
    }

    public synchronized int getDatagramsProcessed() {
        return datagramsProcessed;
    }

    public synchronized void incrementDatagramsProcessed() {
        datagramsProcessed++;
        lastUpdate = LocalDateTime.now();
    }

    public synchronized int getDatagramsInvalid() {
        return datagramsInvalid;
    }

    public synchronized void incrementDatagramsInvalid() {
        datagramsInvalid++;
        lastUpdate = LocalDateTime.now();
    }

    public synchronized int getPositionsUpdated() {
        return positionsUpdated;
    }

    public synchronized int getBasicAlertCount() {
        return alertsCreated;
    }

    public synchronized int getOperationalEventCount() {
        return recentEvents.size();
    }

    public synchronized PipelineSummary getLastPipelineSummary() {
        return lastPipelineSummary;
    }

    public synchronized void setLastPipelineSummary(PipelineSummary lastPipelineSummary) {
        this.lastPipelineSummary = lastPipelineSummary;
        lastUpdate = LocalDateTime.now();
    }

    public synchronized String getStreamStatus() {
        return streamStatus;
    }

    public synchronized void setStreamStatus(String streamStatus) {
        this.streamStatus = streamStatus;
        lastUpdate = LocalDateTime.now();
    }

    public synchronized LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    private BusPosition copyPosition(BusPosition position) {
        return new BusPosition(position.getId(), position.getBusId(), position.getBusCode(), position.getRouteId(),
                position.getLatitude(), position.getLongitude(), position.getSpeed(), position.getTimestamp());
    }
}
