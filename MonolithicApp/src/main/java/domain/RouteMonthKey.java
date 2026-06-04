package domain;

import java.util.Objects;

public final class RouteMonthKey implements Comparable<RouteMonthKey> {
    private final int routeId;
    private final int year;
    private final int month;

    public RouteMonthKey(int routeId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        this.routeId = routeId;
        this.year = year;
        this.month = month;
    }

    public static RouteMonthKey from(Datagram datagram) {
        return new RouteMonthKey(datagram.getRouteId(), datagram.getYear(), datagram.getMonth());
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

    @Override
    public int compareTo(RouteMonthKey other) {
        int routeComparison = Integer.compare(routeId, other.routeId);
        if (routeComparison != 0) {
            return routeComparison;
        }
        int yearComparison = Integer.compare(year, other.year);
        if (yearComparison != 0) {
            return yearComparison;
        }
        return Integer.compare(month, other.month);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RouteMonthKey)) {
            return false;
        }
        RouteMonthKey that = (RouteMonthKey) object;
        return routeId == that.routeId && year == that.year && month == that.month;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(routeId), Integer.valueOf(year), Integer.valueOf(month));
    }

    @Override
    public String toString() {
        return routeId + "-" + year + "-" + String.format("%02d", Integer.valueOf(month));
    }
}
