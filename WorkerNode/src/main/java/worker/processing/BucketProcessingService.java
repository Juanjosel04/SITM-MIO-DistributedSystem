package worker.processing;

import worker.processing.forkjoin.BucketProcessingTask;
import worker.runtime.WorkerRuntimePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public final class BucketProcessingService {
    private final WorkerRuntimePaths runtimePaths;

    public BucketProcessingService(WorkerRuntimePaths runtimePaths) {
        this.runtimePaths = runtimePaths;
    }

    public PartialSpeedResult processLocalBuckets() {
        long started = System.currentTimeMillis();
        try {
            runtimePaths.ensureDirectories();
            List<Path> buckets = locateReceivedBuckets();
            if (buckets.isEmpty()) {
                buckets.add(new DemoBucketFileFactory(runtimePaths).createDemoBucket());
            }
            return processBuckets(buckets, started);
        } catch (IOException exception) {
            PartialSpeedResult failure = PartialSpeedResult.failure("Bucket processing failed: " + exception.getMessage());
            failure.setElapsedMillis(System.currentTimeMillis() - started);
            return failure;
        }
    }

    public PartialSpeedResult processReceivedBuckets(String jobId) {
        long started = System.currentTimeMillis();
        try {
            runtimePaths.ensureDirectoriesWithoutCleanup();
            Path directory = receivedJobDirectory(jobId);
            boolean directoryExists = directory != null && Files.exists(directory);
            List<Path> buckets = locateReceivedBuckets(jobId);
            if (buckets.isEmpty()) {
                PartialSpeedResult failure = PartialSpeedResult.failure("No received buckets found for jobId="
                        + safeJobId(jobId) + " at path=" + directory + "; exists=" + directoryExists
                        + "; csvFiles=0");
                failure.setElapsedMillis(System.currentTimeMillis() - started);
                return failure;
            }
            PartialSpeedResult result = processBuckets(buckets, started);
            if (result.isSuccess()) {
                result.setMessage("Worker processing jobId=" + safeJobId(jobId)
                        + "; receivedBucketDir=" + directory
                        + "; exists=" + directoryExists
                        + "; csvFilesFound=" + buckets.size()
                        + "; processedBuckets=" + result.getProcessedBuckets().size()
                        + "; partialRows=" + result.getAccumulators().size()
                        + "; validIntervals=" + result.getCounters().getValidIntervals()
                        + "; cleanupAfterProcessing=" + cleanupJobAfterProcessing());
            }
            return result;
        } catch (IOException exception) {
            PartialSpeedResult failure = PartialSpeedResult.failure("Bucket processing failed for jobId="
                    + safeJobId(jobId) + " at path=" + receivedJobDirectory(jobId) + ": " + exception.getMessage());
            failure.setElapsedMillis(System.currentTimeMillis() - started);
            return failure;
        }
    }

    public PartialSpeedResult processBuckets(List<Path> buckets) {
        return processBuckets(buckets, System.currentTimeMillis());
    }

    private PartialSpeedResult processBuckets(List<Path> buckets, long started) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            PartialSpeedResult result = pool.invoke(new BucketProcessingTask(buckets));
            result.setElapsedMillis(System.currentTimeMillis() - started);
            return result;
        } finally {
            pool.shutdown();
        }
    }

    private List<Path> locateReceivedBuckets() throws IOException {
        Path directory = runtimePaths.receivedBucketsDirectory();
        if (!Files.exists(directory)) {
            return new ArrayList<Path>();
        }
        List<Path> buckets = new ArrayList<Path>();
        java.util.stream.Stream<Path> stream = Files.walk(directory);
        try {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .forEach(buckets::add);
        } finally {
            stream.close();
        }
        Collections.sort(buckets);
        return buckets;
    }

    private List<Path> locateReceivedBuckets(String jobId) throws IOException {
        if (jobId == null || jobId.trim().isEmpty()) {
            return new ArrayList<Path>();
        }
        Path directory = runtimePaths.receivedBucketsDirectory().resolve(jobId.trim());
        if (!Files.exists(directory)) {
            return new ArrayList<Path>();
        }
        List<Path> buckets = new ArrayList<Path>();
        java.util.stream.Stream<Path> stream = Files.walk(directory);
        try {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .forEach(buckets::add);
        } finally {
            stream.close();
        }
        Collections.sort(buckets);
        return buckets;
    }

    public void cleanupReceivedJobAfterProcessing(String jobId, PartialSpeedResult result) {
        if (result == null || !result.isSuccess() || !cleanupJobAfterProcessing()) {
            return;
        }
        deleteRecursivelyQuietly(receivedJobDirectory(jobId));
    }

    private boolean cleanupJobAfterProcessing() {
        return Boolean.parseBoolean(System.getProperty("sitm.worker.cleanup.job.after.processing", "false"));
    }

    private Path receivedJobDirectory(String jobId) {
        return runtimePaths.receivedBucketsDirectory().resolve(safeJobId(jobId));
    }

    private String safeJobId(String jobId) {
        return jobId == null || jobId.trim().isEmpty() ? "unknown" : jobId.trim();
    }

    private void deleteRecursivelyQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        java.util.stream.Stream<Path> stream = null;
        try {
            stream = Files.walk(directory);
            List<Path> paths = new ArrayList<Path>();
            stream.forEach(paths::add);
            paths.sort(new Comparator<Path>() {
                @Override
                public int compare(Path left, Path right) {
                    return right.getNameCount() - left.getNameCount();
                }
            });
            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Optional cleanup must not invalidate a successful worker result.
                }
            }
        } catch (IOException ignored) {
            // Optional cleanup must not invalidate a successful worker result.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
