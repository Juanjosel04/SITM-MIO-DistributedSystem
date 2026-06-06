package master.merge;

public final class GlobalMergedResultRow {
    private final String routeId;
    private final int year;
    private final int month;
    private final double totalDistanceMeters;
    private final double totalTimeSeconds;
    private final long validIntervals;
    private final double averageKmh;
    private final int contributingWorkers;

    public GlobalMergedResultRow(GlobalRouteMonthKey key, GlobalSpeedAccumulator accumulator) {
        this.routeId = key.getRouteId();
        this.year = key.getYear();
        this.month = key.getMonth();
        this.totalDistanceMeters = accumulator.getTotalDistanceMeters();
        this.totalTimeSeconds = accumulator.getTotalTimeSeconds();
        this.validIntervals = accumulator.getValidIntervals();
        this.averageKmh = accumulator.averageKmh();
        this.contributingWorkers = accumulator.getContributingWorkerCount();
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

    public String getYearMonth() {
        return year + "-" + String.format("%02d", month);
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public double getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public long getValidIntervals() {
        return validIntervals;
    }

    public double getAverageKmh() {
        return averageKmh;
    }

    public int getContributingWorkers() {
        return contributingWorkers;
    }
}
