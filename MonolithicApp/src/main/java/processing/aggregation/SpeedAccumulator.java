package processing.aggregation;

public final class SpeedAccumulator {
    private double sumDistanceMeters;
    private double sumTimeSeconds;
    private long intervals;

    public void addInterval(double distanceMeters, double timeSeconds) {
        sumDistanceMeters += distanceMeters;
        sumTimeSeconds += timeSeconds;
        intervals++;
    }

    public void merge(SpeedAccumulator other) {
        if (other == null || !other.hasData()) {
            return;
        }
        sumDistanceMeters += other.sumDistanceMeters;
        sumTimeSeconds += other.sumTimeSeconds;
        intervals += other.intervals;
    }

    public double getSumDistanceMeters() {
        return sumDistanceMeters;
    }

    public double getSumTimeSeconds() {
        return sumTimeSeconds;
    }

    public long getIntervals() {
        return intervals;
    }

    public long getCount() {
        return intervals;
    }

    public double getAverageKmh() {
        if (!hasData()) {
            return 0.0;
        }
        return (sumDistanceMeters / sumTimeSeconds) * 3.6;
    }

    public boolean hasData() {
        return intervals > 0 && sumTimeSeconds > 0.0;
    }
}
