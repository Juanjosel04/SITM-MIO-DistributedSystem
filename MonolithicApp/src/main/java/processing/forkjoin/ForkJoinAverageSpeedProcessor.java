package processing.forkjoin;

import domain.Datagram;
import domain.MonthlyRouteAverage;
import domain.Route;
import domain.RouteMonthKey;
import processing.AverageSpeedProcessingResult;
import processing.AverageSpeedProcessor;
import processing.aggregation.AverageSpeedAggregator;
import processing.aggregation.SpeedAccumulator;
import processing.benchmark.ProcessingMetrics;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public final class ForkJoinAverageSpeedProcessor implements AverageSpeedProcessor {
    public static final int DEFAULT_PARALLELISM = Math.max(1, Runtime.getRuntime().availableProcessors());
    public static final int DEFAULT_THRESHOLD = 1000;

    private final int parallelism;
    private final int threshold;
    private final String datasetName;
    private final AverageSpeedAggregator aggregator;

    public ForkJoinAverageSpeedProcessor() {
        this(DEFAULT_PARALLELISM, DEFAULT_THRESHOLD);
    }

    public ForkJoinAverageSpeedProcessor(int parallelism, int threshold) {
        this(parallelism, threshold, "Not specified");
    }

    public ForkJoinAverageSpeedProcessor(int parallelism, int threshold, String datasetName) {
        this.parallelism = Math.max(1, parallelism);
        this.threshold = Math.max(1, threshold);
        this.datasetName = datasetName == null || datasetName.trim().isEmpty() ? "Not specified" : datasetName.trim();
        this.aggregator = new AverageSpeedAggregator();
    }

    @Override
    public AverageSpeedProcessingResult process(List<Route> routes, List<Datagram> datagrams) {
        List<Route> safeRoutes = routes == null ? Collections.<Route>emptyList() : routes;
        List<Datagram> safeDatagrams = datagrams == null ? Collections.<Datagram>emptyList() : datagrams;

        ForkJoinPool pool = new ForkJoinPool(parallelism);
        long startNanos = System.nanoTime();
        Map<RouteMonthKey, SpeedAccumulator> accumulators;
        try {
            AverageSpeedTask task = new AverageSpeedTask(safeDatagrams, 0, safeDatagrams.size(), threshold);
            accumulators = pool.invoke(task);
        } finally {
            pool.shutdown();
        }
        List<MonthlyRouteAverage> averages = aggregator.toMonthlyAverages(safeRoutes, accumulators);
        Duration processingTime = Duration.ofNanos(System.nanoTime() - startNanos);

        ProcessingMetrics metrics = new ProcessingMetrics(
                datasetName,
                safeRoutes.size(),
                safeDatagrams.size(),
                countProcessedDatagrams(accumulators),
                averages.size(),
                countRoutesWithoutData(averages),
                parallelism,
                threshold,
                processingTime
        );

        return new AverageSpeedProcessingResult(averages, metrics, accumulators, safeRoutes);
    }

    private long countProcessedDatagrams(Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        long count = 0L;
        for (SpeedAccumulator accumulator : accumulators.values()) {
            count += accumulator.getCount();
        }
        return count;
    }

    private long countRoutesWithoutData(List<MonthlyRouteAverage> averages) {
        java.util.Map<Integer, Boolean> routeHasData = new java.util.HashMap<Integer, Boolean>();
        for (MonthlyRouteAverage average : averages) {
            Integer routeId = Integer.valueOf(average.getRouteId());
            Boolean current = routeHasData.get(routeId);
            routeHasData.put(routeId, Boolean.valueOf((current != null && current.booleanValue()) || average.hasData()));
        }

        long count = 0L;
        for (Boolean hasData : routeHasData.values()) {
            if (!hasData.booleanValue()) {
                count++;
            }
        }
        return count;
    }
}
