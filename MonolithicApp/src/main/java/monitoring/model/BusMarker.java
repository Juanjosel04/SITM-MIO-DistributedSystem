package monitoring.model;

import java.time.LocalDateTime;

public class BusMarker {
    private String busCode;
    private int routeId;
    private double latitude;
    private double longitude;
    private Double speed;
    private LocalDateTime lastUpdate;
    private String status;

    public BusMarker(String busCode, int routeId, double latitude, double longitude, Double speed,
                     LocalDateTime lastUpdate, String status) {
        this.busCode = busCode;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.lastUpdate = lastUpdate;
        this.status = status;
    }

    public String getBusCode() {
        return busCode;
    }

    public int getRouteId() {
        return routeId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String getStatus() {
        return status;
    }
}
