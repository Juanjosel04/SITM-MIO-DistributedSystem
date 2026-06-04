package processing.forkjoin;

import domain.Datagram;
import domain.RouteMonthKey;
import processing.aggregation.SpeedAccumulator;
import processing.benchmark.ProcessingCounters;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public final class AverageSpeedTask extends RecursiveTask<PartialSpeedResult> {
    private static final double MAX_INTERVAL_SECONDS = 600.0;
    private static final double MAX_SPEED_KMH = 120.0;

    private final List<BusDatagramGroup> busGroups;
    private final Set<Integer> activeRouteIds;
    private final int start;
    private final int end;
    private final int threshold;

    public AverageSpeedTask(
            List<BusDatagramGroup> busGroups,
            Set<Integer> activeRouteIds,
            int start,
            int end,
            int threshold
    ) {
        this.busGroups = busGroups;
        this.activeRouteIds = activeRouteIds;
        this.start = start;
        this.end = end;
        this.threshold = Math.max(1, threshold);
    }

    @Override
    protected PartialSpeedResult compute() {
        int size = end - start;
        if (size <= threshold) {
            return computeSequentially();
        }

        int middle = start + (size / 2);
        AverageSpeedTask leftTask = new AverageSpeedTask(busGroups, activeRouteIds, start, middle, threshold);
        AverageSpeedTask rightTask = new AverageSpeedTask(busGroups, activeRouteIds, middle, end, threshold);

        leftTask.fork();
        PartialSpeedResult rightResult = rightTask.compute();
        PartialSpeedResult leftResult = leftTask.join();
        leftResult.merge(rightResult);
        return leftResult;
    }

    private PartialSpeedResult computeSequentially() {
        PartialSpeedResult result = new PartialSpeedResult();
        ProcessingCounters counters = result.getCounters();
        for (int i = start; i < end; i++) {
            BusDatagramGroup group = busGroups.get(i);
            if (group == null || group.getBusId().isEmpty()) {
                counters.incrementMalformedDatagrams();
                continue;
            }
            counters.incrementProcessedBuses();
            processBusGroup(group.getDatagrams(), result);
        }
        return result;
    }

    private void processBusGroup(List<Datagram> datagrams, PartialSpeedResult result) {
        ProcessingCounters counters = result.getCounters();
        Datagram previous = null;
        for (Datagram current : datagrams) {
            if (!isUsablePoint(current)) {
                counters.incrementMalformedDatagrams();
                continue;
            }

            counters.incrementGroupedDatagrams();
            if (previous == null) {
                counters.incrementDiscardNoPreviousPoint();
                previous = current;
                continue;
            }

            if (!isActiveRoute(previous.getRouteId()) || !isActiveRoute(current.getRouteId())) {
                counters.incrementNoRouteOrInactive();
                previous = current;
                continue;
            }
            if (previous.getRouteId() != current.getRouteId()) {
                counters.incrementDiscardRouteChanged();
                previous = current;
                continue;
            }

            double deltaTimeSeconds = Duration.between(previous.getTimestamp(), current.getTimestamp()).toMillis() / 1000.0;
            if (deltaTimeSeconds <= 0.0) {
                counters.incrementDiscardDeltaTimeInvalid();
                previous = current;
                continue;
            }
            if (deltaTimeSeconds > MAX_INTERVAL_SECONDS) {
                counters.incrementDiscardDeltaTimeTooLong();
                previous = current;
                continue;
            }
            if (previous.getOdometer() < 0.0 || current.getOdometer() < 0.0) {
                counters.incrementDiscardBadOdometer();
                previous = current;
                continue;
            }

            double deltaDistanceMeters = current.getOdometer() - previous.getOdometer();
            if (deltaDistanceMeters <= 0.0) {
                counters.incrementDiscardNoDistanceGain();
                previous = current;
                continue;
            }

            double speedKmh = (deltaDistanceMeters / deltaTimeSeconds) * 3.6;
            if (speedKmh > MAX_SPEED_KMH) {
                counters.incrementDiscardSpeedTooHigh();
                previous = current;
                continue;
            }

            RouteMonthKey key = RouteMonthKey.from(current);
            SpeedAccumulator accumulator = result.getAccumulators().get(key);
            if (accumulator == null) {
                accumulator = new SpeedAccumulator();
                result.getAccumulators().put(key, accumulator);
            }
            accumulator.addInterval(deltaDistanceMeters, deltaTimeSeconds);
            counters.incrementValidIntervals();
            previous = current;
        }
    }

    private boolean isUsablePoint(Datagram datagram) {
        return datagram != null
                && datagram.getTimestamp() != null
                && datagram.getBusId() != null
                && !datagram.getBusId().trim().isEmpty()
                && !Double.isNaN(datagram.getOdometer())
                && !Double.isInfinite(datagram.getOdometer());
    }

    private boolean isActiveRoute(int routeId) {
        return routeId > 0 && activeRouteIds.contains(Integer.valueOf(routeId));
    }
}
