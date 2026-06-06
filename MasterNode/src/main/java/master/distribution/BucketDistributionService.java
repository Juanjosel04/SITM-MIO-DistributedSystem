package master.distribution;

import master.ice.MasterIceClient;
import master.ice.WorkerConnectionResult;
import master.transfer.BucketTransferRequest;
import master.transfer.BucketTransferResult;
import master.transfer.TestBucketFileFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public final class BucketDistributionService {
    private static final DateTimeFormatter JOB_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_RETRIES = 1;

    private final MasterIceClient iceClient;

    public BucketDistributionService(MasterIceClient iceClient) {
        this.iceClient = iceClient;
    }

    public DistributionResult distribute(List<Path> bucketFiles, List<WorkerConnectionResult> activeWorkers,
                                         DistributionProgress progress) {
        String jobId = "job-distribution-" + LocalDateTime.now().format(JOB_FORMAT);
        long started = System.currentTimeMillis();
        List<BucketDistributionItem> items = createItems(bucketFiles);
        notifyStarted(progress, jobId, items, activeWorkers);

        if (items.isEmpty()) {
            return new DistributionResult(false, "No generated buckets found. Run Generate Buckets first.", jobId,
                    0, 0, 0, 0, System.currentTimeMillis() - started, items);
        }
        if (activeWorkers.isEmpty()) {
            markAllFailed(items, "No active workers available for bucket distribution.");
            return DistributionResult.from(jobId, "No active workers available for bucket distribution.",
                    System.currentTimeMillis() - started, items);
        }

        Queue<BucketDistributionItem> queue = new LinkedList<BucketDistributionItem>(items);
        int workerIndex = 0;
        while (!queue.isEmpty()) {
            BucketDistributionItem item = queue.poll();
            if (!validateItem(item)) {
                notifyItem(progress, item);
                continue;
            }

            WorkerConnectionResult worker = activeWorkers.get(workerIndex % activeWorkers.size());
            workerIndex++;
            assign(item, worker);
            notifyItem(progress, item);

            item.setStatus(BucketDistributionStatus.TRANSFERRING);
            item.setMessage("Transferring " + item.getFileName() + " to " + worker.getLogicalName());
            notifyItem(progress, item);

            BucketTransferResult transfer = transfer(jobId, item, worker, progress);
            if (transfer.isSuccess()) {
                cleanupMasterBucketAfterSuccess(item);
                item.setStatus(BucketDistributionStatus.SENT);
                item.setMessage("SENT to " + worker.getLogicalName() + storageSuffix(item));
                notifyItem(progress, item);
            } else if (item.getAttempts() <= MAX_RETRIES && activeWorkers.size() > 1) {
                item.setStatus(BucketDistributionStatus.PENDING);
                item.setMessage("Retry queued after failure on " + worker.getLogicalName() + ": " + transfer.getMessage());
                notifyItem(progress, item);
                queue.add(item);
            } else {
                cleanupFailedBucketIfConfigured(item);
                item.setStatus(BucketDistributionStatus.FAILED);
                item.setMessage(transfer.getMessage() + storageSuffix(item));
                notifyItem(progress, item);
            }
        }

        DistributionResult result = DistributionResult.from(jobId,
                "Distribution completed", System.currentTimeMillis() - started, items);
        notifyFinished(progress, result);
        return result;
    }

    private List<BucketDistributionItem> createItems(List<Path> bucketFiles) {
        List<BucketDistributionItem> items = new ArrayList<BucketDistributionItem>();
        for (Path path : bucketFiles) {
            try {
                long size = Files.exists(path) ? Files.size(path) : 0;
                items.add(new BucketDistributionItem(bucketId(path), path, size));
            } catch (IOException exception) {
                BucketDistributionItem item = new BucketDistributionItem(bucketId(path), path, 0);
                item.setStatus(BucketDistributionStatus.FAILED);
                item.setMessage("Could not inspect bucket: " + exception.getMessage());
                items.add(item);
            }
        }
        return items;
    }

    private boolean validateItem(BucketDistributionItem item) {
        if (!Files.exists(item.getPath())) {
            item.setStatus(BucketDistributionStatus.FAILED);
            item.setMessage("Bucket file does not exist");
            return false;
        }
        if (!Files.isRegularFile(item.getPath())) {
            item.setStatus(BucketDistributionStatus.FAILED);
            item.setMessage("Bucket path is not a regular file");
            return false;
        }
        if (!item.getFileName().toLowerCase().endsWith(".csv")) {
            item.setStatus(BucketDistributionStatus.FAILED);
            item.setMessage("Bucket file is not CSV");
            return false;
        }
        if (item.getSizeBytes() <= 0) {
            item.setStatus(BucketDistributionStatus.FAILED);
            item.setMessage("Bucket file is empty");
            return false;
        }
        return true;
    }

    private void assign(BucketDistributionItem item, WorkerConnectionResult worker) {
        item.incrementAttempts();
        item.setAssignedWorker(worker.getLogicalName());
        item.setStatus(BucketDistributionStatus.ASSIGNED);
        item.setMessage("Assigned to " + worker.getLogicalName());
    }

    private BucketTransferResult transfer(String jobId, BucketDistributionItem item, WorkerConnectionResult worker,
                                          DistributionProgress progress) {
        BucketTransferRequest request = new BucketTransferRequest(
                jobId,
                item.getBucketId(),
                item.getPath(),
                TestBucketFileFactory.DEFAULT_CHUNK_SIZE_BYTES,
                item.getSizeBytes()
        );
        return iceClient.transferBucket(worker, request, (message, sentChunks, totalChunks, totalBytes) -> {
            if (progress != null) {
                progress.onTransferProgress(item, worker, message, sentChunks, totalChunks, totalBytes);
            }
        });
    }

    private void cleanupMasterBucketAfterSuccess(BucketDistributionItem item) {
        if (!deleteBucketAfterTransfer()) {
            item.markMasterBucketRetained("Retained by sitm.master.delete.bucket.after.transfer=false");
            return;
        }
        deleteBucketFile(item, "Deleted after successful transfer");
    }

    private void cleanupFailedBucketIfConfigured(BucketDistributionItem item) {
        if (keepFailedBuckets()) {
            item.markMasterBucketRetained("Retained failed bucket for diagnostics");
            return;
        }
        deleteBucketFile(item, "Deleted failed bucket by sitm.master.keep.failed.buckets=false");
    }

    private void deleteBucketFile(BucketDistributionItem item, String successMessage) {
        try {
            long size = Files.exists(item.getPath()) ? Files.size(item.getPath()) : item.getSizeBytes();
            if (Files.deleteIfExists(item.getPath())) {
                item.markMasterBucketDeleted(size);
                item.setMessage(item.getMessage() + ". " + successMessage + " (" + size + " bytes)");
            } else {
                item.markMasterBucketRetained("Bucket file already absent on Master");
                item.setMessage(item.getMessage() + ". Bucket file already absent on Master");
            }
        } catch (IOException exception) {
            item.markMasterBucketRetained("Delete failed: " + exception.getMessage());
            item.setMessage(item.getMessage() + ". Warning: could not delete Master bucket: "
                    + exception.getMessage());
        }
    }

    private boolean deleteBucketAfterTransfer() {
        return Boolean.parseBoolean(System.getProperty("sitm.master.delete.bucket.after.transfer", "true"));
    }

    private boolean keepFailedBuckets() {
        return Boolean.parseBoolean(System.getProperty("sitm.master.keep.failed.buckets", "true"));
    }

    private String storageSuffix(BucketDistributionItem item) {
        String message = item.getMasterDeleteMessage();
        return message == null || message.trim().isEmpty() ? "" : ". " + message;
    }

    private void markAllFailed(List<BucketDistributionItem> items, String message) {
        for (BucketDistributionItem item : items) {
            item.setStatus(BucketDistributionStatus.FAILED);
            item.setMessage(message);
        }
    }

    private String bucketId(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private void notifyStarted(DistributionProgress progress, String jobId, List<BucketDistributionItem> items,
                               List<WorkerConnectionResult> workers) {
        if (progress != null) {
            progress.onStarted(jobId, items, workers);
        }
    }

    private void notifyItem(DistributionProgress progress, BucketDistributionItem item) {
        if (progress != null) {
            progress.onItemUpdated(item);
        }
    }

    private void notifyFinished(DistributionProgress progress, DistributionResult result) {
        if (progress != null) {
            progress.onFinished(result);
        }
    }

    public interface DistributionProgress {
        void onStarted(String jobId, List<BucketDistributionItem> items, List<WorkerConnectionResult> workers);

        void onItemUpdated(BucketDistributionItem item);

        void onTransferProgress(BucketDistributionItem item, WorkerConnectionResult worker, String message,
                                int sentChunks, int totalChunks, long totalBytes);

        void onFinished(DistributionResult result);
    }
}
