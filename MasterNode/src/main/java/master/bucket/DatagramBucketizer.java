package master.bucket;

import master.ingestion.CompactDatagramRecord;
import master.ingestion.DatagramCsvParser;
import master.ingestion.DatagramParseResult;
import master.map.MapPlaybackSampler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DatagramBucketizer {
    private final DatagramCsvParser parser = new DatagramCsvParser();

    public BucketizationResult bucketize(BucketizationConfig config, BucketizationProgress progress) {
        long started = System.currentTimeMillis();
        try {
            validateInput(config);
            Files.createDirectories(config.getOutputDirectory());
            notify(progress, "Creating buckets for " + config.getJobId() + ".", 0, 0);
            BucketizationStats stats = new BucketizationStats();
            MapPlaybackSampler visualSampler = new MapPlaybackSampler();
            long[] recordsPerBucket = new long[config.getBucketCount()];
            Path[] bucketPaths = createBucketPaths(config);

            try (WriterGroup writers = WriterGroup.open(bucketPaths);
                 BufferedReader reader = Files.newBufferedReader(config.getDatagramsPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stats.countLine();
                    DatagramParseResult parseResult = parser.parse(line);
                    if (parseResult.isValid()) {
                        CompactDatagramRecord record = parseResult.getRecord();
                        int bucketIndex = Math.floorMod(record.getBusId().hashCode(), config.getBucketCount());
                        writers.write(bucketIndex, record.toCompactCsvLine());
                        recordsPerBucket[bucketIndex]++;
                        stats.countValid();
                        visualSampler.offer(record, stats.getValidRecordsWritten());
                    } else {
                        stats.countInvalid(parseResult.getReason());
                    }
                    if (stats.getTotalLinesRead() % 1000 == 0) {
                        notify(progress, "Bucketization read " + stats.getTotalLinesRead() + " lines.", stats.getTotalLinesRead(),
                                stats.getValidRecordsWritten());
                    }
                }
            }

            List<BucketInfo> buckets = buildBucketInfo(bucketPaths, recordsPerBucket);
            validateBucketCounts(stats, buckets);
            long elapsed = System.currentTimeMillis() - started;
            notify(progress, "Bucketization finished with " + stats.getValidRecordsWritten() + " compact records.",
                    stats.getTotalLinesRead(), stats.getValidRecordsWritten());
            return BucketizationResult.success("Buckets ready", config, stats, buckets, elapsed,
                    visualSampler.getTotalSeen(), visualSampler.getTotalKept(), visualSampler.getSampleStep(),
                    visualSampler.getMaxVisualPoints(), visualSampler.snapshot());
        } catch (RuntimeException exception) {
            long elapsed = System.currentTimeMillis() - started;
            return BucketizationResult.failure("Bucketization failed: " + exception.getMessage(), config, elapsed);
        } catch (IOException exception) {
            long elapsed = System.currentTimeMillis() - started;
            return BucketizationResult.failure("Bucketization failed: " + exception.getMessage(), config, elapsed);
        }
    }

    private void validateInput(BucketizationConfig config) {
        if (!Files.exists(config.getDatagramsPath())) {
            throw new IllegalArgumentException("Input CSV does not exist: " + config.getDatagramsPath());
        }
        if (config.getBucketCount() <= 0) {
            throw new IllegalArgumentException("bucketCount must be positive");
        }
    }

    private Path[] createBucketPaths(BucketizationConfig config) {
        Path[] paths = new Path[config.getBucketCount()];
        for (int index = 0; index < paths.length; index++) {
            paths[index] = config.getOutputDirectory().resolve(String.format("bucket-%03d.csv", index));
        }
        return paths;
    }

    private List<BucketInfo> buildBucketInfo(Path[] bucketPaths, long[] recordsPerBucket) throws IOException {
        List<BucketInfo> buckets = new ArrayList<BucketInfo>();
        for (int index = 0; index < bucketPaths.length; index++) {
            buckets.add(new BucketInfo(String.format("bucket-%03d", index), index, bucketPaths[index],
                    recordsPerBucket[index], Files.size(bucketPaths[index])));
        }
        return buckets;
    }

    private void validateBucketCounts(BucketizationStats stats, List<BucketInfo> buckets) {
        long sum = 0;
        for (BucketInfo bucket : buckets) {
            sum += bucket.getRecordsWritten();
        }
        if (sum != stats.getValidRecordsWritten()) {
            throw new IllegalStateException("Bucket counts do not match valid records");
        }
    }

    private void notify(BucketizationProgress progress, String message, long totalLinesRead, long validRecordsWritten) {
        if (progress != null) {
            progress.onProgress(message, totalLinesRead, validRecordsWritten);
        }
    }

    public interface BucketizationProgress {
        void onProgress(String message, long totalLinesRead, long validRecordsWritten);
    }

    private static final class WriterGroup implements AutoCloseable {
        private final List<BufferedWriter> writers;

        private WriterGroup(List<BufferedWriter> writers) {
            this.writers = writers;
        }

        private static WriterGroup open(Path[] paths) throws IOException {
            List<BufferedWriter> writers = new ArrayList<BufferedWriter>();
            try {
                for (Path path : paths) {
                    BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
                    writer.write(CompactDatagramRecord.HEADER);
                    writer.newLine();
                    writers.add(writer);
                }
                return new WriterGroup(writers);
            } catch (IOException exception) {
                for (BufferedWriter writer : writers) {
                    try {
                        writer.close();
                    } catch (IOException ignored) {
                        // The original exception explains the open failure.
                    }
                }
                throw exception;
            }
        }

        private void write(int bucketIndex, String line) throws IOException {
            BufferedWriter writer = writers.get(bucketIndex);
            writer.write(line);
            writer.newLine();
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (BufferedWriter writer : writers) {
                try {
                    writer.close();
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
