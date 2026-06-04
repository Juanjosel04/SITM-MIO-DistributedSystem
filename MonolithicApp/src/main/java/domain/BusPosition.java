package domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class BusPosition {
    private final String busId;
    private final int routeId;
    private final double latitude;
    private final double longitude;
    private final LocalDateTime timestamp;
    private final double speed;

    public BusPosition(String busId, int routeId, double latitude, double longitude, LocalDateTime timestamp, double speed) {
        this.busId = busId == null ? "" : busId.trim();
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.speed = speed;
    }

    public String getBusId() {
        return busId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getSpeed() {
        return speed;
    }
}
