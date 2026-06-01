package analytics.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SystemAnalyticsSnapshot {
    private final int totalRoutes;
    private final int totalActiveBuses;
    private final int totalPositions;
    private final int totalEvents;
    private final int totalAlerts;
    private final double globalAverageSpeed;
    private final String routeWithMostEvents;
    private final String routeWithHighestAverageSpeed;
    private final String routeWithMostActiveBuses;
    private final ThroughputStatistic throughput;
    private final List<RouteSpeedStatistic> routeSpeedStatistics;
    private final List<EventRouteStatistic> eventRouteStatistics;
    private final List<AlertPriorityStatistic> alertPriorityStatistics;
    private final List<ActiveBusesByRouteStatistic> activeBusesByRouteStatistics;
    private final LocalDateTime refreshedAt;

    public SystemAnalyticsSnapshot(int totalRoutes, int totalActiveBuses, int totalPositions,
                                   int totalEvents, int totalAlerts, double globalAverageSpeed,
                                   String routeWithMostEvents, String routeWithHighestAverageSpeed,
                                   String routeWithMostActiveBuses, ThroughputStatistic throughput,
                                   List<RouteSpeedStatistic> routeSpeedStatistics,
                                   List<EventRouteStatistic> eventRouteStatistics,
                                   List<AlertPriorityStatistic> alertPriorityStatistics,
                                   List<ActiveBusesByRouteStatistic> activeBusesByRouteStatistics,
                                   LocalDateTime refreshedAt) {
        this.totalRoutes = totalRoutes;
        this.totalActiveBuses = totalActiveBuses;
        this.totalPositions = totalPositions;
        this.totalEvents = totalEvents;
        this.totalAlerts = totalAlerts;
        this.globalAverageSpeed = globalAverageSpeed;
        this.routeWithMostEvents = routeWithMostEvents;
        this.routeWithHighestAverageSpeed = routeWithHighestAverageSpeed;
        this.routeWithMostActiveBuses = routeWithMostActiveBuses;
        this.throughput = throughput;
        this.routeSpeedStatistics = new ArrayList<RouteSpeedStatistic>(routeSpeedStatistics);
        this.eventRouteStatistics = new ArrayList<EventRouteStatistic>(eventRouteStatistics);
        this.alertPriorityStatistics = new ArrayList<AlertPriorityStatistic>(alertPriorityStatistics);
        this.activeBusesByRouteStatistics = new ArrayList<ActiveBusesByRouteStatistic>(activeBusesByRouteStatistics);
        this.refreshedAt = refreshedAt;
    }

    public int getTotalRoutes() {
        return totalRoutes;
    }

    public int getTotalActiveBuses() {
        return totalActiveBuses;
    }

    public int getTotalPositions() {
        return totalPositions;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getTotalAlerts() {
        return totalAlerts;
    }

    public double getGlobalAverageSpeed() {
        return globalAverageSpeed;
    }

    public String getRouteWithMostEvents() {
        return routeWithMostEvents;
    }

    public String getRouteWithHighestAverageSpeed() {
        return routeWithHighestAverageSpeed;
    }

    public String getRouteWithMostActiveBuses() {
        return routeWithMostActiveBuses;
    }

    public ThroughputStatistic getThroughput() {
        return throughput;
    }

    public List<RouteSpeedStatistic> getRouteSpeedStatistics() {
        return new ArrayList<RouteSpeedStatistic>(routeSpeedStatistics);
    }

    public List<EventRouteStatistic> getEventRouteStatistics() {
        return new ArrayList<EventRouteStatistic>(eventRouteStatistics);
    }

    public List<AlertPriorityStatistic> getAlertPriorityStatistics() {
        return new ArrayList<AlertPriorityStatistic>(alertPriorityStatistics);
    }

    public List<ActiveBusesByRouteStatistic> getActiveBusesByRouteStatistics() {
        return new ArrayList<ActiveBusesByRouteStatistic>(activeBusesByRouteStatistics);
    }

    public LocalDateTime getRefreshedAt() {
        return refreshedAt;
    }
}
