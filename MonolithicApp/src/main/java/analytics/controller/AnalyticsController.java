package analytics.controller;

import analytics.model.AnalyticsFilter;
import analytics.model.AnalyticsSelectionSummary;
import analytics.model.RouteSpeedAnalyticsRow;
import analytics.model.SystemAnalyticsSnapshot;
import analytics.service.AnalyticsService;
import monitoring.service.MonitoringStateListener;
import security.model.AccessScope;

import java.util.List;

public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final monitoring.controller.MonitoringController monitoringController;

    public AnalyticsController(AnalyticsService analyticsService,
                               monitoring.controller.MonitoringController monitoringController) {
        this.analyticsService = analyticsService;
        this.monitoringController = monitoringController;
    }

    public SystemAnalyticsSnapshot refreshSnapshot() {
        return analyticsService.refreshSnapshot();
    }

    public SystemAnalyticsSnapshot refreshSnapshot(AccessScope scope) {
        return analyticsService.refreshSnapshot(scope);
    }

    public List<Integer> getAvailableRoutes() {
        return analyticsService.getAvailableRoutes();
    }

    public List<Integer> getAvailableRoutes(AccessScope scope) {
        return analyticsService.getAvailableRoutes(scope);
    }

    public List<Integer> getAvailableYears() {
        return analyticsService.getAvailableYears();
    }

    public List<Integer> getAvailableYears(AccessScope scope) {
        return analyticsService.getAvailableYears(scope);
    }

    public List<Integer> getAvailableMonthsForYear(int year) {
        return analyticsService.getAvailableMonthsForYear(year);
    }

    public List<Integer> getAvailableMonthsForYear(int year, AccessScope scope) {
        return analyticsService.getAvailableMonthsForYear(year, scope);
    }

    public List<RouteSpeedAnalyticsRow> getFilteredRouteSpeedAnalytics(AnalyticsFilter filter) {
        return analyticsService.getFilteredRouteSpeedAnalytics(filter);
    }

    public List<RouteSpeedAnalyticsRow> getFilteredRouteSpeedAnalytics(AnalyticsFilter filter, AccessScope scope) {
        return analyticsService.getFilteredRouteSpeedAnalytics(filter, scope);
    }

    public AnalyticsSelectionSummary getSelectionSummary(AnalyticsFilter filter) {
        return analyticsService.getSelectionSummary(filter);
    }

    public AnalyticsSelectionSummary getSelectionSummary(AnalyticsFilter filter, AccessScope scope) {
        return analyticsService.getSelectionSummary(filter, scope);
    }

    public void addStateListener(MonitoringStateListener listener) {
        monitoringController.addStateListener(listener);
    }

    public String getRouteDisplayName(int routeId) {
        return monitoringController.getRouteDisplayName(routeId);
    }

    public String getRouteFilterLabel(int routeId) {
        return monitoringController.getRouteFilterLabel(routeId);
    }

    public String getRouteFullDisplayName(int routeId) {
        return monitoringController.getRouteFullDisplayName(routeId);
    }
}
