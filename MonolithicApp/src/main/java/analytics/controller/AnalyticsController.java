package analytics.controller;

import analytics.model.AnalyticsFilter;
import analytics.model.AnalyticsSelectionSummary;
import analytics.model.RouteSpeedAnalyticsRow;
import analytics.model.SystemAnalyticsSnapshot;
import analytics.service.AnalyticsService;
import monitoring.service.MonitoringStateListener;

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

    public List<Integer> getAvailableRoutes() {
        return analyticsService.getAvailableRoutes();
    }

    public List<Integer> getAvailableYears() {
        return analyticsService.getAvailableYears();
    }

    public List<Integer> getAvailableMonthsForYear(int year) {
        return analyticsService.getAvailableMonthsForYear(year);
    }

    public List<RouteSpeedAnalyticsRow> getFilteredRouteSpeedAnalytics(AnalyticsFilter filter) {
        return analyticsService.getFilteredRouteSpeedAnalytics(filter);
    }

    public AnalyticsSelectionSummary getSelectionSummary(AnalyticsFilter filter) {
        return analyticsService.getSelectionSummary(filter);
    }

    public void addStateListener(MonitoringStateListener listener) {
        monitoringController.addStateListener(listener);
    }
}
