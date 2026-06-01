package analytics.model;

public class AnalyticsFilter {
    private final Integer routeId;
    private final Integer year;
    private final Integer month;

    public AnalyticsFilter(Integer routeId, Integer year, Integer month) {
        this.routeId = routeId;
        this.year = year;
        this.month = month;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }
}
