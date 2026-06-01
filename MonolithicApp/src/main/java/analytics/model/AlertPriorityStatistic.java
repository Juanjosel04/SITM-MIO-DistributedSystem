package analytics.model;

import java.time.LocalDateTime;

public class AlertPriorityStatistic {
    private final String priority;
    private final int count;
    private final String lastAlert;
    private final LocalDateTime lastAlertAt;

    public AlertPriorityStatistic(String priority, int count, String lastAlert, LocalDateTime lastAlertAt) {
        this.priority = priority;
        this.count = count;
        this.lastAlert = lastAlert;
        this.lastAlertAt = lastAlertAt;
    }

    public String getPriority() {
        return priority;
    }

    public int getCount() {
        return count;
    }

    public String getLastAlert() {
        return lastAlert;
    }

    public LocalDateTime getLastAlertAt() {
        return lastAlertAt;
    }
}
