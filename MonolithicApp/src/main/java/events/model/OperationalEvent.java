package events.model;

import shared.enums.EventPriority;
import shared.enums.EventSourceType;
import shared.enums.EventStatus;
import shared.enums.EventType;

import java.time.LocalDateTime;

public class OperationalEvent {
    private long id;
    private long busId;
    private String busCode;
    private int routeId;
    private String eventCode;
    private EventType eventType;
    private EventPriority priority;
    private String description;
    private EventSourceType sourceType;
    private EventStatus status;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;

    public OperationalEvent() {
    }

    public OperationalEvent(long id, long busId, String busCode, int routeId, String eventCode, EventType eventType,
                            EventPriority priority, String description, EventSourceType sourceType,
                            EventStatus status, LocalDateTime timestamp, LocalDateTime createdAt) {
        this.id = id;
        this.busId = busId;
        this.busCode = busCode;
        this.routeId = routeId;
        this.eventCode = eventCode;
        this.eventType = eventType;
        this.priority = priority;
        this.description = description;
        this.sourceType = sourceType;
        this.status = status;
        this.timestamp = timestamp;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBusId() {
        return busId;
    }

    public void setBusId(long busId) {
        this.busId = busId;
    }

    public String getBusCode() {
        return busCode;
    }

    public void setBusCode(String busCode) {
        this.busCode = busCode;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public void setPriority(EventPriority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(EventSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
