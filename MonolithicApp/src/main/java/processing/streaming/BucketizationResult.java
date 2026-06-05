package processing.streaming;

import domain.Datagram;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class BucketizationResult {
    private final Path tempDirectory;
    private final List<Path> bucketFiles;
    private final long readDatagrams;
    private final long bucketizedDatagrams;
    private final long invalidLines;
    private final long maxBucketApproxLines;
    private final long bucketizationTimeMillis;
    private final List<Datagram> visualSample;

    public BucketizationResult(
            Path tempDirectory,
            List<Path> bucketFiles,
            long readDatagrams,
            long bucketizedDatagrams,
            long invalidLines,
            long maxBucketApproxLines,
            long bucketizationTimeMillis,
            List<Datagram> visualSample
    ) {
        this.tempDirectory = tempDirectory;
        this.bucketFiles = bucketFiles == null ? Collections.<Path>emptyList() : Collections.unmodifiableList(bucketFiles);
        this.readDatagrams = readDatagrams;
        this.bucketizedDatagrams = bucketizedDatagrams;
        this.invalidLines = invalidLines;
        this.maxBucketApproxLines = maxBucketApproxLines;
        this.bucketizationTimeMillis = bucketizationTimeMillis;
        this.visualSample = visualSample == null ? Collections.<Datagram>emptyList() : Collections.unmodifiableList(visualSample);
    }

    public Path getTempDirectory() {
        return tempDirectory;
    }

    public List<Path> getBucketFiles() {
        return bucketFiles;
    }

    public long getReadDatagrams() {
        return readDatagrams;
    }

    public long getBucketizedDatagrams() {
        return bucketizedDatagrams;
    }

    public long getInvalidLines() {
        return invalidLines;
    }

    public long getMaxBucketApproxLines() {
        return maxBucketApproxLines;
    }

    public long getBucketizationTimeMillis() {
        return bucketizationTimeMillis;
    }

    public List<Datagram> getVisualSample() {
        return visualSample;
    }
}
