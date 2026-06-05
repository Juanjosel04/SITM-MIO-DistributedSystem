package processing.benchmark;

import java.time.Duration;
import java.util.Objects;

public final class ProcessingMetrics {
    private final String datasetName;
    private final long loadedRoutes;
    private final long readDatagrams;
    private final long groupedDatagrams;
    private final long processedBuses;
    private final long validIntervals;
    private final long routesWithResult;
    private final long routesWithoutData;
    private final long routeMonthCombinations;
    private final ProcessingCounters counters;
    private final int parallelism;
    private final int threshold;
    private final Duration forkJoinProcessingTime;
    private final Duration totalProcessingTime;
    private final double throughputDatagramsPerSecond;
    private final double throughputIntervalsPerSecond;
    private final double globalAverageSpeedKmh;
    private final int bucketCount;
    private final long bucketizationTimeMillis;
    private final long maxBucketApproxLines;
    private final String tempDirectoryUsed;

    public ProcessingMetrics(
            String datasetName,
            long loadedRoutes,
            long readDatagrams,
            long groupedDatagrams,
            long processedBuses,
            long validIntervals,
            long routesWithResult,
            long routesWithoutData,
            long routeMonthCombinations,
            ProcessingCounters counters,
            int parallelism,
            int threshold,
            Duration forkJoinProcessingTime,
            Duration totalProcessingTime,
            double throughputDatagramsPerSecond,
            double throughputIntervalsPerSecond,
            double globalAverageSpeedKmh
    ) {
        this(
                datasetName,
                loadedRoutes,
                readDatagrams,
                groupedDatagrams,
                processedBuses,
                validIntervals,
                routesWithResult,
                routesWithoutData,
                routeMonthCombinations,
                counters,
                parallelism,
                threshold,
                forkJoinProcessingTime,
                totalProcessingTime,
                throughputDatagramsPerSecond,
                throughputIntervalsPerSecond,
                globalAverageSpeedKmh,
                0,
                0L,
                0L,
                ""
        );
    }

    public ProcessingMetrics(
            String datasetName,
            long loadedRoutes,
            long readDatagrams,
            long groupedDatagrams,
            long processedBuses,
            long validIntervals,
            long routesWithResult,
            long routesWithoutData,
            long routeMonthCombinations,
            ProcessingCounters counters,
            int parallelism,
            int threshold,
            Duration forkJoinProcessingTime,
            Duration totalProcessingTime,
            double throughputDatagramsPerSecond,
            double throughputIntervalsPerSecond,
            double globalAverageSpeedKmh,
            int bucketCount,
            long bucketizationTimeMillis,
            long maxBucketApproxLines,
            String tempDirectoryUsed
    ) {
        this.datasetName = Objects.requireNonNull(datasetName, "datasetName");
        this.loadedRoutes = loadedRoutes;
        this.readDatagrams = readDatagrams;
        this.groupedDatagrams = groupedDatagrams;
        this.processedBuses = processedBuses;
        this.validIntervals = validIntervals;
        this.routesWithResult = routesWithResult;
        this.routesWithoutData = routesWithoutData;
        this.routeMonthCombinations = routeMonthCombinations;
        this.counters = counters == null ? new ProcessingCounters() : counters;
        this.parallelism = parallelism;
        this.threshold = threshold;
        this.forkJoinProcessingTime = Objects.requireNonNull(forkJoinProcessingTime, "forkJoinProcessingTime");
        this.totalProcessingTime = Objects.requireNonNull(totalProcessingTime, "totalProcessingTime");
        this.throughputDatagramsPerSecond = throughputDatagramsPerSecond;
        this.throughputIntervalsPerSecond = throughputIntervalsPerSecond;
        this.globalAverageSpeedKmh = globalAverageSpeedKmh;
        this.bucketCount = bucketCount;
        this.bucketizationTimeMillis = bucketizationTimeMillis;
        this.maxBucketApproxLines = maxBucketApproxLines;
        this.tempDirectoryUsed = tempDirectoryUsed == null ? "" : tempDirectoryUsed;
    }

    public static ProcessingMetrics empty() {
        return new ProcessingMetrics(
                "Not loaded",
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                new ProcessingCounters(),
                0,
                0,
                Duration.ZERO,
                Duration.ZERO,
                0.0,
                0.0,
                0.0,
                0,
                0L,
                0L,
                ""
        );
    }

    public String getDatasetName() {
        return datasetName;
    }

    public long getLoadedRoutes() {
        return loadedRoutes;
    }

    public long getReadDatagrams() {
        return readDatagrams;
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

    public long getProcessedDatagrams() {
        return validIntervals;
    }

    public int getActiveRoutes() {
        return (int) loadedRoutes;
    }

    public long getRoutesWithResult() {
        return routesWithResult;
    }

    public long getRoutesWithoutData() {
        return routesWithoutData;
    }

    public long getRouteMonthCombinations() {
        return routeMonthCombinations;
    }

    public ProcessingCounters getCounters() {
        return counters;
    }

    public long getNoRouteOrInactive() {
        return counters.getNoRouteOrInactive();
    }

    public long getMalformedDatagrams() {
        return counters.getMalformedDatagrams();
    }

    public long getInvalidLines() {
        return counters.getInvalidLines();
    }

    public long getDiscardNoPreviousPoint() {
        return counters.getDiscardNoPreviousPoint();
    }

    public long getDiscardRouteChanged() {
        return counters.getDiscardRouteChanged();
    }

    public long getDiscardDeltaTimeInvalid() {
        return counters.getDiscardDeltaTimeInvalid();
    }

    public long getDiscardDeltaTimeTooLong() {
        return counters.getDiscardDeltaTimeTooLong();
    }

    public long getDiscardBadOdometer() {
        return counters.getDiscardBadOdometer();
    }

    public long getDiscardNoDistanceGain() {
        return counters.getDiscardNoDistanceGain();
    }

    public long getDiscardSpeedTooHigh() {
        return counters.getDiscardSpeedTooHigh();
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getThreshold() {
        return threshold;
    }

    public Duration getProcessingTime() {
        return totalProcessingTime;
    }

    public long getProcessingTimeMillis() {
        return totalProcessingTime.toMillis();
    }

    public long getForkJoinProcessingTimeMillis() {
        return forkJoinProcessingTime.toMillis();
    }

    public long getTotalProcessingTimeMillis() {
        return totalProcessingTime.toMillis();
    }

    public double getThroughputDatagramsPerSecond() {
        return throughputDatagramsPerSecond;
    }

    public double getThroughputIntervalsPerSecond() {
        return throughputIntervalsPerSecond;
    }

    public double getGlobalAverageSpeedKmh() {
        return globalAverageSpeedKmh;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public long getBucketizationTimeMillis() {
        return bucketizationTimeMillis;
    }

    public long getMaxBucketApproxLines() {
        return maxBucketApproxLines;
    }

    public String getTempDirectoryUsed() {
        return tempDirectoryUsed;
    }
}
