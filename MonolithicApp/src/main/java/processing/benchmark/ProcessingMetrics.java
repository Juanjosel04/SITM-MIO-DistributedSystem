package processing.benchmark;

import java.time.Duration;
import java.util.Objects;

/**
 * Minimal immutable metrics snapshot for the V2 processing pipeline.
 */
public final class ProcessingMetrics {
    private final String datasetName;
    private final long loadedRoutes;
    private final long processedDatagrams;
    private final int activeRoutes;
    private final int parallelism;
    private final Duration processingTime;

    public ProcessingMetrics(
            String datasetName,
            long loadedRoutes,
            long processedDatagrams,
            int activeRoutes,
            int parallelism,
            Duration processingTime
    ) {
        this.datasetName = Objects.requireNonNull(datasetName, "datasetName");
        this.loadedRoutes = loadedRoutes;
        this.processedDatagrams = processedDatagrams;
        this.activeRoutes = activeRoutes;
        this.parallelism = parallelism;
        this.processingTime = Objects.requireNonNull(processingTime, "processingTime");
    }

    public static ProcessingMetrics empty() {
        return new ProcessingMetrics("Not loaded", 0L, 0L, 0, 0, Duration.ZERO);
    }

    public String getDatasetName() {
        return datasetName;
    }

    public long getLoadedRoutes() {
        return loadedRoutes;
    }

    public long getProcessedDatagrams() {
        return processedDatagrams;
    }

    public int getActiveRoutes() {
        return activeRoutes;
    }

    public int getParallelism() {
        return parallelism;
    }

    public Duration getProcessingTime() {
        return processingTime;
    }
}
