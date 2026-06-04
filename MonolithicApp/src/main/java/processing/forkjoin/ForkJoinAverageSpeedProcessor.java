package processing.forkjoin;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public final class ForkJoinAverageSpeedProcessor implements AverageSpeedProcessor {
    public static final int DEFAULT_PARALLELISM = Math.max(1, Runtime.getRuntime().availableProcessors());
    public static final int DEFAULT_THRESHOLD = 64;

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
        Set<Integer> activeRouteIds = activeRouteIds(safeRoutes);
        List<BusDatagramGroup> groups = groupByBus(safeDatagrams);

        ForkJoinPool pool = new ForkJoinPool(parallelism);
        long forkJoinStartNanos = System.nanoTime();
        PartialSpeedResult partialResult;
        try {
            AverageSpeedTask task = new AverageSpeedTask(groups, activeRouteIds, 0, groups.size(), threshold);
            partialResult = pool.invoke(task);
        } finally {
            pool.shutdown();
        }
        Duration forkJoinProcessingTime = Duration.ofNanos(System.nanoTime() - forkJoinStartNanos);

        long aggregationStartNanos = System.nanoTime();
        Map<RouteMonthKey, SpeedAccumulator> accumulators = partialResult.getAccumulators();
        List<MonthlyRouteAverage> averages = aggregator.toMonthlyAverages(safeRoutes, accumulators);
        Duration aggregationTime = Duration.ofNanos(System.nanoTime() - aggregationStartNanos);
        Duration totalProcessingTime = forkJoinProcessingTime.plus(aggregationTime);

        ProcessingCounters counters = partialResult.getCounters();
        long routesWithResult = countRoutesWithResult(accumulators);
        double globalAverageSpeedKmh = calculateGlobalAverageSpeedKmh(accumulators);
        ProcessingMetrics metrics = new ProcessingMetrics(
                datasetName,
                safeRoutes.size(),
                safeDatagrams.size(),
                counters.getGroupedDatagrams(),
                counters.getProcessedBuses(),
                counters.getValidIntervals(),
                routesWithResult,
                Math.max(0L, safeRoutes.size() - routesWithResult),
                averages.size(),
                counters,
                parallelism,
                threshold,
                forkJoinProcessingTime,
                totalProcessingTime,
                throughput(safeDatagrams.size(), totalProcessingTime),
                throughput(counters.getValidIntervals(), forkJoinProcessingTime),
                globalAverageSpeedKmh
        );

        return new AverageSpeedProcessingResult(averages, metrics, accumulators, safeRoutes);
    }

    private Set<Integer> activeRouteIds(List<Route> routes) {
        Set<Integer> ids = new HashSet<Integer>();
        for (Route route : routes) {
            ids.add(Integer.valueOf(route.getId()));
        }
        return ids;
    }

    private List<BusDatagramGroup> groupByBus(List<Datagram> datagrams) {
        Map<String, List<Datagram>> byBus = new HashMap<String, List<Datagram>>();
        for (Datagram datagram : datagrams) {
            if (datagram == null || datagram.getBusId() == null || datagram.getBusId().trim().isEmpty()) {
                continue;
            }
            String busId = datagram.getBusId().trim();
            List<Datagram> group = byBus.get(busId);
            if (group == null) {
                group = new ArrayList<Datagram>();
                byBus.put(busId, group);
            }
            group.add(datagram);
        }

        List<String> busIds = new ArrayList<String>(byBus.keySet());
        Collections.sort(busIds);
        List<BusDatagramGroup> groups = new ArrayList<BusDatagramGroup>();
        for (String busId : busIds) {
            List<Datagram> group = byBus.get(busId);
            Collections.sort(group, new Comparator<Datagram>() {
                @Override
                public int compare(Datagram first, Datagram second) {
                    return first.getTimestamp().compareTo(second.getTimestamp());
                }
            });
            groups.add(new BusDatagramGroup(busId, group));
        }
        return groups;
    }

    private long countRoutesWithResult(Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        Set<Integer> routes = new HashSet<Integer>();
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : accumulators.entrySet()) {
            if (entry.getValue().hasData()) {
                routes.add(Integer.valueOf(entry.getKey().getRouteId()));
            }
        }
        return routes.size();
    }

    private double calculateGlobalAverageSpeedKmh(Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        double distanceMeters = 0.0;
        double timeSeconds = 0.0;
        for (SpeedAccumulator accumulator : accumulators.values()) {
            distanceMeters += accumulator.getSumDistanceMeters();
            timeSeconds += accumulator.getSumTimeSeconds();
        }
        if (timeSeconds <= 0.0) {
            return 0.0;
        }
        return (distanceMeters / timeSeconds) * 3.6;
    }

    private double throughput(long items, Duration duration) {
        double seconds = duration.toNanos() / 1_000_000_000.0;
        if (items <= 0L || seconds <= 0.0) {
            return 0.0;
        }
        return items / seconds;
    }
}
