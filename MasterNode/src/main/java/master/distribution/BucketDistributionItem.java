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

    public BucketDistributionItem(String bucketId, Path path, long sizeBytes) {
        this.bucketId = bucketId;
        this.path = path;
        this.sizeBytes = sizeBytes;
        this.status = BucketDistributionStatus.PENDING;
        this.assignedWorker = "";
        this.message = "Queued";
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
}
