package processing.streaming;

import domain.Datagram;
import domain.MonthlyRouteAverage;
import domain.Route;
import domain.RouteMonthKey;
import processing.AverageSpeedProcessingResult;
import processing.AverageSpeedProcessor;
import processing.aggregation.AverageSpeedAggregator;
import processing.aggregation.SpeedAccumulator;
import processing.benchmark.ProcessingCounters;
import processing.benchmark.ProcessingMetrics;
import processing.forkjoin.BucketAverageSpeedTask;
import processing.forkjoin.PartialSpeedResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public final class StreamingBucketedAverageSpeedProcessor implements AverageSpeedProcessor {
    public static final int DEFAULT_PARALLELISM = Math.max(1, Runtime.getRuntime().availableProcessors());

    private final String datasetPath;
    private final BucketProcessingConfig config;
    private final StreamingDatagramBucketizer bucketizer;
    private final AverageSpeedAggregator aggregator;

    public StreamingBucketedAverageSpeedProcessor(String datasetPath) {
        this(datasetPath, BucketProcessingConfig.defaults(DEFAULT_PARALLELISM));
    }

    public StreamingBucketedAverageSpeedProcessor(String datasetPath, BucketProcessingConfig config) {
        this.datasetPath = datasetPath;
        this.config = config;
        this.bucketizer = new StreamingDatagramBucketizer();
        this.aggregator = new AverageSpeedAggregator();
    }

    @Override
    public AverageSpeedProcessingResult process(List<Route> routes, List<Datagram> datagrams) {
        List<Route> safeRoutes = routes == null ? Collections.<Route>emptyList() : routes;
        long totalStartNanos = System.nanoTime();
        BucketizationResult bucketization = null;
        try {
            bucketization = bucketizer.bucketize(datasetPath, config);
            ForkJoinPool pool = new ForkJoinPool(config.getParallelism());
            long forkJoinStartNanos = System.nanoTime();
            PartialSpeedResult partial;
            try {
                BucketAverageSpeedTask rootTask = new BucketAverageSpeedTask(
                        bucketization.getBucketFiles(),
                        activeRouteIds(safeRoutes),
                        0,
                        bucketization.getBucketFiles().size(),
                        config.getBucketTaskThreshold()
                );
                partial = pool.invoke(rootTask);
            } finally {
                pool.shutdown();
            }
            Duration forkJoinTime = Duration.ofNanos(System.nanoTime() - forkJoinStartNanos);

            Map<RouteMonthKey, SpeedAccumulator> accumulators = partial.getAccumulators();
            List<MonthlyRouteAverage> averages = aggregator.toMonthlyAverages(safeRoutes, accumulators);
            Duration totalTime = Duration.ofNanos(System.nanoTime() - totalStartNanos);
            ProcessingCounters counters = partial.getCounters();
            counters.addInvalidLines(bucketization.getInvalidLines());

            ProcessingMetrics metrics = new ProcessingMetrics(
                    datasetPath,
                    safeRoutes.size(),
                    bucketization.getReadDatagrams(),
                    bucketization.getBucketizedDatagrams(),
                    counters.getProcessedBuses(),
                    counters.getValidIntervals(),
                    countRoutesWithResult(accumulators),
                    Math.max(0L, safeRoutes.size() - countRoutesWithResult(accumulators)),
                    averages.size(),
                    counters,
                    config.getParallelism(),
                    config.getBucketTaskThreshold(),
                    forkJoinTime,
                    totalTime,
                    throughput(bucketization.getReadDatagrams(), totalTime),
                    throughput(counters.getValidIntervals(), forkJoinTime),
                    globalAverageSpeedKmh(accumulators),
                    config.getBucketCount(),
                    bucketization.getBucketizationTimeMillis(),
                    bucketization.getMaxBucketApproxLines(),
                    bucketization.getTempDirectory() == null ? "" : bucketization.getTempDirectory().toString()
            );

            return new AverageSpeedProcessingResult(
                    averages,
                    metrics,
                    accumulators,
                    safeRoutes,
                    bucketization.getVisualSample()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to process streaming bucketed dataset: " + datasetPath, exception);
        } finally {
            if (bucketization != null) {
                cleanup(bucketization);
            }
        }
    }

    private Set<Integer> activeRouteIds(List<Route> routes) {
        Set<Integer> ids = new HashSet<Integer>();
        for (Route route : routes) {
            ids.add(Integer.valueOf(route.getId()));
        }
        return ids;
    }

    private long countRoutesWithResult(Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        Set<Integer> routeIds = new HashSet<Integer>();
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : accumulators.entrySet()) {
            if (entry.getValue().hasData()) {
                routeIds.add(Integer.valueOf(entry.getKey().getRouteId()));
            }
        }
        return routeIds.size();
    }

    private double globalAverageSpeedKmh(Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        double distance = 0.0;
        double time = 0.0;
        for (SpeedAccumulator accumulator : accumulators.values()) {
            distance += accumulator.getSumDistanceMeters();
            time += accumulator.getSumTimeSeconds();
        }
        if (time <= 0.0) {
            return 0.0;
        }
        return (distance / time) * 3.6;
    }

    private double throughput(long items, Duration duration) {
        double seconds = duration.toNanos() / 1_000_000_000.0;
        if (items <= 0L || seconds <= 0.0) {
            return 0.0;
        }
        return items / seconds;
    }

    private void cleanup(BucketizationResult result) {
        for (Path bucketFile : result.getBucketFiles()) {
            try {
                Files.deleteIfExists(bucketFile);
            } catch (IOException exception) {
                System.err.println("Warning: could not delete temp bucket file " + bucketFile);
            }
        }
        Path tempDirectory = result.getTempDirectory();
        if (tempDirectory != null) {
            try {
                Files.deleteIfExists(tempDirectory);
            } catch (IOException exception) {
                System.err.println("Warning: could not delete temp bucket directory " + tempDirectory);
            }
        }
    }
}
