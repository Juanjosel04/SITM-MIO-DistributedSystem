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
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public final class DatagramBucketizer {
    private final DatagramCsvParser parser = new DatagramCsvParser();

    public BucketizationResult bucketize(BucketizationConfig config, BucketizationProgress progress) {
        long started = System.currentTimeMillis();
        try {
            validateInput(config);
            cleanupOldJobs(config);
            preflightDiskSpace(config, progress);
            Files.createDirectories(config.getOutputDirectory());
            notify(progress, "Creating buckets for " + config.getJobId() + ".", 0, 0);
            BucketizationStats stats = new BucketizationStats();
            MapPlaybackSampler visualSampler = new MapPlaybackSampler();
            long logEvery = readLogEvery();
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
                    if (logEvery > 0 && stats.getTotalLinesRead() % logEvery == 0) {
                        notify(progress, "Bucketization read " + stats.getTotalLinesRead() + " lines.", stats.getTotalLinesRead(),
                                stats.getValidRecordsWritten());
                    }
                }
            }

            List<BucketInfo> buckets = buildBucketInfo(bucketPaths, recordsPerBucket);
            notify(progress, "Bucketization storage: generatedBucketBytes=" + humanBytes(totalBucketBytes(buckets))
                            + "; output free space after bucketization=" + humanBytes(usableSpace(config)) + ".",
                    stats.getTotalLinesRead(), stats.getValidRecordsWritten());
            validateBucketCounts(stats, buckets);
            long elapsed = System.currentTimeMillis() - started;
            notify(progress, "Bucketization finished with " + stats.getValidRecordsWritten() + " compact records.",
                    stats.getTotalLinesRead(), stats.getValidRecordsWritten());
            return BucketizationResult.success("Buckets ready", config, stats, buckets, elapsed,
                    visualSampler.getTotalSeen(), visualSampler.getTotalKept(), visualSampler.getSampleStep(),
                    visualSampler.getMaxVisualPoints(), visualSampler.snapshot());
        } catch (RuntimeException exception) {
            cleanupPartialJob(config, progress);
            long elapsed = System.currentTimeMillis() - started;
            return BucketizationResult.failure("Bucketization failed: " + exception.getMessage(), config, elapsed);
        } catch (IOException exception) {
            cleanupPartialJob(config, progress);
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

    private void preflightDiskSpace(BucketizationConfig config, BucketizationProgress progress) throws IOException {
        Path outputRoot = config.getOutputDirectory().getParent();
        if (outputRoot == null) {
            outputRoot = config.getOutputDirectory();
        }
        Files.createDirectories(outputRoot);
        long inputSize = Files.size(config.getDatagramsPath());
        FileStore store = Files.getFileStore(outputRoot);
        long usable = store.getUsableSpace();
        long required = safeMultiply(inputSize, 2L);
        long recommended = safeMultiply(inputSize, 3L);
        notify(progress, "Bucket output directory: " + config.getOutputDirectory(), 0, 0);
        notify(progress, "Input CSV size=" + humanBytes(inputSize) + "; output free space="
                + humanBytes(usable) + "; recommended=" + humanBytes(recommended) + ".", 0, 0);
        if (usable < required) {
            throw new IOException("Espacio insuficiente para generar buckets. Archivo original: "
                    + humanBytes(inputSize) + ". Espacio libre en salida: " + humanBytes(usable)
                    + ". Salida actual: " + outputRoot
                    + ". Use -Dsitm.master.bucket.output.dir=<path> para seleccionar otro disco.");
        }
        if (usable < recommended) {
            notify(progress, "Warning: output free space is below the recommended 3x input size.", 0, 0);
        }
    }

    private void cleanupOldJobs(BucketizationConfig config) throws IOException {
        Path root = config.getOutputDirectory().getParent();
        if (root == null || !Files.exists(root)) {
            return;
        }
        final Path active = config.getOutputDirectory().toAbsolutePath().normalize();
        java.util.stream.Stream<Path> stream = Files.list(root);
        try {
            stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("job-bucketization-"))
                    .filter(path -> !path.toAbsolutePath().normalize().equals(active))
                    .forEach(this::deleteRecursivelyQuietly);
        } finally {
            stream.close();
        }
    }

    private void cleanupPartialJob(BucketizationConfig config, BucketizationProgress progress) {
        deleteRecursivelyQuietly(config.getOutputDirectory());
        notify(progress, "Partial bucketization job cleaned: " + config.getOutputDirectory(), 0, 0);
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

    private long totalBucketBytes(List<BucketInfo> buckets) {
        long total = 0L;
        for (BucketInfo bucket : buckets) {
            total += bucket.getSizeBytes();
        }
        return total;
    }

    private long usableSpace(BucketizationConfig config) throws IOException {
        Path outputRoot = config.getOutputDirectory().getParent();
        if (outputRoot == null) {
            outputRoot = config.getOutputDirectory();
        }
        return Files.getFileStore(outputRoot).getUsableSpace();
    }

    private void notify(BucketizationProgress progress, String message, long totalLinesRead, long validRecordsWritten) {
        if (progress != null) {
            progress.onProgress(message, totalLinesRead, validRecordsWritten);
        }
    }

    private long readLogEvery() {
        String value = System.getProperty("sitm.bucketization.log.every", "100000");
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed < 0L ? 100000L : parsed;
        } catch (RuntimeException exception) {
            return 100000L;
        }
    }

    private long safeMultiply(long value, long factor) {
        if (value > Long.MAX_VALUE / factor) {
            return Long.MAX_VALUE;
        }
        return value * factor;
    }

    private String humanBytes(long bytes) {
        double gib = bytes / 1024.0 / 1024.0 / 1024.0;
        return String.format(java.util.Locale.US, "%.2f GB", Double.valueOf(gib));
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
                    // Best-effort cleanup; the caller reports the original failure.
                }
            }
        } catch (IOException ignored) {
            // Best-effort cleanup; the caller reports the original failure.
        } finally {
            if (stream != null) {
                stream.close();
            }
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
