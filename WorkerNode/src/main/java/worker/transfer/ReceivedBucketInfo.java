package worker.transfer;

import java.nio.file.Path;

public final class ReceivedBucketInfo {
    private final String jobId;
    private final String bucketId;
    private final Path localPath;
    private final long totalBytes;
    private final long receivedBytes;
    private final int totalChunks;
    private final int receivedChunks;
    private final String message;

    public ReceivedBucketInfo(String jobId, String bucketId, Path localPath, long totalBytes, long receivedBytes,
                              int totalChunks, int receivedChunks, String message) {
        this.jobId = jobId;
        this.bucketId = bucketId;
        this.localPath = localPath;
        this.totalBytes = totalBytes;
        this.receivedBytes = receivedBytes;
        this.totalChunks = totalChunks;
        this.receivedChunks = receivedChunks;
        this.message = message;
    }

    public String getJobId() {
        return jobId;
    }

    public String getBucketId() {
        return bucketId;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getReceivedChunks() {
        return receivedChunks;
    }

    public String getMessage() {
        return message;
    }
}
