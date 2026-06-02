package app;

import analytics.controller.AnalyticsController;
import analytics.service.AnalyticsService;
import core.observer.subject.SystemEventPublisher;
import core.utils.AppLogger;
import events.service.EventService;
import javafx.application.Application;
import javafx.stage.Stage;
import monitoring.controller.MonitoringController;
import monitoring.service.MonitoringService;
import persistence.connection.DatabaseConfig;
import persistence.connection.DatabaseConnectionManager;
import security.auth.AuthService;
import security.navigation.NavigationController;

public class SitmMioFxApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        AppLogger.info("JavaFX application starting.");

        SystemEventPublisher eventPublisher = new SystemEventPublisher();
        MonitoringService monitoringService = new MonitoringService();
        MonitoringController monitoringController = new MonitoringController(monitoringService);
        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(new DatabaseConfig());
        boolean persistenceEnabled = connectionManager.canConnect();
        EventService eventService = ApplicationInitializer.createEventService(connectionManager,
                persistenceEnabled, eventPublisher);

        eventPublisher.addListener(monitoringService);

        AnalyticsService analyticsService = new AnalyticsService(monitoringController);
        AnalyticsController analyticsController = new AnalyticsController(analyticsService, monitoringController);

        AuthService authService = new AuthService();
        NavigationController navigationController = new NavigationController(primaryStage, authService,
                monitoringController, eventService, analyticsController);
        navigationController.showLogin();

        startPipeline(eventPublisher, eventService);
        AppLogger.info("JavaFX application initialized.");
    }

    private void startPipeline(final SystemEventPublisher eventPublisher, final EventService eventService) {
        Thread pipelineThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new ApplicationInitializer(eventPublisher, eventService).run();
            }
        }, "sitm-mio-pipeline");
        pipelineThread.setDaemon(true);
        pipelineThread.start();
    }
}
