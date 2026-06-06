package master.merge;

import master.results.RemoteRouteMonthPartial;

import java.util.LinkedHashSet;
import java.util.Set;

public final class GlobalSpeedAccumulator {
    private double totalDistanceMeters;
    private double totalTimeSeconds;
    private long validIntervals;
    private final Set<String> contributingWorkers = new LinkedHashSet<String>();

    public void mergeFrom(RemoteRouteMonthPartial partial) {
        totalDistanceMeters += partial.getTotalDistanceMeters();
        totalTimeSeconds += partial.getTotalTimeSeconds();
        validIntervals += partial.getValidIntervals();
        contributingWorkers.add(partial.getWorkerId());
    }

    public double averageKmh() {
        if (totalTimeSeconds <= 0) {
            return 0;
        }
        return totalDistanceMeters / totalTimeSeconds * 3.6;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public double getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public long getValidIntervals() {
        return validIntervals;
    }

    public int getContributingWorkerCount() {
        return contributingWorkers.size();
    }
}
