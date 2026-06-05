package processing.streaming;

import domain.Datagram;
import ingestion.CsvReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class StreamingDatagramBucketizer {
    private static final Charset DATASET_CHARSET = Charset.forName("UTF-8");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double COORDINATE_SCALE = 10000000.0;

    private final CsvReader csvReader = new CsvReader();

    public BucketizationResult bucketize(String datagramPath, BucketProcessingConfig config) throws IOException {
        long startNanos = System.nanoTime();
        Path source = csvReader.resolvePath(datagramPath);
        Path tempDirectory = Files.createTempDirectory("sitm-v2-buckets-");
        List<Path> bucketFiles = createBucketFiles(tempDirectory, config.getBucketCount());
        long[] bucketLineCounts = new long[config.getBucketCount()];
        AdaptiveVisualDatagramSampler visualSampler = new AdaptiveVisualDatagramSampler(config.getVisualSampleLimit());
        BufferedWriter[] writers = openWriters(bucketFiles);

        long readDatagrams = 0L;
        long bucketizedDatagrams = 0L;
        long invalidLines = 0L;

        BufferedReader reader = Files.newBufferedReader(source, DATASET_CHARSET);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                readDatagrams++;
                try {
                    BucketedDatagramRecord record = parseLine(line);
                    int bucket = bucketIndex(record.getBusId(), config.getBucketCount());
                    writers[bucket].write(record.toCompactLine());
                    writers[bucket].newLine();
                    bucketLineCounts[bucket]++;
                    bucketizedDatagrams++;
                    visualSampler.offer(record);
                } catch (RuntimeException exception) {
                    invalidLines++;
                }
            }
        } finally {
            reader.close();
            closeWriters(writers);
        }

        return new BucketizationResult(
                tempDirectory,
                bucketFiles,
                readDatagrams,
                bucketizedDatagrams,
                invalidLines,
                max(bucketLineCounts),
                nanosToMillis(System.nanoTime() - startNanos),
                visualSampler.snapshot()
        );
    }

    private BucketedDatagramRecord parseLine(String line) {
        List<String> values = CsvReader.split(line);
        if (values.size() < 12) {
            throw new IllegalArgumentException("Datagram row has fewer columns than expected");
        }

        double odometer = Double.parseDouble(value(values, 3));
        Double latitude = parseCoordinate(value(values, 4));
        Double longitude = parseCoordinate(value(values, 5));
        int routeId = Integer.parseInt(value(values, 7));
        LocalDateTime timestamp = parseTimestamp(value(values, 10));
        String busId = value(values, 11);
        if (busId.isEmpty()) {
            throw new IllegalArgumentException("Missing busId");
        }

        return new BucketedDatagramRecord(busId, routeId, odometer, timestamp, latitude, longitude);
    }

    private int bucketIndex(String busId, int bucketCount) {
        return Math.floorMod(busId.hashCode(), bucketCount);
    }

    private List<Path> createBucketFiles(Path tempDirectory, int bucketCount) throws IOException {
        List<Path> files = new ArrayList<Path>();
        for (int i = 0; i < bucketCount; i++) {
            Path file = tempDirectory.resolve("bucket-" + i + ".csv");
            Files.createFile(file);
            files.add(file);
        }
        return files;
    }

    private BufferedWriter[] openWriters(List<Path> bucketFiles) throws IOException {
        BufferedWriter[] writers = new BufferedWriter[bucketFiles.size()];
        for (int i = 0; i < bucketFiles.size(); i++) {
            writers[i] = Files.newBufferedWriter(bucketFiles.get(i), DATASET_CHARSET);
        }
        return writers;
    }

    private void closeWriters(BufferedWriter[] writers) throws IOException {
        IOException failure = null;
        for (BufferedWriter writer : writers) {
            try {
                writer.close();
            } catch (IOException exception) {
                failure = exception;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private String value(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return CsvReader.clean(values.get(index));
    }

    private Double parseCoordinate(String value) {
        if (value == null || value.trim().isEmpty() || "-1".equals(value.trim())) {
            return null;
        }
        double coordinate = Double.parseDouble(value);
        if (Math.abs(coordinate) > 180.0) {
            return Double.valueOf(coordinate / COORDINATE_SCALE);
        }
        return Double.valueOf(coordinate);
    }

    private LocalDateTime parseTimestamp(String value) {
        try {
            return LocalDateTime.parse(value, TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid timestamp", exception);
        }
    }

    private long max(long[] values) {
        long max = 0L;
        for (long value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private static final class AdaptiveVisualDatagramSampler {
        private static final int INITIAL_SAMPLE_STEP = 10;

        private final int maxSampleSize;
        private final List<Datagram> sample = new ArrayList<Datagram>();
        private final java.util.Map<String, Long> candidateCountByBusId = new java.util.HashMap<String, Long>();
        private int sampleStep = INITIAL_SAMPLE_STEP;

        private AdaptiveVisualDatagramSampler(int maxSampleSize) {
            this.maxSampleSize = Math.max(5000, maxSampleSize);
        }

        private void offer(BucketedDatagramRecord record) {
            if (record == null || record.getBusId().isEmpty()) {
                return;
            }
            if (record.getRouteId() == -1) {
                return;
            }
            if (record.getLatitude() == null || record.getLongitude() == null) {
                return;
            }
            Long currentCount = candidateCountByBusId.get(record.getBusId());
            long nextCount = currentCount == null ? 1L : currentCount.longValue() + 1L;
            candidateCountByBusId.put(record.getBusId(), Long.valueOf(nextCount));
            if (nextCount % sampleStep != 0L) {
                return;
            }

            sample.add(record.toDatagram());
            if (sample.size() > maxSampleSize) {
                sampleStep = sampleStep * 2;
                compactSample();
            }
        }

        private List<Datagram> snapshot() {
            return new ArrayList<Datagram>(sample);
        }

        private void compactSample() {
            if (sample.isEmpty()) {
                return;
            }
            List<Datagram> compacted = new ArrayList<Datagram>();
            for (int i = 0; i < sample.size(); i += 2) {
                compacted.add(sample.get(i));
            }
            Datagram last = sample.get(sample.size() - 1);
            if (compacted.isEmpty() || compacted.get(compacted.size() - 1) != last) {
                compacted.add(last);
            }
            sample.clear();
            sample.addAll(compacted);
        }
    }
}
