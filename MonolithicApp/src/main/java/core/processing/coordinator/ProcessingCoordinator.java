package core.processing.coordinator;

import core.model.Datagram;
import core.model.PipelineSummary;
import core.observer.events.SystemEvent;
import core.observer.events.SystemEventType;
import core.observer.subject.SystemEventPublisher;
import core.processing.filtering.DatagramFilter;
import core.processing.parser.DatagramParser;
import core.processing.validation.DatagramValidator;
import core.queue.DatagramQueue;
import core.workers.DatagramWorker;
import events.service.EventService;
import shared.exceptions.CsvParsingException;
import shared.exceptions.InvalidDatagramException;

public class ProcessingCoordinator {
    private final DatagramParser datagramParser;
    private final DatagramValidator datagramValidator;
    private final DatagramFilter datagramFilter;
    private final DatagramQueue datagramQueue;
    private final DatagramWorker datagramWorker;
    private final PipelineSummary summary;
    private final SystemEventPublisher eventPublisher;
    private final EventService eventService;

    public ProcessingCoordinator(DatagramParser datagramParser, DatagramValidator datagramValidator,
                                 DatagramFilter datagramFilter, DatagramQueue datagramQueue,
                                 DatagramWorker datagramWorker, PipelineSummary summary) {
        this(datagramParser, datagramValidator, datagramFilter, datagramQueue, datagramWorker, summary, null);
    }

    public ProcessingCoordinator(DatagramParser datagramParser, DatagramValidator datagramValidator,
                                 DatagramFilter datagramFilter, DatagramQueue datagramQueue,
                                 DatagramWorker datagramWorker, PipelineSummary summary,
                                 SystemEventPublisher eventPublisher) {
        this(datagramParser, datagramValidator, datagramFilter, datagramQueue, datagramWorker, summary,
                eventPublisher, null);
    }

    public ProcessingCoordinator(DatagramParser datagramParser, DatagramValidator datagramValidator,
                                 DatagramFilter datagramFilter, DatagramQueue datagramQueue,
                                 DatagramWorker datagramWorker, PipelineSummary summary,
                                 SystemEventPublisher eventPublisher, EventService eventService) {
        this.datagramParser = datagramParser;
        this.datagramValidator = datagramValidator;
        this.datagramFilter = datagramFilter;
        this.datagramQueue = datagramQueue;
        this.datagramWorker = datagramWorker;
        this.summary = summary;
        this.eventPublisher = eventPublisher;
        this.eventService = eventService;
    }

    public void processLine(String line) {
        summary.incrementDatagramsRead();
        try {
            Datagram datagram = datagramParser.parse(line);
            datagramValidator.validate(datagram);
            if (datagramFilter.isProcessable(datagram)) {
                datagramQueue.add(datagram);
                summary.incrementDatagramsValid();
                datagramWorker.consumeAvailable(summary);
            } else {
                summary.incrementDatagramsInvalid();
                publishInvalid("Datagram was not processable", line);
            }
        } catch (CsvParsingException exception) {
            summary.incrementDatagramsInvalid();
            publishInvalid(exception.getMessage(), line);
        } catch (InvalidDatagramException exception) {
            summary.incrementDatagramsInvalid();
            publishInvalid(exception.getMessage(), line);
        }
    }

    public void finish() {
        datagramWorker.consumeAvailable(summary);
    }

    private void publishInvalid(String message, String rawPayload) {
        if (eventPublisher != null) {
            String payload = rawPayload == null ? "" : rawPayload;
            if (payload.length() > 120) {
                payload = payload.substring(0, 120);
            }
            eventPublisher.publish(new SystemEvent(SystemEventType.DATAGRAM_INVALID, message, payload));
        }
        if (eventService != null) {
            eventService.processInvalidDatagram(message);
        }
    }
}
