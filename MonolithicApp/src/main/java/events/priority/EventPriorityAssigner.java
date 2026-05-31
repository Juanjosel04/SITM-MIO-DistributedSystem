package events.priority;

import shared.enums.EventPriority;
import shared.enums.EventType;

public class EventPriorityAssigner {
    public EventPriority assign(EventType eventType) {
        if (eventType == null) {
            return EventPriority.MEDIUM;
        }
        if (EventType.COLLISION == eventType || EventType.SECURITY_INCIDENT == eventType) {
            return EventPriority.CRITICAL;
        }
        if (EventType.MECHANICAL_FAILURE == eventType || EventType.DOOR_FAILURE == eventType ||
                EventType.FLAT_TIRE == eventType || EventType.HIGH_SPEED == eventType ||
                EventType.PROCESSING_ERROR == eventType) {
            return EventPriority.HIGH;
        }
        if (EventType.TRAFFIC_JAM == eventType || EventType.GPS_ANOMALY == eventType ||
                EventType.LOW_SPEED == eventType || EventType.GENERAL_OPERATIONAL_EVENT == eventType) {
            return EventPriority.MEDIUM;
        }
        return EventPriority.LOW;
    }
}
