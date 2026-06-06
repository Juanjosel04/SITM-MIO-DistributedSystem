package worker.processing;

import java.time.YearMonth;

public final class RouteMonthKey {
    private final String routeId;
    private final YearMonth yearMonth;

    public RouteMonthKey(String routeId, YearMonth yearMonth) {
        this.routeId = routeId;
        this.yearMonth = yearMonth;
    }

    public static RouteMonthKey from(String routeId, java.time.LocalDateTime timestamp) {
        return new RouteMonthKey(routeId, YearMonth.from(timestamp));
    }

    public String getRouteId() {
        return routeId;
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public String display() {
        return routeId + " " + yearMonth;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RouteMonthKey)) {
            return false;
        }
        RouteMonthKey that = (RouteMonthKey) other;
        return routeId.equals(that.routeId) && yearMonth.equals(that.yearMonth);
    }

    @Override
    public int hashCode() {
        int result = routeId.hashCode();
        result = 31 * result + yearMonth.hashCode();
        return result;
    }
}
