package app;

import core.observer.subject.SystemEventPublisher;
import events.service.EventService;
import monitoring.controller.MonitoringController;
import monitoring.service.MonitoringService;
import monitoring.view.MainDashboardView;
import persistence.connection.DatabaseConfig;
import persistence.connection.DatabaseConnectionManager;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

public class Main {
    public static void main(String[] args) {
        if (isUiEnabled()) {
            startMonitoringApplication();
        } else {
            new ApplicationInitializer().run();
        }
    }

    private static void startMonitoringApplication() {
        final SystemEventPublisher eventPublisher = new SystemEventPublisher();
        final MonitoringService monitoringService = new MonitoringService();
        final MonitoringController monitoringController = new MonitoringController(monitoringService);
        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(new DatabaseConfig());
        boolean persistenceEnabled = connectionManager.canConnect();
        final EventService eventService = ApplicationInitializer.createEventService(connectionManager,
                persistenceEnabled, eventPublisher);
        eventPublisher.addListener(monitoringService);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainDashboardView dashboardView = new MainDashboardView(monitoringController, eventService);
                dashboardView.showView();
            }
        });

        Thread pipelineThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new ApplicationInitializer(eventPublisher, eventService).run();
            }
        }, "sitm-mio-pipeline");
        pipelineThread.start();
    }

    private static boolean isUiEnabled() {
        String property = System.getProperty("sitm.ui.enabled");
        if (property != null && "false".equalsIgnoreCase(property.trim())) {
            return false;
        }
        return !GraphicsEnvironment.isHeadless();
    }
}
