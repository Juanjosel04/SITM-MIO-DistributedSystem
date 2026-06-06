package master.map;

public final class MapPlaybackPoint {
    private final String busId;
    private final String routeId;
    private final String timestamp;
    private final double latitude;
    private final double longitude;
    private final long sequence;

    public MapPlaybackPoint(String busId, String routeId, String timestamp, double latitude, double longitude,
                            long sequence) {
        this.busId = busId == null ? "" : busId.trim();
        this.routeId = routeId == null ? "" : routeId.trim();
        this.timestamp = timestamp == null ? "" : timestamp.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.sequence = sequence;
    }

    public String getBusId() {
        return busId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getSequence() {
        return sequence;
    }
}
