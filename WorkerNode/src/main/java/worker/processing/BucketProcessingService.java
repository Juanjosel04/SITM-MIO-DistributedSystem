package worker.processing;

import worker.processing.forkjoin.BucketProcessingTask;
import worker.runtime.WorkerRuntimePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
            runtimePaths.ensureDirectories();
            List<Path> buckets = locateReceivedBuckets(jobId);
            if (buckets.isEmpty()) {
                PartialSpeedResult failure = PartialSpeedResult.failure("No received buckets found for jobId " + jobId);
                failure.setElapsedMillis(System.currentTimeMillis() - started);
                return failure;
            }
            return processBuckets(buckets, started);
        } catch (IOException exception) {
            PartialSpeedResult failure = PartialSpeedResult.failure("Bucket processing failed: " + exception.getMessage());
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
}
