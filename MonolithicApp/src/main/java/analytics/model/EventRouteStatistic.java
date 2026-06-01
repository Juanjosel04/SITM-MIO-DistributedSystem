package analytics.model;

import java.time.LocalDateTime;

public class EventRouteStatistic {
    private final int routeId;
    private final int totalEvents;
    private final int highPriorityEvents;
    private final int criticalEvents;
    private final String mostCommonType;
    private final LocalDateTime lastOccurrence;

    public EventRouteStatistic(int routeId, int totalEvents, int highPriorityEvents, int criticalEvents,
                               String mostCommonType, LocalDateTime lastOccurrence) {
        this.routeId = routeId;
        this.totalEvents = totalEvents;
        this.highPriorityEvents = highPriorityEvents;
        this.criticalEvents = criticalEvents;
        this.mostCommonType = mostCommonType;
        this.lastOccurrence = lastOccurrence;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getHighPriorityEvents() {
        return highPriorityEvents;
    }

    public int getCriticalEvents() {
        return criticalEvents;
    }

    public String getMostCommonType() {
        return mostCommonType;
    }

    public LocalDateTime getLastOccurrence() {
        return lastOccurrence;
    }
}
