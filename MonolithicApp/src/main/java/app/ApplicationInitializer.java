package app;

import core.ingestion.CsvReader;
import core.ingestion.DatagramLoader;
import core.ingestion.RouteLoader;
import core.model.PipelineSummary;
import core.observer.events.SystemEvent;
import core.observer.events.SystemEventType;
import core.observer.subject.SystemEventPublisher;
import core.processing.coordinator.ProcessingCoordinator;
import core.processing.filtering.DatagramFilter;
import core.processing.parser.DatagramParser;
import core.processing.parser.RouteParser;
import core.processing.validation.DatagramValidator;
import core.queue.DatagramQueue;
import core.utils.AppLogger;
import core.workers.DatagramWorker;
import events.alerts.AlertService;
import events.classification.EventClassifier;
import events.priority.EventPriorityAssigner;
import events.service.EventService;
import persistence.connection.DatabaseConfig;
import persistence.connection.DatabaseConnectionManager;
import persistence.repository.AlertRepository;
import persistence.repository.BusPositionRepository;
import persistence.repository.BusRepository;
import persistence.repository.DatagramRepository;
import persistence.repository.EventRepository;
import persistence.repository.RouteRepository;
import shared.constants.DatasetPaths;
import shared.constants.ProcessingConstants;

import java.io.IOException;

public class ApplicationInitializer {
    private final SystemEventPublisher eventPublisher;
    private final EventService configuredEventService;

    public ApplicationInitializer() {
        this(new SystemEventPublisher(), null);
    }

    public ApplicationInitializer(SystemEventPublisher eventPublisher) {
        this(eventPublisher, null);
    }

    public ApplicationInitializer(SystemEventPublisher eventPublisher, EventService configuredEventService) {
        this.eventPublisher = eventPublisher;
        this.configuredEventService = configuredEventService;
    }

    public void run() {
        PipelineSummary summary = new PipelineSummary();
        summary.start();

        AppLogger.info("Application started.");

        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(new DatabaseConfig());
        boolean persistenceEnabled = connectionManager.canConnect();
        if (persistenceEnabled) {
            AppLogger.info("PostgreSQL connection available.");
        } else {
            AppLogger.warn("PostgreSQL connection unavailable. Pipeline will run without persistence.");
        }

        CsvReader csvReader = new CsvReader();
        RouteRepository routeRepository = new RouteRepository(connectionManager);
        RouteLoader routeLoader = new RouteLoader(csvReader, new RouteParser(), routeRepository);
        EventService eventService = configuredEventService == null
                ? createEventService(connectionManager, persistenceEnabled, eventPublisher)
                : configuredEventService;

        try {
            int routesLoaded = routeLoader.loadRoutes(DatasetPaths.ROUTES_FILE, persistenceEnabled);
            summary.setRoutesLoaded(routesLoaded);
            AppLogger.info("Routes loaded: " + routesLoaded);
            publish(SystemEventType.ROUTES_LOADED, "Routes loaded", Integer.valueOf(routesLoaded));
        } catch (IOException exception) {
            summary.incrementErrors();
            AppLogger.error("Route loading failed: " + exception.getMessage());
            publish(SystemEventType.PROCESSING_ERROR, exception.getMessage(), exception.getMessage());
        }

        DatagramQueue datagramQueue = new DatagramQueue();
        BusRepository busRepository = new BusRepository(connectionManager);
        DatagramRepository datagramRepository = new DatagramRepository(connectionManager);
        BusPositionRepository busPositionRepository = new BusPositionRepository(connectionManager);
        DatagramWorker datagramWorker = new DatagramWorker(datagramQueue, busRepository, datagramRepository,
                busPositionRepository, persistenceEnabled, eventPublisher, eventService);
        ProcessingCoordinator processingCoordinator = new ProcessingCoordinator(new DatagramParser(),
                new DatagramValidator(), new DatagramFilter(), datagramQueue, datagramWorker, summary, eventPublisher,
                eventService);
        DatagramLoader datagramLoader = new DatagramLoader(csvReader, processingCoordinator,
                ProcessingConstants.STREAMING_DELAY_MILLIS);

        AppLogger.info("Datagram streaming started.");
        publish(SystemEventType.STREAM_STARTED, "Datagram streaming started", DatasetPaths.DATAGRAMS_FILE);
        try {
            datagramLoader.stream(DatasetPaths.DATAGRAMS_FILE);
        } catch (IOException exception) {
            summary.incrementErrors();
            AppLogger.error("Datagram streaming failed: " + exception.getMessage());
            publish(SystemEventType.PROCESSING_ERROR, exception.getMessage(), exception.getMessage());
        }

        summary.finish();
        publish(SystemEventType.STREAM_FINISHED, "Datagram streaming finished", summary);
        logSummary(summary);
    }

    private void logSummary(PipelineSummary summary) {
        AppLogger.info("Datagrams read: " + summary.getDatagramsRead());
        AppLogger.info("Datagrams valid: " + summary.getDatagramsValid());
        AppLogger.info("Datagrams invalid: " + summary.getDatagramsInvalid());
        AppLogger.info("Datagrams processed: " + summary.getDatagramsProcessed());
        AppLogger.info("Bus positions saved: " + summary.getPositionsSaved());
        AppLogger.info("Errors: " + summary.getErrors());
        AppLogger.info("Processing time: " + summary.getElapsedMillis() + " ms");
        AppLogger.info("Processing finished.");
    }

    private void publish(SystemEventType eventType, String message, Object payload) {
        if (eventPublisher != null) {
            eventPublisher.publish(new SystemEvent(eventType, message, payload));
        }
    }

    public static EventService createEventService(DatabaseConnectionManager connectionManager,
                                                  boolean persistenceEnabled,
                                                  SystemEventPublisher eventPublisher) {
        EventRepository eventRepository = new EventRepository(connectionManager);
        AlertRepository alertRepository = new AlertRepository(connectionManager);
        return new EventService(new EventClassifier(), new EventPriorityAssigner(), new AlertService(),
                eventRepository, alertRepository, eventPublisher, persistenceEnabled);
    }
}
