package processing.forkjoin;

import domain.RouteMonthKey;
import processing.aggregation.SpeedAccumulator;
import processing.benchmark.ProcessingCounters;

import java.util.HashMap;
import java.util.Map;

public final class PartialSpeedResult {
    private final Map<RouteMonthKey, SpeedAccumulator> accumulators;
    private final ProcessingCounters counters;

    public PartialSpeedResult() {
        this(new HashMap<RouteMonthKey, SpeedAccumulator>(), new ProcessingCounters());
    }

    public PartialSpeedResult(Map<RouteMonthKey, SpeedAccumulator> accumulators, ProcessingCounters counters) {
        this.accumulators = accumulators == null ? new HashMap<RouteMonthKey, SpeedAccumulator>() : accumulators;
        this.counters = counters == null ? new ProcessingCounters() : counters;
    }

    public Map<RouteMonthKey, SpeedAccumulator> getAccumulators() {
        return accumulators;
    }

    public ProcessingCounters getCounters() {
        return counters;
    }

    public void merge(PartialSpeedResult other) {
        if (other == null) {
            return;
        }
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : other.accumulators.entrySet()) {
            SpeedAccumulator accumulator = accumulators.get(entry.getKey());
            if (accumulator == null) {
                accumulator = new SpeedAccumulator();
                accumulators.put(entry.getKey(), accumulator);
            }
            accumulator.merge(entry.getValue());
        }
        counters.merge(other.counters);
    }
}
