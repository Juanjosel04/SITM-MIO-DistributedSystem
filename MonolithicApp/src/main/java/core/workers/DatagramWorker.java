package core.workers;

import core.model.Bus;
import core.model.BusPosition;
import core.model.Datagram;
import core.model.PipelineSummary;
import core.model.ProcessingResult;
import core.observer.events.SystemEvent;
import core.observer.events.SystemEventType;
import core.observer.subject.SystemEventPublisher;
import core.queue.DatagramQueue;
import events.service.EventService;
import persistence.repository.BusPositionRepository;
import persistence.repository.BusRepository;
import persistence.repository.DatagramRepository;
import shared.enums.BusStatus;
import shared.exceptions.RepositoryException;

import java.time.LocalDateTime;

public class DatagramWorker {
    private final DatagramQueue queue;
    private final BusRepository busRepository;
    private final DatagramRepository datagramRepository;
    private final BusPositionRepository busPositionRepository;
    private final boolean persistenceEnabled;
    private final SystemEventPublisher eventPublisher;
    private final EventService eventService;

    public DatagramWorker(DatagramQueue queue, BusRepository busRepository, DatagramRepository datagramRepository,
                          BusPositionRepository busPositionRepository, boolean persistenceEnabled) {
        this(queue, busRepository, datagramRepository, busPositionRepository, persistenceEnabled, null);
    }

    public DatagramWorker(DatagramQueue queue, BusRepository busRepository, DatagramRepository datagramRepository,
                          BusPositionRepository busPositionRepository, boolean persistenceEnabled,
                          SystemEventPublisher eventPublisher) {
        this(queue, busRepository, datagramRepository, busPositionRepository, persistenceEnabled, eventPublisher, null);
    }

    public DatagramWorker(DatagramQueue queue, BusRepository busRepository, DatagramRepository datagramRepository,
                          BusPositionRepository busPositionRepository, boolean persistenceEnabled,
                          SystemEventPublisher eventPublisher, EventService eventService) {
        this.queue = queue;
        this.busRepository = busRepository;
        this.datagramRepository = datagramRepository;
        this.busPositionRepository = busPositionRepository;
        this.persistenceEnabled = persistenceEnabled;
        this.eventPublisher = eventPublisher;
        this.eventService = eventService;
    }

    public void consumeAvailable(PipelineSummary summary) {
        while (!queue.isEmpty()) {
            ProcessingResult result = process(queue.poll(), summary);
            if (result.isSuccess()) {
                summary.incrementDatagramsProcessed();
            } else {
                summary.incrementErrors();
            }
        }
    }

    private ProcessingResult process(Datagram datagram, PipelineSummary summary) {
        if (datagram == null) {
            return new ProcessingResult(false, "Datagram was empty", null, LocalDateTime.now());
        }

        Bus bus = new Bus(0L, datagram.getBusCode(), null, datagram.getRouteId(), BusStatus.ACTIVE.name());
        BusPosition position = new BusPosition(0L, 0L, datagram.getBusCode(), datagram.getRouteId(),
                datagram.getLatitude(), datagram.getLongitude(), datagram.getSpeed(), datagram.getTimestamp());

        if (!persistenceEnabled) {
            publishPosition(position);
            publishProcessed(datagram);
            processAutomaticEvent(datagram);
            return new ProcessingResult(true, "Processed without persistence", datagram, LocalDateTime.now());
        }

        try {
            Bus savedBus = busRepository.saveOrUpdate(bus);
            position.setBusId(savedBus.getId());
            datagramRepository.save(datagram, savedBus);
            busPositionRepository.save(position);
            summary.incrementPositionsSaved();
            publishPosition(position);
            publishProcessed(datagram);
            processAutomaticEvent(datagram);
            return new ProcessingResult(true, "Processed", datagram, LocalDateTime.now());
        } catch (RepositoryException exception) {
            publishError(exception.getMessage());
            processProcessingError(exception.getMessage());
            return new ProcessingResult(false, exception.getMessage(), datagram, LocalDateTime.now());
        }
    }

    private void publishPosition(BusPosition position) {
        if (eventPublisher != null) {
            eventPublisher.publish(new SystemEvent(SystemEventType.BUS_POSITION_UPDATED,
                    "Bus position updated", position));
        }
    }

    private void publishProcessed(Datagram datagram) {
        if (eventPublisher != null) {
            eventPublisher.publish(new SystemEvent(SystemEventType.DATAGRAM_PROCESSED,
                    "Datagram processed", datagram));
        }
    }

    private void publishError(String message) {
        if (eventPublisher != null) {
            eventPublisher.publish(new SystemEvent(SystemEventType.PROCESSING_ERROR, message, message));
        }
    }

    private void processAutomaticEvent(Datagram datagram) {
        if (eventService != null) {
            eventService.processAutomaticDatagramEvent(datagram);
        }
    }

    private void processProcessingError(String message) {
        if (eventService != null) {
            eventService.processProcessingError(message);
        }
    }
}
