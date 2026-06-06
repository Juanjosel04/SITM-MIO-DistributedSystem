package master.merge;

public final class GlobalRouteMonthKey implements Comparable<GlobalRouteMonthKey> {
    private final String routeId;
    private final int year;
    private final int month;

    public GlobalRouteMonthKey(String routeId, int year, int month) {
        this.routeId = routeId == null ? "" : routeId.trim();
        this.year = year;
        this.month = month;
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

    public String yearMonth() {
        return year + "-" + String.format("%02d", month);
    }

    @Override
    public int compareTo(GlobalRouteMonthKey other) {
        int route = routeId.compareTo(other.routeId);
        if (route != 0) {
            return route;
        }
        int yearCompare = Integer.compare(year, other.year);
        if (yearCompare != 0) {
            return yearCompare;
        }
        return Integer.compare(month, other.month);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GlobalRouteMonthKey)) {
            return false;
        }
        GlobalRouteMonthKey that = (GlobalRouteMonthKey) other;
        return year == that.year && month == that.month && routeId.equals(that.routeId);
    }

    @Override
    public int hashCode() {
        int result = routeId.hashCode();
        result = 31 * result + year;
        result = 31 * result + month;
        return result;
    }
}
