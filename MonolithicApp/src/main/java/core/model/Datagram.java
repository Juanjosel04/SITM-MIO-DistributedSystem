package core.model;

import java.time.LocalDateTime;

public class Datagram {
    private String id;
    private String busCode;
    private int routeId;
    private double latitude;
    private double longitude;
    private Double speed;
    private String eventCode;
    private LocalDateTime timestamp;
    private String rawPayload;

    public Datagram() {
    }

    public Datagram(String id, String busCode, int routeId, double latitude, double longitude, Double speed,
                    String eventCode, LocalDateTime timestamp, String rawPayload) {
        this.id = id;
        this.busCode = busCode;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.eventCode = eventCode;
        this.timestamp = timestamp;
        this.rawPayload = rawPayload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusCode() {
        return busCode;
    }

    public void setBusCode(String busCode) {
        this.busCode = busCode;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
