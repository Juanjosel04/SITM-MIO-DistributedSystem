package processing.aggregation;

public final class SpeedAccumulator {
    private double sumSpeed;
    private long count;

    public void addSpeed(double speed) {
        sumSpeed += speed;
        count++;
    }

    public void merge(SpeedAccumulator other) {
        if (other == null || !other.hasData()) {
            return;
        }
        sumSpeed += other.sumSpeed;
        count += other.count;
    }

    public double getSumSpeed() {
        return sumSpeed;
    }

    public long getCount() {
        return count;
    }

    public double getAverage() {
        if (!hasData()) {
            return 0.0;
        }
        return sumSpeed / count;
    }

    public boolean hasData() {
        return count > 0;
    }
}
