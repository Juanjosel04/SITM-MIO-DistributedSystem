package worker.processing;

import java.time.LocalDateTime;

public final class CompactBucketRecord {
    private final String busId;
    private final String routeId;
    private final double odometer;
    private final LocalDateTime timestamp;
    private final String latitude;
    private final String longitude;

    public CompactBucketRecord(String busId, String routeId, double odometer, LocalDateTime timestamp,
                               String latitude, String longitude) {
        this.busId = busId;
        this.routeId = routeId;
        this.odometer = odometer;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getBusId() {
        return busId;
    }

    public String getRouteId() {
        return routeId;
    }

    public double getOdometer() {
        return odometer;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
