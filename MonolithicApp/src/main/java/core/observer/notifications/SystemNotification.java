package core.observer.notifications;

import java.time.LocalDateTime;

public class SystemNotification {
    private final String title;
    private final String message;
    private final NotificationLevel level;
    private final LocalDateTime timestamp;

    public SystemNotification(String title, String message, NotificationLevel level, LocalDateTime timestamp) {
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
