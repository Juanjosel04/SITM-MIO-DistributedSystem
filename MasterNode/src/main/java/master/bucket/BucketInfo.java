package master.bucket;

import java.nio.file.Path;

public final class BucketInfo {
    private final String bucketId;
    private final int bucketIndex;
    private final Path path;
    private final long recordsWritten;
    private final long sizeBytes;

    public BucketInfo(String bucketId, int bucketIndex, Path path, long recordsWritten, long sizeBytes) {
        this.bucketId = bucketId;
        this.bucketIndex = bucketIndex;
        this.path = path;
        this.recordsWritten = recordsWritten;
        this.sizeBytes = sizeBytes;
    }

    public String getBucketId() {
        return bucketId;
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    public Path getPath() {
        return path;
    }

    public long getRecordsWritten() {
        return recordsWritten;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
