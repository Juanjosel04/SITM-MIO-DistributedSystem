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
import java.util.concurrent.ThreadLocalRandom;

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
        ReservoirVisualDatagramSampler visualSampler = new ReservoirVisualDatagramSampler(config.getVisualSampleLimit());
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

    /**
     * Reservoir sampler (Algorithm R, Vitter 1985).
     *
     * Maintains a fixed-size reservoir of exactly {@code reservoirSize} elements
     * chosen uniformly at random from the full stream, in a single pass and with
     * O(reservoirSize) memory regardless of stream length.  Only records that have
     * both coordinates and a valid route id are eligible; the rest are ignored so
     * they cannot corrupt the visual sample.
     */
    private static final class ReservoirVisualDatagramSampler {
        private final int reservoirSize;
        private final Datagram[] reservoir;
        private long eligibleCount;

        private ReservoirVisualDatagramSampler(int reservoirSize) {
            this.reservoirSize = Math.max(1, reservoirSize);
            this.reservoir = new Datagram[this.reservoirSize];
            this.eligibleCount = 0L;
        }

        private void offer(BucketedDatagramRecord record) {
            if (record == null
                    || record.getBusId().isEmpty()
                    || record.getRouteId() == -1
                    || record.getLatitude() == null
                    || record.getLongitude() == null) {
                return;
            }
            if (eligibleCount < reservoirSize) {
                reservoir[(int) eligibleCount] = record.toDatagram();
            } else {
                // Choose a uniform random slot in [0, eligibleCount] inclusive.
                // If it falls inside the reservoir, replace that slot.
                long j = ThreadLocalRandom.current().nextLong(eligibleCount + 1L);
                if (j < reservoirSize) {
                    reservoir[(int) j] = record.toDatagram();
                }
            }
            eligibleCount++;
        }

        private List<Datagram> snapshot() {
            int count = (int) Math.min(eligibleCount, (long) reservoirSize);
            List<Datagram> result = new ArrayList<Datagram>(count);
            for (int i = 0; i < count; i++) {
                result.add(reservoir[i]);
            }
            return result;
        }
    }
}
