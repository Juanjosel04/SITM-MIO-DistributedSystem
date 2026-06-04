package domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Datagram {
    private final String id;
    private final String busId;
    private final int routeId;
    private final LocalDateTime timestamp;
    private final double speed;
    private final Double latitude;
    private final Double longitude;

    public Datagram(
            String id,
            String busId,
            int routeId,
            LocalDateTime timestamp,
            double speed,
            Double latitude,
            Double longitude
    ) {
        this.id = normalize(id);
        this.busId = normalize(busId);
        this.routeId = routeId;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getBusId() {
        return busId;
    }

    public int getRouteId() {
        return routeId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getYear() {
        return timestamp.getYear();
    }

    public int getMonth() {
        return timestamp.getMonthValue();
    }

    public double getSpeed() {
        return speed;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean hasPosition() {
        return latitude != null && longitude != null;
    }

    public BusPosition toBusPosition() {
        if (!hasPosition()) {
            throw new IllegalStateException("Datagram does not contain coordinates");
        }
        return new BusPosition(busId, routeId, latitude.doubleValue(), longitude.doubleValue(), timestamp, speed);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
