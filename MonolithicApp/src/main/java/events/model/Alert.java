package events.model;

import shared.enums.EventPriority;

import java.time.LocalDateTime;

public class Alert {
    private long id;
    private long eventId;
    private EventPriority priority;
    private String title;
    private String message;
    private boolean acknowledged;
    private LocalDateTime createdAt;

    public Alert() {
    }

    public Alert(long id, long eventId, EventPriority priority, String title, String message,
                 boolean acknowledged, LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.priority = priority;
        this.title = title;
        this.message = message;
        this.acknowledged = acknowledged;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public void setPriority(EventPriority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
