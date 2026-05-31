package monitoring.model;

import core.observer.notifications.NotificationLevel;

import java.time.LocalDateTime;

public class AlertPanelModel {
    private final String title;
    private final String message;
    private final NotificationLevel level;
    private final LocalDateTime timestamp;

    public AlertPanelModel(String title, String message, NotificationLevel level, LocalDateTime timestamp) {
        this.title = title;
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
