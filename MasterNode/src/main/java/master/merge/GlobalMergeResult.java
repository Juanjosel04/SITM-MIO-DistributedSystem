package master.merge;

import java.util.Collections;
import java.util.List;

public final class GlobalMergeResult {
    private final boolean success;
    private final String jobId;
    private final String message;
    private final int totalWorkersRequested;
    private final int totalWorkersMerged;
    private final int failedWorkers;
    private final int totalPartialRows;
    private final int totalGlobalRows;
    private final long elapsedMillis;
    private final GlobalMergeCounters counters;
    private final List<GlobalMergedResultRow> rows;

    public GlobalMergeResult(boolean success, String jobId, String message, int totalWorkersRequested,
                             int totalWorkersMerged, int failedWorkers, int totalPartialRows, long elapsedMillis,
                             GlobalMergeCounters counters, List<GlobalMergedResultRow> rows) {
        this.success = success;
        this.jobId = jobId;
        this.message = message;
        this.totalWorkersRequested = totalWorkersRequested;
        this.totalWorkersMerged = totalWorkersMerged;
        this.failedWorkers = failedWorkers;
        this.totalPartialRows = totalPartialRows;
        this.totalGlobalRows = rows.size();
        this.elapsedMillis = elapsedMillis;
        this.counters = counters;
        this.rows = Collections.unmodifiableList(rows);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getJobId() {
        return jobId;
    }

    public String getMessage() {
        return message;
    }

    public int getTotalWorkersRequested() {
        return totalWorkersRequested;
    }

    public int getTotalWorkersMerged() {
        return totalWorkersMerged;
    }

    public int getFailedWorkers() {
        return failedWorkers;
    }

    public int getTotalPartialRows() {
        return totalPartialRows;
    }

    public int getTotalGlobalRows() {
        return totalGlobalRows;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public GlobalMergeCounters getCounters() {
        return counters;
    }

    public List<GlobalMergedResultRow> getRows() {
        return rows;
    }
}
