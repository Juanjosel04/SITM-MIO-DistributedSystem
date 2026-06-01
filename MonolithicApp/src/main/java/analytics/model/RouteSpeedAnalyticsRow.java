package analytics.model;

public class RouteSpeedAnalyticsRow {
    private final int routeId;
    private final String yearLabel;
    private final String monthLabel;
    private final Double monthlyAverageSpeed;
    private final Double yearlyAverageSpeed;
    private final int processedDatagrams;

    public RouteSpeedAnalyticsRow(int routeId, String yearLabel, String monthLabel, Double monthlyAverageSpeed,
                                  Double yearlyAverageSpeed, int processedDatagrams) {
        this.routeId = routeId;
        this.yearLabel = yearLabel;
        this.monthLabel = monthLabel;
        this.monthlyAverageSpeed = monthlyAverageSpeed;
        this.yearlyAverageSpeed = yearlyAverageSpeed;
        this.processedDatagrams = processedDatagrams;
    }

    public int getRouteId() {
        return routeId;
    }

    public String getYearLabel() {
        return yearLabel;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public Double getMonthlyAverageSpeed() {
        return monthlyAverageSpeed;
    }

    public Double getYearlyAverageSpeed() {
        return yearlyAverageSpeed;
    }

    public int getProcessedDatagrams() {
        return processedDatagrams;
    }
}
