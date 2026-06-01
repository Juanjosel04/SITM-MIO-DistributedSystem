package analytics.model;

import java.time.LocalDateTime;
import java.util.List;

public class ActiveBusesByRouteStatistic {
    private final int routeId;
    private final int activeBuses;
    private final String busCodes;
    private final LocalDateTime lastPositionAt;

    public ActiveBusesByRouteStatistic(int routeId, int activeBuses, List<String> busCodes,
                                       LocalDateTime lastPositionAt) {
        this.routeId = routeId;
        this.activeBuses = activeBuses;
        this.busCodes = String.join(", ", busCodes);
        this.lastPositionAt = lastPositionAt;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getActiveBuses() {
        return activeBuses;
    }

    public String getBusCodes() {
        return busCodes;
    }

    public LocalDateTime getLastPositionAt() {
        return lastPositionAt;
    }
}
