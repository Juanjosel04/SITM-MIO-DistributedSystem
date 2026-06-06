package master.results;

public final class RemoteRouteMonthPartial {
    private final String workerId;
    private final String routeId;
    private final int year;
    private final int month;
    private final double totalDistanceMeters;
    private final double totalTimeSeconds;
    private final int validIntervals;
    private final double averageKmh;

    public RemoteRouteMonthPartial(String workerId, String routeId, int year, int month,
                                   double totalDistanceMeters, double totalTimeSeconds,
                                   int validIntervals, double averageKmh) {
        this.workerId = workerId;
        this.routeId = routeId;
        this.year = year;
        this.month = month;
        this.totalDistanceMeters = totalDistanceMeters;
        this.totalTimeSeconds = totalTimeSeconds;
        this.validIntervals = validIntervals;
        this.averageKmh = averageKmh;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getRouteId() {
        return routeId;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public double getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public int getValidIntervals() {
        return validIntervals;
    }

    public double getAverageKmh() {
        return averageKmh;
    }
}
