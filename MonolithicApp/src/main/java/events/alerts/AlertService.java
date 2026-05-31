package events.alerts;

import events.model.Alert;
import events.model.OperationalEvent;
import shared.enums.EventPriority;

import java.time.LocalDateTime;

public class AlertService {
    public Alert createAlertIfNeeded(OperationalEvent event) {
        if (event == null || event.getPriority() == EventPriority.LOW) {
            return null;
        }
        String title = event.getPriority().name() + " operational event";
        String message = buildMessage(event);
        return new Alert(0L, event.getId(), event.getPriority(), title, message, false, LocalDateTime.now());
    }

    private String buildMessage(OperationalEvent event) {
        String bus = event.getBusCode() == null || event.getBusCode().trim().isEmpty()
                ? "unknown bus" : "bus " + event.getBusCode();
        return event.getEventType().name() + " reported for " + bus + " on route " + event.getRouteId();
    }
}
