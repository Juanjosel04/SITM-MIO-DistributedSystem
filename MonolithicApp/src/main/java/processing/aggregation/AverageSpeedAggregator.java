package processing.aggregation;

import domain.MonthlyRouteAverage;
import domain.Route;
import domain.RouteMonthKey;
import visualization.RouteDisplayService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AverageSpeedAggregator {
    public Map<RouteMonthKey, SpeedAccumulator> merge(
            Map<RouteMonthKey, SpeedAccumulator> first,
            Map<RouteMonthKey, SpeedAccumulator> second
    ) {
        Map<RouteMonthKey, SpeedAccumulator> merged = new HashMap<RouteMonthKey, SpeedAccumulator>();
        mergeInto(merged, first);
        mergeInto(merged, second);
        return merged;
    }

    public void mergeInto(Map<RouteMonthKey, SpeedAccumulator> target, Map<RouteMonthKey, SpeedAccumulator> source) {
        if (target == null || source == null) {
            return;
        }
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : source.entrySet()) {
            SpeedAccumulator accumulator = target.get(entry.getKey());
            if (accumulator == null) {
                accumulator = new SpeedAccumulator();
                target.put(entry.getKey(), accumulator);
            }
            accumulator.merge(entry.getValue());
        }
    }

    public List<MonthlyRouteAverage> toMonthlyAverages(List<Route> routes, Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        List<Route> safeRoutes = routes == null ? Collections.<Route>emptyList() : routes;
        Map<RouteMonthKey, SpeedAccumulator> safeAccumulators =
                accumulators == null ? Collections.<RouteMonthKey, SpeedAccumulator>emptyMap() : accumulators;
        RouteDisplayService routeDisplayService = new RouteDisplayService(safeRoutes);
        Set<YearMonth> months = observedMonths(safeAccumulators);
        List<MonthlyRouteAverage> averages = new ArrayList<MonthlyRouteAverage>();

        for (Route route : safeRoutes) {
            for (YearMonth month : months) {
                RouteMonthKey key = new RouteMonthKey(route.getId(), month.year, month.month);
                SpeedAccumulator accumulator = safeAccumulators.get(key);
                String label = routeDisplayService.labelFor(route.getId());
                if (accumulator == null || !accumulator.hasData()) {
                    averages.add(MonthlyRouteAverage.noData(route.getId(), label, month.year, month.month));
                } else {
                    averages.add(new MonthlyRouteAverage(
                            route.getId(),
                            label,
                            month.year,
                            month.month,
                            accumulator.getAverageKmh(),
                            accumulator.getIntervals()
                    ));
                }
            }
        }

        Collections.sort(averages, new Comparator<MonthlyRouteAverage>() {
            @Override
            public int compare(MonthlyRouteAverage first, MonthlyRouteAverage second) {
                int labelComparison = first.getRouteLabel().compareToIgnoreCase(second.getRouteLabel());
                if (labelComparison != 0) {
                    return labelComparison;
                }
                int routeComparison = Integer.compare(first.getRouteId(), second.getRouteId());
                if (routeComparison != 0) {
                    return routeComparison;
                }
                int yearComparison = Integer.compare(first.getYear(), second.getYear());
                if (yearComparison != 0) {
                    return yearComparison;
                }
                return Integer.compare(first.getMonth(), second.getMonth());
            }
        });

        return averages;
    }

    private Set<YearMonth> observedMonths(Map<RouteMonthKey, SpeedAccumulator> accumulators) {
        Set<YearMonth> months = new LinkedHashSet<YearMonth>();
        List<RouteMonthKey> keys = new ArrayList<RouteMonthKey>(accumulators.keySet());
        Collections.sort(keys);
        for (RouteMonthKey key : keys) {
            months.add(new YearMonth(key.getYear(), key.getMonth()));
        }
        return months;
    }

    private static final class YearMonth {
        private final int year;
        private final int month;

        private YearMonth(int year, int month) {
            this.year = year;
            this.month = month;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof YearMonth)) {
                return false;
            }
            YearMonth other = (YearMonth) object;
            return year == other.year && month == other.month;
        }

        @Override
        public int hashCode() {
            int result = Integer.valueOf(year).hashCode();
            result = 31 * result + Integer.valueOf(month).hashCode();
            return result;
        }
    }
}
