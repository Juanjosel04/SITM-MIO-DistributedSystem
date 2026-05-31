package events.model;

import java.time.LocalDateTime;

public class EventProcessingResult {
    private final boolean success;
    private final String message;
    private final OperationalEvent operationalEvent;
    private final Alert alert;
    private final LocalDateTime processedAt;

    public EventProcessingResult(boolean success, String message, OperationalEvent operationalEvent, Alert alert,
                                 LocalDateTime processedAt) {
        this.success = success;
        this.message = message;
        this.operationalEvent = operationalEvent;
        this.alert = alert;
        this.processedAt = processedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public OperationalEvent getOperationalEvent() {
        return operationalEvent;
    }

    public Alert getAlert() {
        return alert;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
