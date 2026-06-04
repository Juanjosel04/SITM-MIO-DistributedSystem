package processing.forkjoin;

import domain.Datagram;
import domain.RouteMonthKey;
import processing.aggregation.AverageSpeedAggregator;
import processing.aggregation.SpeedAccumulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

public final class AverageSpeedTask extends RecursiveTask<Map<RouteMonthKey, SpeedAccumulator>> {
    private final List<Datagram> datagrams;
    private final int start;
    private final int end;
    private final int threshold;
    private final AverageSpeedAggregator aggregator;

    public AverageSpeedTask(List<Datagram> datagrams, int start, int end, int threshold) {
        this.datagrams = datagrams;
        this.start = start;
        this.end = end;
        this.threshold = Math.max(1, threshold);
        this.aggregator = new AverageSpeedAggregator();
    }

    @Override
    protected Map<RouteMonthKey, SpeedAccumulator> compute() {
        int size = end - start;
        if (size <= threshold) {
            return computeSequentially();
        }

        int middle = start + (size / 2);
        AverageSpeedTask leftTask = new AverageSpeedTask(datagrams, start, middle, threshold);
        AverageSpeedTask rightTask = new AverageSpeedTask(datagrams, middle, end, threshold);

        leftTask.fork();
        Map<RouteMonthKey, SpeedAccumulator> rightResult = rightTask.compute();
        Map<RouteMonthKey, SpeedAccumulator> leftResult = leftTask.join();

        return aggregator.merge(leftResult, rightResult);
    }

    private Map<RouteMonthKey, SpeedAccumulator> computeSequentially() {
        Map<RouteMonthKey, SpeedAccumulator> result = new HashMap<RouteMonthKey, SpeedAccumulator>();
        for (int i = start; i < end; i++) {
            Datagram datagram = datagrams.get(i);
            if (!isValid(datagram)) {
                continue;
            }

            RouteMonthKey key = RouteMonthKey.from(datagram);
            SpeedAccumulator accumulator = result.get(key);
            if (accumulator == null) {
                accumulator = new SpeedAccumulator();
                result.put(key, accumulator);
            }
            accumulator.addSpeed(datagram.getSpeed());
        }
        return result;
    }

    private boolean isValid(Datagram datagram) {
        if (datagram == null || datagram.getRouteId() <= 0 || datagram.getTimestamp() == null) {
            return false;
        }
        double speed = datagram.getSpeed();
        return !Double.isNaN(speed) && !Double.isInfinite(speed) && speed >= 0.0;
    }
}
