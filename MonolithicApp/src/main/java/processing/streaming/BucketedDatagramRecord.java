package processing.streaming;

import domain.Datagram;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BucketedDatagramRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "\t";

    private final String busId;
    private final int routeId;
    private final double odometer;
    private final LocalDateTime timestamp;
    private final Double latitude;
    private final Double longitude;

    public BucketedDatagramRecord(String busId, int routeId, double odometer, LocalDateTime timestamp, Double latitude, Double longitude) {
        this.busId = busId == null ? "" : busId.trim();
        this.routeId = routeId;
        this.odometer = odometer;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static BucketedDatagramRecord fromCompactLine(String line) {
        String[] parts = line.split(SEPARATOR, -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Bucket line has fewer columns than expected");
        }
        return new BucketedDatagramRecord(
                parts[0],
                Integer.parseInt(parts[1]),
                Double.parseDouble(parts[2]),
                LocalDateTime.parse(parts[3], FORMATTER),
                parseNullableDouble(parts[4]),
                parseNullableDouble(parts[5])
        );
    }

    public String toCompactLine() {
        return busId + SEPARATOR
                + routeId + SEPARATOR
                + odometer + SEPARATOR
                + FORMATTER.format(timestamp) + SEPARATOR
                + nullable(latitude) + SEPARATOR
                + nullable(longitude);
    }

    public Datagram toDatagram() {
        return new Datagram("", busId, routeId, timestamp, odometer, latitude, longitude);
    }

    public String getBusId() {
        return busId;
    }

    public int getRouteId() {
        return routeId;
    }

    public double getOdometer() {
        return odometer;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    private static String nullable(Double value) {
        return value == null ? "" : value.toString();
    }

    private static Double parseNullableDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Double.valueOf(value);
    }
}
