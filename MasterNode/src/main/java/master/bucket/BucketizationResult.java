package master.bucket;

import master.map.MapPlaybackPoint;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class BucketizationResult {
    private final boolean success;
    private final String message;
    private final String jobId;
    private final Path inputPath;
    private final Path outputDirectory;
    private final boolean demoDataset;
    private final long totalLinesRead;
    private final long headerSkipped;
    private final long validRecordsWritten;
    private final long invalidLines;
    private final long missingBusId;
    private final long missingRouteId;
    private final long missingTimestamp;
    private final long missingCoordinates;
    private final long missingOdometer;
    private final int bucketCount;
    private final long elapsedMillis;
    private final List<BucketInfo> buckets;
    private final long visualSampleTotalSeen;
    private final int visualSampleTotalKept;
    private final int visualSampleStep;
    private final int visualSampleMaxPoints;
    private final List<MapPlaybackPoint> visualSamplePoints;

    private BucketizationResult(boolean success, String message, String jobId, Path inputPath, Path outputDirectory,
                                boolean demoDataset, long totalLinesRead, long headerSkipped,
                                long validRecordsWritten, long invalidLines, long missingBusId,
                                long missingRouteId, long missingTimestamp, long missingCoordinates,
                                long missingOdometer, int bucketCount, long elapsedMillis, List<BucketInfo> buckets,
                                long visualSampleTotalSeen, int visualSampleTotalKept, int visualSampleStep,
                                int visualSampleMaxPoints, List<MapPlaybackPoint> visualSamplePoints) {
        this.success = success;
        this.message = message;
        this.jobId = jobId;
        this.inputPath = inputPath;
        this.outputDirectory = outputDirectory;
        this.demoDataset = demoDataset;
        this.totalLinesRead = totalLinesRead;
        this.headerSkipped = headerSkipped;
        this.validRecordsWritten = validRecordsWritten;
        this.invalidLines = invalidLines;
        this.missingBusId = missingBusId;
        this.missingRouteId = missingRouteId;
        this.missingTimestamp = missingTimestamp;
        this.missingCoordinates = missingCoordinates;
        this.missingOdometer = missingOdometer;
        this.bucketCount = bucketCount;
        this.elapsedMillis = elapsedMillis;
        this.buckets = Collections.unmodifiableList(buckets);
        this.visualSampleTotalSeen = visualSampleTotalSeen;
        this.visualSampleTotalKept = visualSampleTotalKept;
        this.visualSampleStep = visualSampleStep;
        this.visualSampleMaxPoints = visualSampleMaxPoints;
        this.visualSamplePoints = Collections.unmodifiableList(visualSamplePoints);
    }

    public static BucketizationResult success(String message, BucketizationConfig config, BucketizationStats stats,
                                              List<BucketInfo> buckets, long elapsedMillis,
                                              long visualSampleTotalSeen, int visualSampleTotalKept,
                                              int visualSampleStep, int visualSampleMaxPoints,
                                              List<MapPlaybackPoint> visualSamplePoints) {
        return new BucketizationResult(true, message, config.getJobId(), config.getDatagramsPath(),
                config.getOutputDirectory(), config.isDemoDataset(), stats.getTotalLinesRead(),
                stats.getHeaderSkipped(), stats.getValidRecordsWritten(), stats.getInvalidLines(),
                stats.getMissingBusId(), stats.getMissingRouteId(), stats.getMissingTimestamp(),
                stats.getMissingCoordinates(), stats.getMissingOdometer(), config.getBucketCount(),
                elapsedMillis, buckets, visualSampleTotalSeen, visualSampleTotalKept, visualSampleStep,
                visualSampleMaxPoints, visualSamplePoints);
    }

    public static BucketizationResult failure(String message, BucketizationConfig config, long elapsedMillis) {
        return new BucketizationResult(false, message, config.getJobId(), config.getDatagramsPath(),
                config.getOutputDirectory(), config.isDemoDataset(), 0, 0, 0, 0, 0, 0, 0,
                0, 0, config.getBucketCount(), elapsedMillis, Collections.<BucketInfo>emptyList(),
                0, 0, 1, 0, Collections.<MapPlaybackPoint>emptyList());
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

    public Path getInputPath() {
        return inputPath;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isDemoDataset() {
        return demoDataset;
    }

    public long getTotalLinesRead() {
        return totalLinesRead;
    }

    public long getHeaderSkipped() {
        return headerSkipped;
    }

    public long getValidRecordsWritten() {
        return validRecordsWritten;
    }

    public long getInvalidLines() {
        return invalidLines;
    }

    public long getMissingBusId() {
        return missingBusId;
    }

    public long getMissingRouteId() {
        return missingRouteId;
    }

    public long getMissingTimestamp() {
        return missingTimestamp;
    }

    public long getMissingCoordinates() {
        return missingCoordinates;
    }

    public long getMissingOdometer() {
        return missingOdometer;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public List<BucketInfo> getBuckets() {
        return buckets;
    }

    public long getVisualSampleTotalSeen() {
        return visualSampleTotalSeen;
    }

    public int getVisualSampleTotalKept() {
        return visualSampleTotalKept;
    }

    public int getVisualSampleStep() {
        return visualSampleStep;
    }

    public int getVisualSampleMaxPoints() {
        return visualSampleMaxPoints;
    }

    public List<MapPlaybackPoint> getVisualSamplePoints() {
        return visualSamplePoints;
    }
}
