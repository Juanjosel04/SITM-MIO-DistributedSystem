package master.bucket;

import master.ingestion.DemoDatagramFileFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BucketizationConfig {
    public static final int DEFAULT_BUCKET_COUNT = 16;
    private static final DateTimeFormatter JOB_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final String jobId;
    private final Path datagramsPath;
    private final Path outputDirectory;
    private final int bucketCount;
    private final boolean demoDataset;

    public BucketizationConfig(String jobId, Path datagramsPath, Path outputDirectory, int bucketCount,
                               boolean demoDataset) {
        this.jobId = jobId;
        this.datagramsPath = datagramsPath;
        this.outputDirectory = outputDirectory;
        this.bucketCount = bucketCount;
        this.demoDataset = demoDataset;
    }

    public static BucketizationConfig fromSystemProperties() throws IOException {
        String jobId = "job-bucketization-" + LocalDateTime.now().format(JOB_FORMAT);
        int bucketCount = readBucketCount();
        Path datagramsPath;
        boolean demo;
        String configuredPath = System.getProperty("sitm.datagrams.path");
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            datagramsPath = new DemoDatagramFileFactory().createDemoDataset();
            demo = true;
        } else {
            datagramsPath = resolveConfiguredPath(configuredPath.trim());
            demo = false;
        }
        Path outputDirectory = resolveBuildDirectory().resolve("tmp").resolve("generated-buckets").resolve(jobId);
        return new BucketizationConfig(jobId, datagramsPath, outputDirectory, bucketCount, demo);
    }

    public String getJobId() {
        return jobId;
    }

    public Path getDatagramsPath() {
        return datagramsPath;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public boolean isDemoDataset() {
        return demoDataset;
    }

    private static int readBucketCount() {
        String value = System.getProperty("sitm.bucket.count", String.valueOf(DEFAULT_BUCKET_COUNT));
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed <= 0 ? DEFAULT_BUCKET_COUNT : parsed;
        } catch (NumberFormatException exception) {
            return DEFAULT_BUCKET_COUNT;
        }
    }

    private static Path resolveBuildDirectory() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if ("MasterNode".equalsIgnoreCase(current.getFileName().toString())) {
            return current.resolve("build");
        }
        return current.resolve("MasterNode").resolve("build");
    }

    private static Path resolveConfiguredPath(String configuredPath) {
        Path direct = Paths.get(configuredPath).toAbsolutePath().normalize();
        if (java.nio.file.Files.exists(direct)) {
            return direct;
        }
        Path current = Paths.get("").toAbsolutePath().normalize();
        if ("MasterNode".equalsIgnoreCase(current.getFileName().toString()) && current.getParent() != null) {
            Path fromRoot = current.getParent().resolve(configuredPath).normalize();
            if (java.nio.file.Files.exists(fromRoot)) {
                return fromRoot;
            }
        }
        return direct;
    }
}
