package master.transfer;

import master.ice.WorkerConnectionResult;

import java.nio.file.Path;

public final class BucketTransferResult {
    private final boolean success;
    private final WorkerConnectionResult worker;
    private final String jobId;
    private final String bucketId;
    private final Path sourcePath;
    private final String remotePath;
    private final long totalBytes;
    private final int totalChunks;
    private final String message;

    private BucketTransferResult(boolean success, WorkerConnectionResult worker, String jobId, String bucketId,
                                 Path sourcePath, String remotePath, long totalBytes, int totalChunks, String message) {
        this.success = success;
        this.worker = worker;
        this.jobId = jobId;
        this.bucketId = bucketId;
        this.sourcePath = sourcePath;
        this.remotePath = remotePath;
        this.totalBytes = totalBytes;
        this.totalChunks = totalChunks;
        this.message = message;
    }

    public static BucketTransferResult success(WorkerConnectionResult worker, BucketTransferRequest request,
                                               String remotePath, String message) {
        return new BucketTransferResult(true, worker, request.getJobId(), request.getBucketId(), request.getFile(),
                remotePath, request.getTotalBytes(), request.getTotalChunks(), message);
    }

    public static BucketTransferResult failure(WorkerConnectionResult worker, BucketTransferRequest request,
                                               String message) {
        Path sourcePath = request == null ? null : request.getFile();
        String jobId = request == null ? "none" : request.getJobId();
        String bucketId = request == null ? "none" : request.getBucketId();
        long totalBytes = request == null ? 0 : request.getTotalBytes();
        int totalChunks = request == null ? 0 : request.getTotalChunks();
        return new BucketTransferResult(false, worker, jobId, bucketId, sourcePath, "", totalBytes, totalChunks, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public WorkerConnectionResult getWorker() {
        return worker;
    }

    public String getJobId() {
        return jobId;
    }

    public String getBucketId() {
        return bucketId;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public String getMessage() {
        return message;
    }
}
