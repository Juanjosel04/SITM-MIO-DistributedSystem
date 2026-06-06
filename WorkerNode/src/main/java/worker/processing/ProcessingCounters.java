package worker.processing;

public final class ProcessingCounters {
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

    public void countLine() {
        totalLinesRead++;
    }

    public void countValidInterval() {
        validIntervals++;
    }

    public void countInvalidLines() {
        invalidLines++;
    }

    public void countMissingBusId() {
        missingBusId++;
        invalidLines++;
    }

    public void countMissingRouteId() {
        missingRouteId++;
        invalidLines++;
    }

    public void countRouteIdMinusOne() {
        routeIdMinusOne++;
    }

    public void countInvalidTimestamp() {
        invalidTimestamp++;
        invalidLines++;
    }

    public void countInvalidOdometer() {
        invalidOdometer++;
        invalidLines++;
    }

    public void countNegativeOdometer() {
        negativeOdometer++;
    }

    public void countNonPositiveDeltaTime() {
        nonPositiveDeltaTime++;
    }

    public void countExcessiveDeltaTime() {
        excessiveDeltaTime++;
    }

    public void countNonPositiveDistance() {
        nonPositiveDistance++;
    }

    public void countRouteChanged() {
        routeChanged++;
    }

    public void countSpeedTooHigh() {
        speedTooHigh++;
    }

    public void countFirstRecordByBus() {
        firstRecordsByBus++;
    }

    public void merge(ProcessingCounters other) {
        totalLinesRead += other.totalLinesRead;
        validIntervals += other.validIntervals;
        invalidLines += other.invalidLines;
        missingBusId += other.missingBusId;
        missingRouteId += other.missingRouteId;
        routeIdMinusOne += other.routeIdMinusOne;
        invalidTimestamp += other.invalidTimestamp;
        invalidOdometer += other.invalidOdometer;
        negativeOdometer += other.negativeOdometer;
        nonPositiveDeltaTime += other.nonPositiveDeltaTime;
        excessiveDeltaTime += other.excessiveDeltaTime;
        nonPositiveDistance += other.nonPositiveDistance;
        routeChanged += other.routeChanged;
        speedTooHigh += other.speedTooHigh;
        firstRecordsByBus += other.firstRecordsByBus;
    }

    public long discardedIntervals() {
        return routeIdMinusOne + negativeOdometer + nonPositiveDeltaTime + excessiveDeltaTime
                + nonPositiveDistance + routeChanged + speedTooHigh;
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
}
