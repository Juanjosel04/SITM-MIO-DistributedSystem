package master.distribution;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DistributionResult {
    private final boolean success;
    private final String message;
    private final String jobId;
    private final int totalBuckets;
    private final int sentBuckets;
    private final int failedBuckets;
    private final int workersUsed;
    private final long elapsedMillis;
    private final List<BucketDistributionItem> items;
    private final int masterBucketsDeleted;
    private final long masterBytesDeleted;
    private final int masterBucketsRetained;
    private final int failedDeletes;

    public DistributionResult(boolean success, String message, String jobId, int totalBuckets, int sentBuckets,
                              int failedBuckets, int workersUsed, long elapsedMillis,
                              List<BucketDistributionItem> items) {
        this(success, message, jobId, totalBuckets, sentBuckets, failedBuckets, workersUsed, elapsedMillis,
                items, countDeleted(items), sumDeletedBytes(items), countRetained(items), countFailedDeletes(items));
    }

    public DistributionResult(boolean success, String message, String jobId, int totalBuckets, int sentBuckets,
                              int failedBuckets, int workersUsed, long elapsedMillis,
                              List<BucketDistributionItem> items, int masterBucketsDeleted, long masterBytesDeleted,
                              int masterBucketsRetained, int failedDeletes) {
        this.success = success;
        this.message = message;
        this.jobId = jobId;
        this.totalBuckets = totalBuckets;
        this.sentBuckets = sentBuckets;
        this.failedBuckets = failedBuckets;
        this.workersUsed = workersUsed;
        this.elapsedMillis = elapsedMillis;
        this.items = Collections.unmodifiableList(items);
        this.masterBucketsDeleted = masterBucketsDeleted;
        this.masterBytesDeleted = masterBytesDeleted;
        this.masterBucketsRetained = masterBucketsRetained;
        this.failedDeletes = failedDeletes;
    }

    public static DistributionResult from(String jobId, String message, long elapsedMillis,
                                          List<BucketDistributionItem> items) {
        int sent = 0;
        int failed = 0;
        Set<String> workers = new HashSet<String>();
        for (BucketDistributionItem item : items) {
            if (item.getStatus() == BucketDistributionStatus.SENT) {
                sent++;
                if (item.getAssignedWorker() != null && !item.getAssignedWorker().trim().isEmpty()) {
                    workers.add(item.getAssignedWorker());
                }
            } else if (item.getStatus() == BucketDistributionStatus.FAILED) {
                failed++;
            }
        }
        return new DistributionResult(failed == 0 && sent > 0, message, jobId, items.size(), sent, failed,
                workers.size(), elapsedMillis, items);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getJobId() {
        return jobId;
    }

    public int getTotalBuckets() {
        return totalBuckets;
    }

    public int getSentBuckets() {
        return sentBuckets;
    }

    public int getFailedBuckets() {
        return failedBuckets;
    }

    public int getWorkersUsed() {
        return workersUsed;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public List<BucketDistributionItem> getItems() {
        return items;
    }

    public int getMasterBucketsDeleted() {
        return masterBucketsDeleted;
    }

    public long getMasterBytesDeleted() {
        return masterBytesDeleted;
    }

    public int getMasterBucketsRetained() {
        return masterBucketsRetained;
    }

    public int getFailedDeletes() {
        return failedDeletes;
    }

    private static int countDeleted(List<BucketDistributionItem> items) {
        int count = 0;
        for (BucketDistributionItem item : items) {
            if (item.isMasterBucketDeleted()) {
                count++;
            }
        }
        return count;
    }

    private static long sumDeletedBytes(List<BucketDistributionItem> items) {
        long total = 0L;
        for (BucketDistributionItem item : items) {
            total += item.getMasterBytesDeleted();
        }
        return total;
    }

    private static int countRetained(List<BucketDistributionItem> items) {
        int count = 0;
        for (BucketDistributionItem item : items) {
            if (!item.isMasterBucketDeleted()) {
                count++;
            }
        }
        return count;
    }

    private static int countFailedDeletes(List<BucketDistributionItem> items) {
        int count = 0;
        for (BucketDistributionItem item : items) {
            String message = item.getMasterDeleteMessage();
            if (message != null && message.startsWith("Delete failed")) {
                count++;
            }
        }
        return count;
    }
}
