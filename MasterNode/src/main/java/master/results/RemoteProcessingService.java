package master.results;

import master.distribution.BucketDistributionItem;
import master.distribution.BucketDistributionStatus;
import master.distribution.DistributionResult;
import master.ice.MasterIceClient;
import master.ice.WorkerConnectionResult;
import sitm.PartialProcessingResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RemoteProcessingService {
    private final MasterIceClient iceClient;

    public RemoteProcessingService(MasterIceClient iceClient) {
        this.iceClient = iceClient;
    }

    public RemoteProcessingSummary requestPartialResults(DistributionResult distribution,
                                                         List<WorkerConnectionResult> activeWorkers,
                                                         RemoteProcessingProgress progress) {
        long started = System.currentTimeMillis();
        if (distribution == null || distribution.getJobId() == null || distribution.getJobId().trim().isEmpty()) {
            return new RemoteProcessingSummary("none", 0, 0, 0, 0, 0,
                    new ArrayList<RemoteWorkerPartialResult>());
        }

        List<WorkerConnectionResult> workers = workersThatReceivedBuckets(distribution, activeWorkers);
        notifyStarted(progress, distribution.getJobId(), workers);
        List<RemoteWorkerPartialResult> results = new ArrayList<RemoteWorkerPartialResult>();
        int success = 0;
        int failed = 0;
        int rows = 0;
        for (WorkerConnectionResult worker : workers) {
            try {
                notifyWorker(progress, worker, "Requesting remote bucket processing.");
                PartialProcessingResult dto = iceClient.processReceivedBuckets(worker, distribution.getJobId());
                RemoteWorkerPartialResult result = RemoteWorkerPartialResult.fromDto(worker.getLogicalName(), dto);
                results.add(result);
                if (result.isSuccess()) {
                    success++;
                } else {
                    failed++;
                }
                rows += result.getResultCount();
                notifyResult(progress, result);
            } catch (RuntimeException exception) {
                failed++;
                RemoteWorkerPartialResult result = RemoteWorkerPartialResult.failure(worker.getLogicalName(),
                        worker.getWorkerId(), distribution.getJobId(),
                        "Remote processing failed: " + exception.getMessage());
                results.add(result);
                notifyResult(progress, result);
            }
        }
        return new RemoteProcessingSummary(distribution.getJobId(), workers.size(), success, failed, rows,
                System.currentTimeMillis() - started, results);
    }

    private List<WorkerConnectionResult> workersThatReceivedBuckets(DistributionResult distribution,
                                                                    List<WorkerConnectionResult> activeWorkers) {
        Set<String> logicalNames = new LinkedHashSet<String>();
        for (BucketDistributionItem item : distribution.getItems()) {
            if (item.getStatus() == BucketDistributionStatus.SENT && item.getAssignedWorker() != null
                    && !item.getAssignedWorker().trim().isEmpty()) {
                logicalNames.add(item.getAssignedWorker());
            }
        }
        List<WorkerConnectionResult> workers = new ArrayList<WorkerConnectionResult>();
        for (String logicalName : logicalNames) {
            for (WorkerConnectionResult worker : activeWorkers) {
                if (logicalName.equals(worker.getLogicalName())) {
                    workers.add(worker);
                    break;
                }
            }
        }
        return workers;
    }

    private void notifyStarted(RemoteProcessingProgress progress, String jobId, List<WorkerConnectionResult> workers) {
        if (progress != null) {
            progress.onStarted(jobId, workers);
        }
    }

    private void notifyWorker(RemoteProcessingProgress progress, WorkerConnectionResult worker, String message) {
        if (progress != null) {
            progress.onWorkerRequested(worker, message);
        }
    }

    private void notifyResult(RemoteProcessingProgress progress, RemoteWorkerPartialResult result) {
        if (progress != null) {
            progress.onWorkerResult(result);
        }
    }

    public interface RemoteProcessingProgress {
        void onStarted(String jobId, List<WorkerConnectionResult> workers);

        void onWorkerRequested(WorkerConnectionResult worker, String message);

        void onWorkerResult(RemoteWorkerPartialResult result);
    }
}
