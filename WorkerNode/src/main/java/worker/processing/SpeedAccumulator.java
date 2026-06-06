package worker.processing;

public final class SpeedAccumulator {
    private double totalDistanceMeters;
    private double totalTimeSeconds;
    private long validIntervals;

    public void add(double distanceMeters, double timeSeconds) {
        totalDistanceMeters += distanceMeters;
        totalTimeSeconds += timeSeconds;
        validIntervals++;
    }

    public void merge(SpeedAccumulator other) {
        totalDistanceMeters += other.totalDistanceMeters;
        totalTimeSeconds += other.totalTimeSeconds;
        validIntervals += other.validIntervals;
    }

    public double averageKmh() {
        if (totalTimeSeconds <= 0) {
            return 0;
        }
        return totalDistanceMeters / totalTimeSeconds * 3.6;
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
}
