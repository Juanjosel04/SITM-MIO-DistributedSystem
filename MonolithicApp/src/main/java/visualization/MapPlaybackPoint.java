package visualization;

public final class MapPlaybackPoint {
    private final String busId;
    private final int routeId;
    private final String routeLabel;
    private final double latitude;
    private final double longitude;
    private final String timestampText;
    private final String averageSpeedText;

    public MapPlaybackPoint(
            String busId,
            int routeId,
            String routeLabel,
            double latitude,
            double longitude,
            String timestampText,
            String averageSpeedText
    ) {
        this.busId = busId == null ? "" : busId.trim();
        this.routeId = routeId;
        this.routeLabel = routeLabel == null ? "" : routeLabel.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestampText = timestampText == null ? "" : timestampText.trim();
        this.averageSpeedText = averageSpeedText == null ? "Sin datos" : averageSpeedText.trim();
    }

    public String getBusId() {
        return busId;
    }

    public int getRouteId() {
        return routeId;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestampText() {
        return timestampText;
    }

    public String getAverageSpeedText() {
        return averageSpeedText;
    }
}
