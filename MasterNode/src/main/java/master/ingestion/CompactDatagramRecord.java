package master.ingestion;

public final class CompactDatagramRecord {
    public static final String HEADER = "busId,routeId,odometer,timestamp,latitude,longitude";

    private final String busId;
    private final String routeId;
    private final String odometer;
    private final String timestamp;
    private final String latitude;
    private final String longitude;

    public CompactDatagramRecord(String busId, String routeId, String odometer, String timestamp,
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

    public String getTimestamp() {
        return timestamp;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String toCompactCsvLine() {
        return escape(busId) + "," + escape(routeId) + "," + escape(odometer) + ","
                + escape(timestamp) + "," + escape(latitude) + "," + escape(longitude);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
