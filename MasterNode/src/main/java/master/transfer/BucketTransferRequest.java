package master.transfer;

import java.nio.file.Path;

public final class BucketTransferRequest {
    private final String jobId;
    private final String bucketId;
    private final Path file;
    private final int chunkSizeBytes;
    private final long totalBytes;
    private final int totalChunks;

    public BucketTransferRequest(String jobId, String bucketId, Path file, int chunkSizeBytes, long totalBytes) {
        this.jobId = jobId;
        this.bucketId = bucketId;
        this.file = file;
        this.chunkSizeBytes = chunkSizeBytes;
        this.totalBytes = totalBytes;
        this.totalChunks = (int) Math.max(1, (totalBytes + chunkSizeBytes - 1) / chunkSizeBytes);
    }

    public String getJobId() {
        return jobId;
    }

    public String getBucketId() {
        return bucketId;
    }

    public Path getFile() {
        return file;
    }

    public String getFileName() {
        return file.getFileName().toString();
    }

    public int getChunkSizeBytes() {
        return chunkSizeBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getTotalChunks() {
        return totalChunks;
    }
}
