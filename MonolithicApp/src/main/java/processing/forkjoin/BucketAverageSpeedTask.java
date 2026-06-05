package processing.forkjoin;

import domain.RouteMonthKey;
import processing.aggregation.SpeedAccumulator;
import processing.benchmark.ProcessingCounters;
import processing.streaming.BucketedDatagramRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public final class BucketAverageSpeedTask extends RecursiveTask<PartialSpeedResult> {
    private static final Charset BUCKET_CHARSET = Charset.forName("UTF-8");
    private static final double MAX_INTERVAL_SECONDS = 600.0;
    private static final double MAX_SPEED_KMH = 120.0;

    private final List<Path> bucketFiles;
    private final Set<Integer> activeRouteIds;
    private final int start;
    private final int end;
    private final int threshold;

    public BucketAverageSpeedTask(List<Path> bucketFiles, Set<Integer> activeRouteIds, int start, int end, int threshold) {
        this.bucketFiles = bucketFiles;
        this.activeRouteIds = activeRouteIds;
        this.start = start;
        this.end = end;
        this.threshold = Math.max(1, threshold);
    }

    @Override
    protected PartialSpeedResult compute() {
        int size = end - start;
        if (size <= threshold) {
            return processBucketsDirectly();
        }

        int middle = start + (size / 2);
        BucketAverageSpeedTask left = new BucketAverageSpeedTask(bucketFiles, activeRouteIds, start, middle, threshold);
        BucketAverageSpeedTask right = new BucketAverageSpeedTask(bucketFiles, activeRouteIds, middle, end, threshold);

        left.fork();
        PartialSpeedResult rightResult = right.compute();
        PartialSpeedResult leftResult = left.join();
        leftResult.merge(rightResult);
        return leftResult;
    }

    private PartialSpeedResult processBucketsDirectly() {
        PartialSpeedResult result = new PartialSpeedResult();
        for (int i = start; i < end; i++) {
            processBucket(bucketFiles.get(i), result);
        }
        return result;
    }

    private void processBucket(Path bucketFile, PartialSpeedResult result) {
        Map<String, BucketedDatagramRecord> previousByBus = new HashMap<String, BucketedDatagramRecord>();
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(bucketFile, BUCKET_CHARSET);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                BucketedDatagramRecord current;
                try {
                    current = BucketedDatagramRecord.fromCompactLine(line);
                } catch (RuntimeException exception) {
                    result.getCounters().incrementMalformedDatagrams();
                    continue;
                }
                processRecord(current, previousByBus, result);
            }
        } catch (IOException exception) {
            result.getCounters().incrementMalformedDatagrams();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    result.getCounters().incrementMalformedDatagrams();
                }
            }
        }
    }

    private void processRecord(
            BucketedDatagramRecord current,
            Map<String, BucketedDatagramRecord> previousByBus,
            PartialSpeedResult result
    ) {
        ProcessingCounters counters = result.getCounters();
        counters.incrementGroupedDatagrams();

        BucketedDatagramRecord previous = previousByBus.get(current.getBusId());
        if (previous == null) {
            counters.incrementProcessedBuses();
            counters.incrementDiscardNoPreviousPoint();
            previousByBus.put(current.getBusId(), current);
            return;
        }

        if (!isActiveRoute(previous.getRouteId()) || !isActiveRoute(current.getRouteId())) {
            counters.incrementNoRouteOrInactive();
            previousByBus.put(current.getBusId(), current);
            return;
        }
        if (previous.getRouteId() != current.getRouteId()) {
            counters.incrementDiscardRouteChanged();
            previousByBus.put(current.getBusId(), current);
            return;
        }

        double deltaTimeSeconds = Duration.between(previous.getTimestamp(), current.getTimestamp()).toMillis() / 1000.0;
        if (deltaTimeSeconds <= 0.0) {
            counters.incrementDiscardDeltaTimeInvalid();
            previousByBus.put(current.getBusId(), current);
            return;
        }
        if (deltaTimeSeconds > MAX_INTERVAL_SECONDS) {
            counters.incrementDiscardDeltaTimeTooLong();
            previousByBus.put(current.getBusId(), current);
            return;
        }
        if (previous.getOdometer() < 0.0 || current.getOdometer() < 0.0) {
            counters.incrementDiscardBadOdometer();
            previousByBus.put(current.getBusId(), current);
            return;
        }

        double deltaDistanceMeters = current.getOdometer() - previous.getOdometer();
        if (deltaDistanceMeters <= 0.0) {
            counters.incrementDiscardNoDistanceGain();
            previousByBus.put(current.getBusId(), current);
            return;
        }

        double speedKmh = (deltaDistanceMeters / deltaTimeSeconds) * 3.6;
        if (speedKmh > MAX_SPEED_KMH) {
            counters.incrementDiscardSpeedTooHigh();
            previousByBus.put(current.getBusId(), current);
            return;
        }

        RouteMonthKey key = new RouteMonthKey(current.getRouteId(), current.getTimestamp().getYear(), current.getTimestamp().getMonthValue());
        SpeedAccumulator accumulator = result.getAccumulators().get(key);
        if (accumulator == null) {
            accumulator = new SpeedAccumulator();
            result.getAccumulators().put(key, accumulator);
        }
        accumulator.addInterval(deltaDistanceMeters, deltaTimeSeconds);
        counters.incrementValidIntervals();
        previousByBus.put(current.getBusId(), current);
    }

    private boolean isActiveRoute(int routeId) {
        return routeId > 0 && activeRouteIds.contains(Integer.valueOf(routeId));
    }
}
