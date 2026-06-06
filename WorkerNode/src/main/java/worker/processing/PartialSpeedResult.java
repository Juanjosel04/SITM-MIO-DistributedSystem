package worker.processing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PartialSpeedResult {
    private final Map<RouteMonthKey, SpeedAccumulator> accumulators = new LinkedHashMap<RouteMonthKey, SpeedAccumulator>();
    private final ProcessingCounters counters = new ProcessingCounters();
    private final List<Path> processedBuckets = new ArrayList<Path>();
    private boolean success = true;
    private String message = "Processing completed";
    private long elapsedMillis;

    public static PartialSpeedResult failure(String message) {
        PartialSpeedResult result = new PartialSpeedResult();
        result.success = false;
        result.message = message;
        return result;
    }

    public void addInterval(RouteMonthKey key, double distanceMeters, double timeSeconds) {
        SpeedAccumulator accumulator = accumulators.get(key);
        if (accumulator == null) {
            accumulator = new SpeedAccumulator();
            accumulators.put(key, accumulator);
        }
        accumulator.add(distanceMeters, timeSeconds);
        counters.countValidInterval();
    }

    public void addProcessedBucket(Path bucket) {
        processedBuckets.add(bucket);
    }

    public void merge(PartialSpeedResult other) {
        success = success && other.success;
        if (!other.success) {
            message = other.message;
        }
        counters.merge(other.counters);
        processedBuckets.addAll(other.processedBuckets);
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : other.accumulators.entrySet()) {
            SpeedAccumulator accumulator = accumulators.get(entry.getKey());
            if (accumulator == null) {
                accumulator = new SpeedAccumulator();
                accumulators.put(entry.getKey(), accumulator);
            }
            accumulator.merge(entry.getValue());
        }
        elapsedMillis += other.elapsedMillis;
    }

    public Map<RouteMonthKey, SpeedAccumulator> getAccumulators() {
        return Collections.unmodifiableMap(accumulators);
    }

    public ProcessingCounters getCounters() {
        return counters;
    }

    public List<Path> getProcessedBuckets() {
        return Collections.unmodifiableList(processedBuckets);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }
}
