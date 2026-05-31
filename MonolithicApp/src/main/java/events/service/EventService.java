package events.service;

import core.model.Datagram;
import core.observer.events.SystemEvent;
import core.observer.events.SystemEventType;
import core.observer.subject.SystemEventPublisher;
import core.utils.AppLogger;
import events.alerts.AlertService;
import events.classification.EventClassifier;
import events.model.Alert;
import events.model.DriverEventRequest;
import events.model.EventProcessingResult;
import events.model.OperationalEvent;
import events.priority.EventPriorityAssigner;
import persistence.repository.AlertRepository;
import persistence.repository.EventRepository;
import shared.enums.EventPriority;
import shared.enums.EventSourceType;
import shared.enums.EventStatus;
import shared.enums.EventType;
import shared.exceptions.RepositoryException;

import java.time.LocalDateTime;

public class EventService {
    private final EventClassifier eventClassifier;
    private final EventPriorityAssigner priorityAssigner;
    private final AlertService alertService;
    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final SystemEventPublisher eventPublisher;
    private final boolean persistenceEnabled;

    public EventService(EventClassifier eventClassifier, EventPriorityAssigner priorityAssigner,
                        AlertService alertService, EventRepository eventRepository, AlertRepository alertRepository,
                        SystemEventPublisher eventPublisher, boolean persistenceEnabled) {
        this.eventClassifier = eventClassifier;
        this.priorityAssigner = priorityAssigner;
        this.alertService = alertService;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
        this.persistenceEnabled = persistenceEnabled;
    }

    public EventProcessingResult processDriverEvent(DriverEventRequest request) {
        publish(SystemEventType.DRIVER_EVENT_SENT, "Driver event sent", request);
        EventType eventType = eventClassifier.classifyDriverEvent(request);
        OperationalEvent event = createEvent(request.getBusCode(), request.getRouteId(), request.getEventCode(),
                eventType, request.getDescription(), EventSourceType.DRIVER_MANUAL, request.getTimestamp());
        return saveAndPublish(event);
    }

    public EventProcessingResult processAutomaticDatagramEvent(Datagram datagram) {
        EventType eventType = eventClassifier.classifyAutomaticEvent(datagram);
        if (eventType == null) {
            return new EventProcessingResult(true, "No operational event detected", null, null, LocalDateTime.now());
        }
        String description = eventType == EventType.LOW_SPEED ? "Very low speed detected" : "High speed detected";
        OperationalEvent event = createEvent(datagram.getBusCode(), datagram.getRouteId(), eventType.name(),
                eventType, description, EventSourceType.AUTOMATIC, datagram.getTimestamp());
        return saveAndPublish(event);
    }

    public EventProcessingResult processInvalidDatagram(String message) {
        EventType eventType = eventClassifier.classifyInvalidDatagram(message);
        OperationalEvent event = createEvent(null, 0, eventType.name(), eventType, message,
                EventSourceType.SYSTEM, LocalDateTime.now());
        return saveAndPublish(event);
    }

    public EventProcessingResult processProcessingError(String message) {
        EventType eventType = eventClassifier.classifyProcessingError(message);
        OperationalEvent event = createEvent(null, 0, eventType.name(), eventType, message,
                EventSourceType.SYSTEM, LocalDateTime.now());
        return saveAndPublish(event);
    }

    private OperationalEvent createEvent(String busCode, int routeId, String eventCode, EventType eventType,
                                         String description, EventSourceType sourceType, LocalDateTime timestamp) {
        EventPriority priority = priorityAssigner.assign(eventType);
        LocalDateTime now = LocalDateTime.now();
        return new OperationalEvent(0L, 0L, busCode, routeId, eventCode, eventType, priority,
                normalizeDescription(description, eventType), sourceType, EventStatus.OPEN,
                timestamp == null ? now : timestamp, now);
    }

    private EventProcessingResult saveAndPublish(OperationalEvent event) {
        try {
            OperationalEvent savedEvent = saveEvent(event);
            Alert alert = alertService.createAlertIfNeeded(savedEvent);
            Alert savedAlert = saveAlert(alert);
            publish(SystemEventType.OPERATIONAL_EVENT_CREATED, "Operational event created", savedEvent);
            if (savedAlert != null) {
                publish(SystemEventType.ALERT_CREATED, "Alert created", savedAlert);
            }
            return new EventProcessingResult(true, "Operational event processed", savedEvent, savedAlert,
                    LocalDateTime.now());
        } catch (RepositoryException exception) {
            AppLogger.warn(exception.getMessage());
            publish(SystemEventType.EVENT_PROCESSING_ERROR, exception.getMessage(), exception.getMessage());
            return new EventProcessingResult(false, exception.getMessage(), event, null, LocalDateTime.now());
        }
    }

    private OperationalEvent saveEvent(OperationalEvent event) throws RepositoryException {
        if (persistenceEnabled) {
            return eventRepository.save(event);
        }
        return event;
    }

    private Alert saveAlert(Alert alert) throws RepositoryException {
        if (alert == null) {
            return null;
        }
        if (persistenceEnabled) {
            return alertRepository.save(alert);
        }
        return alert;
    }

    private String normalizeDescription(String description, EventType eventType) {
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return eventType == null ? "Operational event" : eventType.name();
    }

    private void publish(SystemEventType eventType, String message, Object payload) {
        if (eventPublisher != null) {
            eventPublisher.publish(new SystemEvent(eventType, message, payload));
        }
    }
}
