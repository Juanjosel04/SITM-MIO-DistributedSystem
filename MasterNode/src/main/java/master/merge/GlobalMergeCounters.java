package master.merge;

import master.results.RemoteWorkerPartialResult;
import sitm.ProcessingCountersDto;

public final class GlobalMergeCounters {
    private long totalLinesRead;
    private long validIntervals;
    private long invalidLines;
    private long missingBusId;
    private long missingRouteId;
    private long routeIdMinusOne;
    private long invalidTimestamp;
    private long invalidOdometer;
    private long negativeOdometer;
    private long nonPositiveDeltaTime;
    private long excessiveDeltaTime;
    private long nonPositiveDistance;
    private long routeChanged;
    private long speedTooHigh;
    private long firstRecordsByBus;
    private int totalProcessedBuckets;
    private long totalElapsedRemoteMillis;

    public void mergeFrom(RemoteWorkerPartialResult result) {
        totalProcessedBuckets += result.getProcessedBuckets();
        totalElapsedRemoteMillis += result.getElapsedMillis();
        ProcessingCountersDto counters = result.getCounters();
        if (counters == null) {
            return;
        }
        totalLinesRead += counters.totalLinesRead;
        validIntervals += counters.validIntervals;
        invalidLines += counters.invalidLines;
        missingBusId += counters.missingBusId;
        missingRouteId += counters.missingRouteId;
        routeIdMinusOne += counters.routeIdMinusOne;
        invalidTimestamp += counters.invalidTimestamp;
        invalidOdometer += counters.invalidOdometer;
        negativeOdometer += counters.negativeOdometer;
        nonPositiveDeltaTime += counters.nonPositiveDeltaTime;
        excessiveDeltaTime += counters.excessiveDeltaTime;
        nonPositiveDistance += counters.nonPositiveDistance;
        routeChanged += counters.routeChanged;
        speedTooHigh += counters.speedTooHigh;
        firstRecordsByBus += counters.firstRecordsByBus;
    }

    public long getTotalLinesRead() {
        return totalLinesRead;
    }

    public long getValidIntervals() {
        return validIntervals;
    }

    public long getInvalidLines() {
        return invalidLines;
    }

    public long getMissingBusId() {
        return missingBusId;
    }

    public long getMissingRouteId() {
        return missingRouteId;
    }

    public long getRouteIdMinusOne() {
        return routeIdMinusOne;
    }

    public long getInvalidTimestamp() {
        return invalidTimestamp;
    }

    public long getInvalidOdometer() {
        return invalidOdometer;
    }

    public long getNegativeOdometer() {
        return negativeOdometer;
    }

    public long getNonPositiveDeltaTime() {
        return nonPositiveDeltaTime;
    }

    public long getExcessiveDeltaTime() {
        return excessiveDeltaTime;
    }

    public long getNonPositiveDistance() {
        return nonPositiveDistance;
    }

    public long getRouteChanged() {
        return routeChanged;
    }

    public long getSpeedTooHigh() {
        return speedTooHigh;
    }

    public long getFirstRecordsByBus() {
        return firstRecordsByBus;
    }

    public int getTotalProcessedBuckets() {
        return totalProcessedBuckets;
    }

    public long getTotalElapsedRemoteMillis() {
        return totalElapsedRemoteMillis;
    }
}
