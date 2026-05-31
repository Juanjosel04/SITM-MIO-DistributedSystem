package events.model;

import java.time.LocalDateTime;

public class DriverEventRequest {
    private String busCode;
    private int routeId;
    private String eventCode;
    private String description;
    private LocalDateTime timestamp;

    public DriverEventRequest(String busCode, int routeId, String eventCode, String description,
                              LocalDateTime timestamp) {
        this.busCode = busCode;
        this.routeId = routeId;
        this.eventCode = eventCode;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getBusCode() {
        return busCode;
    }

    public int getRouteId() {
        return routeId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
