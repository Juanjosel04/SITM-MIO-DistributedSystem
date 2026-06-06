package master.results;

import java.util.Collections;
import java.util.List;

public final class RemoteProcessingSummary {
    private final String jobId;
    private final int totalWorkersRequested;
    private final int successfulWorkers;
    private final int failedWorkers;
    private final int totalPartialResults;
    private final long elapsedMillis;
    private final List<RemoteWorkerPartialResult> workerResults;

    public RemoteProcessingSummary(String jobId, int totalWorkersRequested, int successfulWorkers, int failedWorkers,
                                   int totalPartialResults, long elapsedMillis,
                                   List<RemoteWorkerPartialResult> workerResults) {
        this.jobId = jobId;
        this.totalWorkersRequested = totalWorkersRequested;
        this.successfulWorkers = successfulWorkers;
        this.failedWorkers = failedWorkers;
        this.totalPartialResults = totalPartialResults;
        this.elapsedMillis = elapsedMillis;
        this.workerResults = Collections.unmodifiableList(workerResults);
    }

    public String getJobId() {
        return jobId;
    }

    public int getTotalWorkersRequested() {
        return totalWorkersRequested;
    }

    public int getSuccessfulWorkers() {
        return successfulWorkers;
    }

    public int getFailedWorkers() {
        return failedWorkers;
    }

    public int getTotalPartialResults() {
        return totalPartialResults;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public List<RemoteWorkerPartialResult> getWorkerResults() {
        return workerResults;
    }
}
