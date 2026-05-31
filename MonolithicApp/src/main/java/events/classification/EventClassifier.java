package events.classification;

import core.model.Datagram;
import events.model.DriverEventRequest;
import shared.enums.EventType;

public class EventClassifier {
    public EventType classifyDriverEvent(DriverEventRequest request) {
        if (request == null) {
            return EventType.GENERAL_OPERATIONAL_EVENT;
        }
        return classifyText(request.getEventCode(), request.getDescription());
    }

    public EventType classifyAutomaticEvent(Datagram datagram) {
        if (datagram == null || datagram.getSpeed() == null) {
            return null;
        }
        double speed = datagram.getSpeed().doubleValue();
        if (speed <= 1.0) {
            return EventType.LOW_SPEED;
        }
        if (speed >= 80.0) {
            return EventType.HIGH_SPEED;
        }
        return null;
    }

    public EventType classifyInvalidDatagram(String message) {
        return EventType.INVALID_DATAGRAM;
    }

    public EventType classifyProcessingError(String message) {
        return EventType.PROCESSING_ERROR;
    }

    private EventType classifyText(String eventCode, String description) {
        String text = ((eventCode == null ? "" : eventCode) + " " +
                (description == null ? "" : description)).toUpperCase();
        if (text.contains("FLAT") || text.contains("TIRE") || text.contains("PINCHAZO")) {
            return EventType.FLAT_TIRE;
        }
        if (text.contains("MECHANICAL") || text.contains("MOTOR") || text.contains("AVER")) {
            return EventType.MECHANICAL_FAILURE;
        }
        if (text.contains("JAM") || text.contains("TRAFFIC") || text.contains("TRANCON") || text.contains("TRANC")) {
            return EventType.TRAFFIC_JAM;
        }
        if (text.contains("COLLISION") || text.contains("CRASH") || text.contains("CHOQUE")) {
            return EventType.COLLISION;
        }
        if (text.contains("DOOR") || text.contains("PUERTA")) {
            return EventType.DOOR_FAILURE;
        }
        if (text.contains("SECURITY") || text.contains("SEGURIDAD") || text.contains("INCIDENT")) {
            return EventType.SECURITY_INCIDENT;
        }
        return EventType.GENERAL_OPERATIONAL_EVENT;
    }
}
