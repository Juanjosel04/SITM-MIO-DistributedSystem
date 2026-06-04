package domain;

public final class MonthlyRouteAverage {
    private final int routeId;
    private final String routeLabel;
    private final int year;
    private final int month;
    private final double averageSpeed;
    private final long datagramCount;
    private final boolean hasData;

    public MonthlyRouteAverage(
            int routeId,
            String routeLabel,
            int year,
            int month,
            double averageSpeed,
            long datagramCount
    ) {
        this.routeId = routeId;
        this.routeLabel = routeLabel == null ? "" : routeLabel.trim();
        this.year = year;
        this.month = month;
        this.averageSpeed = averageSpeed;
        this.datagramCount = datagramCount;
        this.hasData = datagramCount > 0;
    }

    public static MonthlyRouteAverage noData(int routeId, String routeLabel, int year, int month) {
        return new MonthlyRouteAverage(routeId, routeLabel, year, month, 0.0, 0L);
    }

    public int getRouteId() {
        return routeId;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public long getDatagramCount() {
        return datagramCount;
    }

    public boolean hasData() {
        return hasData;
    }
}
