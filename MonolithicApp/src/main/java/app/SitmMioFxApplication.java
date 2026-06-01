package app;

import core.observer.subject.SystemEventPublisher;
import core.utils.AppLogger;
import events.service.EventService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import monitoring.controller.MonitoringController;
import monitoring.service.MonitoringService;
import monitoring.view.fx.MainDashboardFxShell;
import persistence.connection.DatabaseConfig;
import persistence.connection.DatabaseConnectionManager;

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

        MainDashboardFxShell shell = new MainDashboardFxShell(monitoringController, eventService);
        Scene scene = new Scene(shell, 1120, 720);

        primaryStage.setTitle("SITM-MIO Monitoring Center");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.show();

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
