package worker.transfer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BucketTransferSession implements AutoCloseable {
    private final String jobId;
    private final String bucketId;
    private final String fileName;
    private final long totalBytes;
    private final int chunkSize;
    private final int totalChunks;
    private final Path localPath;
    private final OutputStream output;
    private int receivedChunks;
    private long receivedBytes;
    private boolean closed;

    public BucketTransferSession(String jobId, String bucketId, String fileName, long totalBytes, int chunkSize,
                                 int totalChunks, Path localPath) throws IOException {
        this.jobId = jobId;
        this.bucketId = bucketId;
        this.fileName = fileName;
        this.totalBytes = totalBytes;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.localPath = localPath;
        Files.createDirectories(localPath.getParent());
        Files.deleteIfExists(localPath);
        this.output = Files.newOutputStream(localPath);
    }

    public synchronized void writeChunk(int chunkIndex, byte[] data) throws IOException {
        ensureOpen();
        if (chunkIndex != receivedChunks) {
            throw new IllegalArgumentException("Expected chunk " + receivedChunks + " but received " + chunkIndex);
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Chunk payload is empty");
        }
        if (chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("Chunk index exceeds total chunks");
        }
        output.write(data);
        receivedChunks++;
        receivedBytes += data.length;
    }

    public synchronized void validateComplete() throws IOException {
        ensureOpen();
        output.flush();
        if (receivedChunks != totalChunks) {
            throw new IllegalStateException("Expected " + totalChunks + " chunks but received " + receivedChunks);
        }
        if (receivedBytes != totalBytes) {
            throw new IllegalStateException("Expected " + totalBytes + " bytes but received " + receivedBytes);
        }
    }

    public ReceivedBucketInfo info(String message) {
        return new ReceivedBucketInfo(jobId, bucketId, localPath, totalBytes, receivedBytes, totalChunks,
                receivedChunks, message);
    }

    public String getJobId() {
        return jobId;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public int getReceivedChunks() {
        return receivedChunks;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Transfer session is already closed");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            output.close();
        }
    }
}
