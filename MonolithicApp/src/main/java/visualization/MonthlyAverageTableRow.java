package visualization;

import domain.MonthlyRouteAverage;

import java.util.Locale;

public final class MonthlyAverageTableRow {
    private final int routeId;
    private final int year;
    private final int month;
    private final String route;
    private final String yearText;
    private final String monthText;
    private final String averageSpeed;
    private final String intervalCount;
    private final String status;

    public MonthlyAverageTableRow(MonthlyRouteAverage average) {
        this.routeId = average.getRouteId();
        this.year = average.getYear();
        this.month = average.getMonth();
        this.route = average.getRouteLabel() == null || average.getRouteLabel().trim().isEmpty()
                ? "Route " + average.getRouteId()
                : average.getRouteLabel().trim();
        this.yearText = String.valueOf(average.getYear());
        this.monthText = String.format(Locale.US, "%02d", Integer.valueOf(average.getMonth()));
        this.averageSpeed = average.hasData() ? String.format(Locale.US, "%.2f", Double.valueOf(average.getAverageSpeed())) : "-";
        this.intervalCount = String.valueOf(average.getIntervalCount());
        this.status = average.hasData() ? "Con datos" : "Sin datos";
    }

    public int getRouteId() {
        return routeId;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public String getRoute() {
        return route;
    }

    public String getYearText() {
        return yearText;
    }

    public String getMonthText() {
        return monthText;
    }

    public String getAverageSpeed() {
        return averageSpeed;
    }

    public String getIntervalCount() {
        return intervalCount;
    }

    public String getStatus() {
        return status;
    }
}
