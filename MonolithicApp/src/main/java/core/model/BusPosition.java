package core.model;

import java.time.LocalDateTime;

public class BusPosition {
    private long id;
    private long busId;
    private String busCode;
    private int routeId;
    private double latitude;
    private double longitude;
    private Double speed;
    private LocalDateTime timestamp;

    public BusPosition() {
    }

    public BusPosition(long id, long busId, String busCode, int routeId, double latitude, double longitude,
                       Double speed, LocalDateTime timestamp) {
        this.id = id;
        this.busId = busId;
        this.busCode = busCode;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBusId() {
        return busId;
    }

    public void setBusId(long busId) {
        this.busId = busId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
