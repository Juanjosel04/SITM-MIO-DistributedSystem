package processing;

import domain.MonthlyRouteAverage;
import domain.Route;
import domain.RouteMonthKey;
import domain.Datagram;
import processing.aggregation.SpeedAccumulator;
import processing.benchmark.ProcessingMetrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AverageSpeedProcessingResult {
    private final List<MonthlyRouteAverage> averages;
    private final ProcessingMetrics metrics;
    private final Map<RouteMonthKey, SpeedAccumulator> accumulators;
    private final List<Route> routes;
    private final List<Datagram> visualDatagrams;

    public AverageSpeedProcessingResult(
            List<MonthlyRouteAverage> averages,
            ProcessingMetrics metrics,
            Map<RouteMonthKey, SpeedAccumulator> accumulators,
            List<Route> routes
    ) {
        this(averages, metrics, accumulators, routes, Collections.<Datagram>emptyList());
    }

    public AverageSpeedProcessingResult(
            List<MonthlyRouteAverage> averages,
            ProcessingMetrics metrics,
            Map<RouteMonthKey, SpeedAccumulator> accumulators,
            List<Route> routes,
            List<Datagram> visualDatagrams
    ) {
        this.averages = averages == null ? Collections.<MonthlyRouteAverage>emptyList() : Collections.unmodifiableList(averages);
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.accumulators = accumulators == null ? Collections.<RouteMonthKey, SpeedAccumulator>emptyMap() : Collections.unmodifiableMap(accumulators);
        this.routes = routes == null ? Collections.<Route>emptyList() : Collections.unmodifiableList(routes);
        this.visualDatagrams = visualDatagrams == null ? Collections.<Datagram>emptyList() : Collections.unmodifiableList(visualDatagrams);
    }

    public List<MonthlyRouteAverage> getAverages() {
        return averages;
    }

    public ProcessingMetrics getMetrics() {
        return metrics;
    }

    public Map<RouteMonthKey, SpeedAccumulator> getAccumulators() {
        return accumulators;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<Datagram> getVisualDatagrams() {
        return visualDatagrams;
    }
}
