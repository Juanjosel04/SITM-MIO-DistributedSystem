package processing;

import processing.benchmark.ProcessingMetrics;

/**
 * Contract for future average-speed processors.
 */
public interface AverageSpeedProcessor {
    ProcessingMetrics process();
}
