package master.distribution;

import java.nio.file.Path;

public final class BucketDistributionItem {
    private final String bucketId;
    private final Path path;
    private final long sizeBytes;
    private BucketDistributionStatus status;
    private String assignedWorker;
    private int attempts;
    private String message;
    private boolean masterBucketDeleted;
    private long masterBytesDeleted;
    private String masterDeleteMessage;

    public BucketDistributionItem(String bucketId, Path path, long sizeBytes) {
        this.bucketId = bucketId;
        this.path = path;
        this.sizeBytes = sizeBytes;
        this.status = BucketDistributionStatus.PENDING;
        this.assignedWorker = "";
        this.message = "Queued";
        this.masterDeleteMessage = "";
    }

    public String getBucketId() {
        return bucketId;
    }

    public Path getPath() {
        return path;
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public BucketDistributionStatus getStatus() {
        return status;
    }

    public void setStatus(BucketDistributionStatus status) {
        this.status = status;
    }

    public String getAssignedWorker() {
        return assignedWorker;
    }

    public void setAssignedWorker(String assignedWorker) {
        this.assignedWorker = assignedWorker;
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isMasterBucketDeleted() {
        return masterBucketDeleted;
    }

    public long getMasterBytesDeleted() {
        return masterBytesDeleted;
    }

    public String getMasterDeleteMessage() {
        return masterDeleteMessage;
    }

    public void markMasterBucketDeleted(long bytesDeleted) {
        this.masterBucketDeleted = true;
        this.masterBytesDeleted = Math.max(0L, bytesDeleted);
        this.masterDeleteMessage = "Deleted from Master";
    }

    public void markMasterBucketRetained(String reason) {
        this.masterBucketDeleted = false;
        this.masterBytesDeleted = 0L;
        this.masterDeleteMessage = reason == null ? "" : reason;
    }
}
