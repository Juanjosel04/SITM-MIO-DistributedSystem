package core.observer.events;

import java.time.LocalDateTime;

public class SystemEvent {
    private final SystemEventType eventType;
    private final String message;
    private final LocalDateTime timestamp;
    private final Object payload;

    public SystemEvent(SystemEventType eventType, String message, Object payload) {
        this.eventType = eventType;
        this.message = message;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public SystemEventType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Object getPayload() {
        return payload;
    }
}
