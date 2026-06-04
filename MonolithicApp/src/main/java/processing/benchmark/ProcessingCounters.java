package processing.benchmark;

public final class ProcessingCounters {
    private long groupedDatagrams;
    private long processedBuses;
    private long validIntervals;
    private long noRouteOrInactive;
    private long malformedDatagrams;
    private long discardNoPreviousPoint;
    private long discardRouteChanged;
    private long discardDeltaTimeInvalid;
    private long discardDeltaTimeTooLong;
    private long discardBadOdometer;
    private long discardNoDistanceGain;
    private long discardSpeedTooHigh;

    public void incrementGroupedDatagrams() {
        groupedDatagrams++;
    }

    public void incrementProcessedBuses() {
        processedBuses++;
    }

    public void incrementValidIntervals() {
        validIntervals++;
    }

    public void incrementNoRouteOrInactive() {
        noRouteOrInactive++;
    }

    public void incrementMalformedDatagrams() {
        malformedDatagrams++;
    }

    public void incrementDiscardNoPreviousPoint() {
        discardNoPreviousPoint++;
    }

    public void incrementDiscardRouteChanged() {
        discardRouteChanged++;
    }

    public void incrementDiscardDeltaTimeInvalid() {
        discardDeltaTimeInvalid++;
    }

    public void incrementDiscardDeltaTimeTooLong() {
        discardDeltaTimeTooLong++;
    }

    public void incrementDiscardBadOdometer() {
        discardBadOdometer++;
    }

    public void incrementDiscardNoDistanceGain() {
        discardNoDistanceGain++;
    }

    public void incrementDiscardSpeedTooHigh() {
        discardSpeedTooHigh++;
    }

    public void merge(ProcessingCounters other) {
        if (other == null) {
            return;
        }
        groupedDatagrams += other.groupedDatagrams;
        processedBuses += other.processedBuses;
        validIntervals += other.validIntervals;
        noRouteOrInactive += other.noRouteOrInactive;
        malformedDatagrams += other.malformedDatagrams;
        discardNoPreviousPoint += other.discardNoPreviousPoint;
        discardRouteChanged += other.discardRouteChanged;
        discardDeltaTimeInvalid += other.discardDeltaTimeInvalid;
        discardDeltaTimeTooLong += other.discardDeltaTimeTooLong;
        discardBadOdometer += other.discardBadOdometer;
        discardNoDistanceGain += other.discardNoDistanceGain;
        discardSpeedTooHigh += other.discardSpeedTooHigh;
    }

    public long getGroupedDatagrams() {
        return groupedDatagrams;
    }

    public long getProcessedBuses() {
        return processedBuses;
    }

    public long getValidIntervals() {
        return validIntervals;
    }

    public long getNoRouteOrInactive() {
        return noRouteOrInactive;
    }

    public long getMalformedDatagrams() {
        return malformedDatagrams;
    }

    public long getDiscardNoPreviousPoint() {
        return discardNoPreviousPoint;
    }

    public long getDiscardRouteChanged() {
        return discardRouteChanged;
    }

    public long getDiscardDeltaTimeInvalid() {
        return discardDeltaTimeInvalid;
    }

    public long getDiscardDeltaTimeTooLong() {
        return discardDeltaTimeTooLong;
    }

    public long getDiscardBadOdometer() {
        return discardBadOdometer;
    }

    public long getDiscardNoDistanceGain() {
        return discardNoDistanceGain;
    }

    public long getDiscardSpeedTooHigh() {
        return discardSpeedTooHigh;
    }
}
