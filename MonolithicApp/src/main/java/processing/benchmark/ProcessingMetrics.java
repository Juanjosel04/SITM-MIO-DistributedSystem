package processing.benchmark;

import java.time.Duration;
import java.util.Objects;

/**
 * Minimal immutable metrics snapshot for the V2 processing pipeline.
 */
public final class ProcessingMetrics {
    private final String datasetName;
    private final long loadedRoutes;
    private final long readDatagrams;
    private final long processedDatagrams;
    private final long routeMonthCombinations;
    private final long routesWithoutData;
    private final int parallelism;
    private final int threshold;
    private final Duration processingTime;

    public ProcessingMetrics(
            String datasetName,
            long loadedRoutes,
            long readDatagrams,
            long processedDatagrams,
            long routeMonthCombinations,
            long routesWithoutData,
            int parallelism,
            int threshold,
            Duration processingTime
    ) {
        this.datasetName = Objects.requireNonNull(datasetName, "datasetName");
        this.loadedRoutes = loadedRoutes;
        this.readDatagrams = readDatagrams;
        this.processedDatagrams = processedDatagrams;
        this.routeMonthCombinations = routeMonthCombinations;
        this.routesWithoutData = routesWithoutData;
        this.parallelism = parallelism;
        this.threshold = threshold;
        this.processingTime = Objects.requireNonNull(processingTime, "processingTime");
    }

    public static ProcessingMetrics empty() {
        return new ProcessingMetrics("Not loaded", 0L, 0L, 0L, 0L, 0L, 0, 0, Duration.ZERO);
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

    public long getProcessedDatagrams() {
        return processedDatagrams;
    }

    public int getActiveRoutes() {
        return (int) loadedRoutes;
    }

    public long getRouteMonthCombinations() {
        return routeMonthCombinations;
    }

    public long getRoutesWithoutData() {
        return routesWithoutData;
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getThreshold() {
        return threshold;
    }

    public Duration getProcessingTime() {
        return processingTime;
    }

    public long getProcessingTimeMillis() {
        return processingTime.toMillis();
    }
}
