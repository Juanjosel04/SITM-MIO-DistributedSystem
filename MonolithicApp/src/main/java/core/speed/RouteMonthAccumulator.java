package core.speed;

public final class RouteMonthAccumulator {

    private double sumDistanceMeters = 0.0;
    private double sumTimeSeconds    = 0.0;
    private long   intervals         = 0;

    public void add(double distanceMeters, double timeSeconds) {
        sumDistanceMeters += distanceMeters;
        sumTimeSeconds    += timeSeconds;
        intervals++;
    }

    /** Average speed in km/h: (totalDistance_m / totalTime_s) * 3.6 */
    public double avgKmh() {
        if (sumTimeSeconds <= 0.0) return 0.0;
        return (sumDistanceMeters / sumTimeSeconds) * 3.6;
    }

    public long intervals()           { return intervals; }
    public double sumDistanceMeters() { return sumDistanceMeters; }
    public double sumTimeSeconds()    { return sumTimeSeconds; }
}
