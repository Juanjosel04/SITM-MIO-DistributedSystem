package analytics.service;

import analytics.model.ActiveBusesByRouteStatistic;
import analytics.model.AnalyticsFilter;
import analytics.model.AnalyticsSelectionSummary;
import analytics.model.AlertPriorityStatistic;
import analytics.model.EventRouteStatistic;
import analytics.model.RouteSpeedAnalyticsRow;
import analytics.model.RouteSpeedStatistic;
import analytics.model.SystemAnalyticsSnapshot;
import analytics.model.ThroughputStatistic;
import core.model.BusPosition;
import core.model.PipelineSummary;
import core.utils.AppLogger;
import events.model.OperationalEvent;
import monitoring.controller.MonitoringController;
import monitoring.model.AlertPanelModel;
import monitoring.model.BusMarker;
import security.model.AccessScope;
import shared.enums.EventPriority;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsService {
    private static final double MAX_REASONABLE_SPEED = 120.0;

    private final MonitoringController monitoringController;

    public AnalyticsService(MonitoringController monitoringController) {
        this.monitoringController = monitoringController;
        AppLogger.info("Analytics service initialized.");
    }

    public SystemAnalyticsSnapshot refreshSnapshot() {
        return refreshSnapshot(null);
    }

    public SystemAnalyticsSnapshot refreshSnapshot(AccessScope scope) {
        List<BusMarker> buses = filteredBuses(scope);
        List<BusPosition> history = validHistoricalPositions(scope);
        List<OperationalEvent> events = filteredEvents(scope);
        List<AlertPanelModel> alerts = filteredAlerts(scope);

        List<RouteSpeedStatistic> routeSpeedStatistics = calculateRouteSpeedStatistics(history);
        List<EventRouteStatistic> eventRouteStatistics = calculateEventRouteStatistics(events);
        List<AlertPriorityStatistic> alertPriorityStatistics = calculateAlertPriorityStatistics(alerts);
        List<ActiveBusesByRouteStatistic> activeBusesByRouteStatistics = calculateActiveBusesByRoute(buses);
        ThroughputStatistic throughput = calculateThroughput();

        double globalAverageSpeed = calculateGlobalAverageSpeed(routeSpeedStatistics);
        String routeWithMostEvents = routeWithMostEvents(eventRouteStatistics);
        String routeWithHighestAverageSpeed = routeWithHighestAverageSpeed(routeSpeedStatistics);
        String routeWithMostActiveBuses = routeWithMostActiveBuses(activeBusesByRouteStatistics);

        SystemAnalyticsSnapshot snapshot = new SystemAnalyticsSnapshot(
                totalRoutes(scope),
                buses.size(),
                history.size(),
                events.size(),
                alerts.size(),
                globalAverageSpeed,
                routeWithMostEvents,
                routeWithHighestAverageSpeed,
                routeWithMostActiveBuses,
                throughput,
                routeSpeedStatistics,
                eventRouteStatistics,
                alertPriorityStatistics,
                activeBusesByRouteStatistics,
                LocalDateTime.now());
        AppLogger.info("Analytics snapshot refreshed.");
        return snapshot;
    }

    public List<Integer> getAvailableRoutes() {
        return getAvailableRoutes(null);
    }

    public List<Integer> getAvailableRoutes(AccessScope scope) {
        if (isRestrictedScope(scope)) {
            return allowedRoutes(scope);
        }
        List<Integer> routes = new ArrayList<Integer>();
        for (BusPosition position : validHistoricalPositions()) {
            Integer route = Integer.valueOf(position.getRouteId());
            if (!routes.contains(route)) {
                routes.add(route);
            }
        }
        Collections.sort(routes);
        return routes;
    }

    public List<Integer> getAvailableYears() {
        return getAvailableYears(null);
    }

    public List<Integer> getAvailableYears(AccessScope scope) {
        List<Integer> years = new ArrayList<Integer>();
        for (BusPosition position : validHistoricalPositions(scope)) {
            if (position.getTimestamp() == null) {
                continue;
            }
            Integer year = Integer.valueOf(position.getTimestamp().getYear());
            if (!years.contains(year)) {
                years.add(year);
            }
        }
        Collections.sort(years);
        return years;
    }

    public List<Integer> getAvailableMonthsForYear(int year) {
        return getAvailableMonthsForYear(year, null);
    }

    public List<Integer> getAvailableMonthsForYear(int year, AccessScope scope) {
        List<Integer> months = new ArrayList<Integer>();
        for (BusPosition position : validHistoricalPositions(scope)) {
            if (position.getTimestamp() == null || position.getTimestamp().getYear() != year) {
                continue;
            }
            Integer month = Integer.valueOf(position.getTimestamp().getMonthValue());
            if (!months.contains(month)) {
                months.add(month);
            }
        }
        Collections.sort(months);
        return months;
    }

    public List<RouteSpeedAnalyticsRow> getFilteredRouteSpeedAnalytics(AnalyticsFilter filter) {
        return getFilteredRouteSpeedAnalytics(filter, null);
    }

    public List<RouteSpeedAnalyticsRow> getFilteredRouteSpeedAnalytics(AnalyticsFilter filter, AccessScope scope) {
        AnalyticsFilter safeFilter = filter == null ? new AnalyticsFilter(null, null, null) : filter;
        List<Integer> routeIds = selectedRoutes(safeFilter, scope);
        List<RouteSpeedAnalyticsRow> rows = new ArrayList<RouteSpeedAnalyticsRow>();
        for (Integer routeId : routeIds) {
            if (safeFilter.getYear() == null) {
                int count = countPositions(routeId, null, null, scope);
                rows.add(new RouteSpeedAnalyticsRow(routeId.intValue(), "All years", "All months",
                        null, null, count));
            } else if (safeFilter.getMonth() == null) {
                AverageResult yearAverage = averageFor(routeId, safeFilter.getYear(), null, scope);
                rows.add(new RouteSpeedAnalyticsRow(routeId.intValue(), String.valueOf(safeFilter.getYear()),
                        "All months", null, yearAverage.getAverageOrNull(), yearAverage.getSamples()));
            } else {
                AverageResult monthAverage = averageFor(routeId, safeFilter.getYear(), safeFilter.getMonth(), scope);
                AverageResult yearAverage = averageFor(routeId, safeFilter.getYear(), null, scope);
                rows.add(new RouteSpeedAnalyticsRow(routeId.intValue(), String.valueOf(safeFilter.getYear()),
                        monthName(safeFilter.getMonth().intValue()), monthAverage.getAverageOrNull(),
                        yearAverage.getAverageOrNull(), monthAverage.getSamples()));
            }
        }
        AppLogger.info("Historical speed analytics calculated.");
        return rows;
    }

    public AnalyticsSelectionSummary getSelectionSummary(AnalyticsFilter filter) {
        return getSelectionSummary(filter, null);
    }

    public AnalyticsSelectionSummary getSelectionSummary(AnalyticsFilter filter, AccessScope scope) {
        AnalyticsFilter safeFilter = filter == null ? new AnalyticsFilter(null, null, null) : filter;
        if (safeFilter.getRouteId() != null && !canUseRoute(scope, safeFilter.getRouteId())) {
            return new AnalyticsSelectionSummary(null, null, 0);
        }
        AverageResult monthlyAverage = safeFilter.getYear() == null || safeFilter.getMonth() == null
                ? AverageResult.empty()
                : averageFor(safeFilter.getRouteId(), safeFilter.getYear(), safeFilter.getMonth(), scope);
        AverageResult yearlyAverage = safeFilter.getYear() == null
                ? AverageResult.empty()
                : averageFor(safeFilter.getRouteId(), safeFilter.getYear(), null, scope);
        int processedDatagrams;
        if (safeFilter.getYear() != null && safeFilter.getMonth() != null) {
            processedDatagrams = monthlyAverage.getSamples();
        } else if (safeFilter.getYear() != null) {
            processedDatagrams = yearlyAverage.getSamples();
        } else {
            processedDatagrams = countPositions(safeFilter.getRouteId(), null, null, scope);
        }
        return new AnalyticsSelectionSummary(monthlyAverage.getAverageOrNull(),
                yearlyAverage.getAverageOrNull(), processedDatagrams);
    }

    private List<RouteSpeedStatistic> calculateRouteSpeedStatistics(List<BusPosition> positions) {
        Map<Integer, SpeedAccumulator> accumulators = new LinkedHashMap<Integer, SpeedAccumulator>();
        for (BusPosition position : positions) {
            if (position == null || position.getTimestamp() == null || !isValidSpeed(position.getSpeed())) {
                continue;
            }
            SpeedAccumulator accumulator = accumulators.get(Integer.valueOf(position.getRouteId()));
            if (accumulator == null) {
                accumulator = new SpeedAccumulator(position.getRouteId());
                accumulators.put(Integer.valueOf(position.getRouteId()), accumulator);
            }
            accumulator.add(position);
        }

        List<RouteSpeedStatistic> statistics = new ArrayList<RouteSpeedStatistic>();
        for (SpeedAccumulator accumulator : accumulators.values()) {
            statistics.add(accumulator.toStatistic());
        }
        sortByRoute(statistics);
        return statistics;
    }

    private List<EventRouteStatistic> calculateEventRouteStatistics(List<OperationalEvent> events) {
        Map<Integer, EventAccumulator> accumulators = new LinkedHashMap<Integer, EventAccumulator>();
        for (OperationalEvent event : events) {
            if (event == null) {
                continue;
            }
            EventAccumulator accumulator = accumulators.get(Integer.valueOf(event.getRouteId()));
            if (accumulator == null) {
                accumulator = new EventAccumulator(event.getRouteId());
                accumulators.put(Integer.valueOf(event.getRouteId()), accumulator);
            }
            accumulator.add(event);
        }

        List<EventRouteStatistic> statistics = new ArrayList<EventRouteStatistic>();
        for (EventAccumulator accumulator : accumulators.values()) {
            statistics.add(accumulator.toStatistic());
        }
        sortEvents(statistics);
        return statistics;
    }

    private List<AlertPriorityStatistic> calculateAlertPriorityStatistics(List<AlertPanelModel> alerts) {
        Map<String, AlertAccumulator> accumulators = new LinkedHashMap<String, AlertAccumulator>();
        for (AlertPanelModel alert : alerts) {
            if (alert == null) {
                continue;
            }
            String priority = alert.getLevel() == null ? "UNKNOWN" : alert.getLevel().name();
            AlertAccumulator accumulator = accumulators.get(priority);
            if (accumulator == null) {
                accumulator = new AlertAccumulator(priority);
                accumulators.put(priority, accumulator);
            }
            accumulator.add(alert);
        }

        List<AlertPriorityStatistic> statistics = new ArrayList<AlertPriorityStatistic>();
        for (AlertAccumulator accumulator : accumulators.values()) {
            statistics.add(accumulator.toStatistic());
        }
        Collections.sort(statistics, new Comparator<AlertPriorityStatistic>() {
            @Override
            public int compare(AlertPriorityStatistic first, AlertPriorityStatistic second) {
                return Integer.compare(second.getCount(), first.getCount());
            }
        });
        return statistics;
    }

    private List<ActiveBusesByRouteStatistic> calculateActiveBusesByRoute(List<BusMarker> buses) {
        Map<Integer, BusRouteAccumulator> accumulators = new LinkedHashMap<Integer, BusRouteAccumulator>();
        for (BusMarker bus : buses) {
            if (bus == null) {
                continue;
            }
            BusRouteAccumulator accumulator = accumulators.get(Integer.valueOf(bus.getRouteId()));
            if (accumulator == null) {
                accumulator = new BusRouteAccumulator(bus.getRouteId());
                accumulators.put(Integer.valueOf(bus.getRouteId()), accumulator);
            }
            accumulator.add(bus);
        }

        List<ActiveBusesByRouteStatistic> statistics = new ArrayList<ActiveBusesByRouteStatistic>();
        for (BusRouteAccumulator accumulator : accumulators.values()) {
            statistics.add(accumulator.toStatistic());
        }
        Collections.sort(statistics, new Comparator<ActiveBusesByRouteStatistic>() {
            @Override
            public int compare(ActiveBusesByRouteStatistic first, ActiveBusesByRouteStatistic second) {
                return Integer.compare(second.getActiveBuses(), first.getActiveBuses());
            }
        });
        return statistics;
    }

    private ThroughputStatistic calculateThroughput() {
        PipelineSummary summary = monitoringController.getLastPipelineSummary();
        if (summary != null) {
            return new ThroughputStatistic(summary.getDatagramsRead(), summary.getDatagramsValid(),
                    summary.getDatagramsInvalid(), summary.getDatagramsProcessed(), summary.getErrors(),
                    summary.getElapsedMillis());
        }
        int processed = monitoringController.getDatagramsProcessed();
        int invalid = monitoringController.getDatagramsInvalid();
        return new ThroughputStatistic(processed + invalid, processed, invalid, processed, 0, 0L);
    }

    private boolean isValidSpeed(Double speed) {
        if (speed == null) {
            return false;
        }
        double value = speed.doubleValue();
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0 && value <= MAX_REASONABLE_SPEED;
    }

    private List<BusPosition> validHistoricalPositions() {
        return validHistoricalPositions(null);
    }

    private List<BusPosition> validHistoricalPositions(AccessScope scope) {
        List<BusPosition> validPositions = new ArrayList<BusPosition>();
        for (BusPosition position : monitoringController.getPositionHistory()) {
            if (position != null && position.getTimestamp() != null && isValidSpeed(position.getSpeed()) &&
                    canUseRoute(scope, Integer.valueOf(position.getRouteId()))) {
                validPositions.add(position);
            }
        }
        return validPositions;
    }

    private List<Integer> selectedRoutes(AnalyticsFilter filter) {
        return selectedRoutes(filter, null);
    }

    private List<Integer> selectedRoutes(AnalyticsFilter filter, AccessScope scope) {
        if (filter.getRouteId() != null) {
            List<Integer> singleRoute = new ArrayList<Integer>();
            if (canUseRoute(scope, filter.getRouteId())) {
                singleRoute.add(filter.getRouteId());
            }
            return singleRoute;
        }
        if (isRestrictedScope(scope)) {
            return allowedRoutes(scope);
        }
        List<Integer> routes = new ArrayList<Integer>();
        for (BusPosition position : validHistoricalPositions(scope)) {
            if (!matchesDate(position, filter.getYear(), filter.getMonth())) {
                continue;
            }
            Integer route = Integer.valueOf(position.getRouteId());
            if (!routes.contains(route)) {
                routes.add(route);
            }
        }
        Collections.sort(routes);
        return routes;
    }

    private AverageResult averageFor(Integer routeId, Integer year, Integer month) {
        return averageFor(routeId, year, month, null);
    }

    private AverageResult averageFor(Integer routeId, Integer year, Integer month, AccessScope scope) {
        double total = 0.0;
        int samples = 0;
        for (BusPosition position : validHistoricalPositions(scope)) {
            if (routeId != null && position.getRouteId() != routeId.intValue()) {
                continue;
            }
            if (!matchesDate(position, year, month)) {
                continue;
            }
            total += position.getSpeed().doubleValue();
            samples++;
        }
        return new AverageResult(samples == 0 ? 0.0 : total / samples, samples);
    }

    private int countPositions(Integer routeId, Integer year, Integer month) {
        return countPositions(routeId, year, month, null);
    }

    private int countPositions(Integer routeId, Integer year, Integer month, AccessScope scope) {
        int count = 0;
        for (BusPosition position : validHistoricalPositions(scope)) {
            if (routeId != null && position.getRouteId() != routeId.intValue()) {
                continue;
            }
            if (matchesDate(position, year, month)) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesDate(BusPosition position, Integer year, Integer month) {
        if (year != null && position.getTimestamp().getYear() != year.intValue()) {
            return false;
        }
        return month == null || position.getTimestamp().getMonthValue() == month.intValue();
    }

    private List<BusMarker> filteredBuses(AccessScope scope) {
        if (scope == null || scope.canViewAllRoutes()) {
            return monitoringController.getCurrentBuses();
        }
        return monitoringController.getCurrentBuses(scope);
    }

    private List<OperationalEvent> filteredEvents(AccessScope scope) {
        if (scope == null || scope.canViewAllRoutes()) {
            return monitoringController.getRecentEvents();
        }
        return monitoringController.getRecentEvents(scope);
    }

    private List<AlertPanelModel> filteredAlerts(AccessScope scope) {
        if (scope == null || scope.canViewAllRoutes()) {
            return monitoringController.getAlerts();
        }
        return Collections.emptyList();
    }

    private int totalRoutes(AccessScope scope) {
        return isRestrictedScope(scope) ? scope.getAllowedRouteIds().size() : monitoringController.getRoutesLoaded();
    }

    private boolean canUseRoute(AccessScope scope, Integer routeId) {
        if (routeId == null) {
            return false;
        }
        if (scope == null || scope.canViewAllRoutes()) {
            return true;
        }
        return scope.canViewRoute(routeId);
    }

    private boolean isRestrictedScope(AccessScope scope) {
        return scope != null && !scope.canViewAllRoutes();
    }

    private List<Integer> allowedRoutes(AccessScope scope) {
        List<Integer> routeIds = new ArrayList<Integer>();
        if (scope != null) {
            routeIds.addAll(scope.getAllowedRouteIds());
        }
        Collections.sort(routeIds);
        return routeIds;
    }

    private String monthName(int month) {
        String value = Month.of(month).name().toLowerCase();
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private double calculateGlobalAverageSpeed(List<RouteSpeedStatistic> routeSpeedStatistics) {
        double weightedSum = 0.0;
        int samples = 0;
        for (RouteSpeedStatistic statistic : routeSpeedStatistics) {
            weightedSum += statistic.getAverageSpeed() * statistic.getSamples();
            samples += statistic.getSamples();
        }
        return samples == 0 ? 0.0 : weightedSum / samples;
    }

    private String routeWithMostEvents(List<EventRouteStatistic> statistics) {
        if (statistics.isEmpty()) {
            return "No data";
        }
        return monitoringController.getRouteDisplayName(statistics.get(0).getRouteId()) +
                " (" + statistics.get(0).getTotalEvents() + ")";
    }

    private String routeWithHighestAverageSpeed(List<RouteSpeedStatistic> statistics) {
        if (statistics.isEmpty()) {
            return "No data";
        }
        RouteSpeedStatistic selected = statistics.get(0);
        for (RouteSpeedStatistic statistic : statistics) {
            if (statistic.getAverageSpeed() > selected.getAverageSpeed()) {
                selected = statistic;
            }
        }
        return monitoringController.getRouteDisplayName(selected.getRouteId());
    }

    private String routeWithMostActiveBuses(List<ActiveBusesByRouteStatistic> statistics) {
        if (statistics.isEmpty()) {
            return "No data";
        }
        return monitoringController.getRouteDisplayName(statistics.get(0).getRouteId()) +
                " (" + statistics.get(0).getActiveBuses() + ")";
    }

    private void sortByRoute(List<RouteSpeedStatistic> statistics) {
        Collections.sort(statistics, new Comparator<RouteSpeedStatistic>() {
            @Override
            public int compare(RouteSpeedStatistic first, RouteSpeedStatistic second) {
                return Integer.compare(first.getRouteId(), second.getRouteId());
            }
        });
    }

    private void sortEvents(List<EventRouteStatistic> statistics) {
        Collections.sort(statistics, new Comparator<EventRouteStatistic>() {
            @Override
            public int compare(EventRouteStatistic first, EventRouteStatistic second) {
                return Integer.compare(second.getTotalEvents(), first.getTotalEvents());
            }
        });
    }

    private static class SpeedAccumulator {
        private final int routeId;
        private final List<String> busCodes = new ArrayList<String>();
        private int samples;
        private double totalSpeed;
        private double maxSpeed = -Double.MAX_VALUE;
        private double minSpeed = Double.MAX_VALUE;
        private LocalDateTime lastUpdate;

        private SpeedAccumulator(int routeId) {
            this.routeId = routeId;
        }

        private void add(BusPosition bus) {
            double speed = bus.getSpeed().doubleValue();
            samples++;
            totalSpeed += speed;
            maxSpeed = Math.max(maxSpeed, speed);
            minSpeed = Math.min(minSpeed, speed);
            if (bus.getBusCode() != null && !busCodes.contains(bus.getBusCode())) {
                busCodes.add(bus.getBusCode());
            }
            if (bus.getTimestamp() != null && (lastUpdate == null || bus.getTimestamp().isAfter(lastUpdate))) {
                lastUpdate = bus.getTimestamp();
            }
        }

        private RouteSpeedStatistic toStatistic() {
            return new RouteSpeedStatistic(routeId, samples, samples == 0 ? 0.0 : totalSpeed / samples,
                    samples == 0 ? 0.0 : maxSpeed, samples == 0 ? 0.0 : minSpeed, busCodes.size(), lastUpdate);
        }
    }

    private static class EventAccumulator {
        private final int routeId;
        private final Map<String, Integer> typeCounts = new LinkedHashMap<String, Integer>();
        private int totalEvents;
        private int highPriorityEvents;
        private int criticalEvents;
        private LocalDateTime lastOccurrence;

        private EventAccumulator(int routeId) {
            this.routeId = routeId;
        }

        private void add(OperationalEvent event) {
            totalEvents++;
            String type = event.getEventType() == null ? "UNKNOWN" : event.getEventType().name();
            Integer count = typeCounts.get(type);
            typeCounts.put(type, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            if (EventPriority.HIGH == event.getPriority()) {
                highPriorityEvents++;
            }
            if (EventPriority.CRITICAL == event.getPriority()) {
                criticalEvents++;
            }
            if (event.getTimestamp() != null && (lastOccurrence == null || event.getTimestamp().isAfter(lastOccurrence))) {
                lastOccurrence = event.getTimestamp();
            }
        }

        private EventRouteStatistic toStatistic() {
            return new EventRouteStatistic(routeId, totalEvents, highPriorityEvents, criticalEvents,
                    mostCommonType(), lastOccurrence);
        }

        private String mostCommonType() {
            String selected = "No data";
            int selectedCount = 0;
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                if (entry.getValue().intValue() > selectedCount) {
                    selected = entry.getKey();
                    selectedCount = entry.getValue().intValue();
                }
            }
            return selected;
        }
    }

    private static class AlertAccumulator {
        private final String priority;
        private int count;
        private String lastAlert = "No data";
        private LocalDateTime lastAlertAt;

        private AlertAccumulator(String priority) {
            this.priority = priority;
        }

        private void add(AlertPanelModel alert) {
            count++;
            if (alert.getTimestamp() != null && (lastAlertAt == null || alert.getTimestamp().isAfter(lastAlertAt))) {
                lastAlertAt = alert.getTimestamp();
                lastAlert = alert.getTitle();
            }
        }

        private AlertPriorityStatistic toStatistic() {
            return new AlertPriorityStatistic(priority, count, lastAlert, lastAlertAt);
        }
    }

    private static class BusRouteAccumulator {
        private final int routeId;
        private final List<String> busCodes = new ArrayList<String>();
        private LocalDateTime lastPositionAt;

        private BusRouteAccumulator(int routeId) {
            this.routeId = routeId;
        }

        private void add(BusMarker bus) {
            if (bus.getBusCode() != null && !busCodes.contains(bus.getBusCode())) {
                busCodes.add(bus.getBusCode());
            }
            if (bus.getLastUpdate() != null && (lastPositionAt == null || bus.getLastUpdate().isAfter(lastPositionAt))) {
                lastPositionAt = bus.getLastUpdate();
            }
        }

        private ActiveBusesByRouteStatistic toStatistic() {
            return new ActiveBusesByRouteStatistic(routeId, busCodes.size(), busCodes, lastPositionAt);
        }
    }

    private static class AverageResult {
        private final double average;
        private final int samples;

        private AverageResult(double average, int samples) {
            this.average = average;
            this.samples = samples;
        }

        private static AverageResult empty() {
            return new AverageResult(0.0, 0);
        }

        private Double getAverageOrNull() {
            return samples == 0 ? null : Double.valueOf(average);
        }

        private int getSamples() {
            return samples;
        }
    }
}
