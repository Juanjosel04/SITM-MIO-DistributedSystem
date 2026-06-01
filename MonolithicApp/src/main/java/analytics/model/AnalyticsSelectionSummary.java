package analytics.model;

public class AnalyticsSelectionSummary {
    private final Double monthlyAverageSpeed;
    private final Double yearlyAverageSpeed;
    private final int processedDatagrams;

    public AnalyticsSelectionSummary(Double monthlyAverageSpeed, Double yearlyAverageSpeed, int processedDatagrams) {
        this.monthlyAverageSpeed = monthlyAverageSpeed;
        this.yearlyAverageSpeed = yearlyAverageSpeed;
        this.processedDatagrams = processedDatagrams;
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
