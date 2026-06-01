package analytics.model;

import java.time.LocalDateTime;

public class RouteSpeedStatistic {
    private final int routeId;
    private final int samples;
    private final double averageSpeed;
    private final double maxSpeed;
    private final double minSpeed;
    private final int activeBuses;
    private final LocalDateTime lastUpdate;

    public RouteSpeedStatistic(int routeId, int samples, double averageSpeed, double maxSpeed,
                               double minSpeed, int activeBuses, LocalDateTime lastUpdate) {
        this.routeId = routeId;
        this.samples = samples;
        this.averageSpeed = averageSpeed;
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.activeBuses = activeBuses;
        this.lastUpdate = lastUpdate;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getSamples() {
        return samples;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public int getActiveBuses() {
        return activeBuses;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }
}
