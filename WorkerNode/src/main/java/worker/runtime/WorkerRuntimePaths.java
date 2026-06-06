package worker.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WorkerRuntimePaths {
    public static final String DEFAULT_WORKER_ID = "worker-local";
    private static final int DEFAULT_MAX_RECEIVED_JOBS_TO_KEEP = 2;

    private final String workerId;
    private final String safeWorkerId;
    private final Path baseDirectory;

    private WorkerRuntimePaths(String workerId, Path baseDirectory) {
        this.workerId = normalizeWorkerId(workerId);
        this.safeWorkerId = sanitizePathPart(this.workerId);
        this.baseDirectory = baseDirectory.toAbsolutePath().normalize();
    }

    public static WorkerRuntimePaths fromSystemProperties() {
        String workerId = System.getProperty("sitm.worker.id", DEFAULT_WORKER_ID);
        String configuredStorage = System.getProperty("sitm.worker.storage.dir");
        if (configuredStorage != null && !configuredStorage.trim().isEmpty()) {
            return new WorkerRuntimePaths(workerId, Paths.get(configuredStorage.trim()));
        }
        return new WorkerRuntimePaths(workerId,
                resolveBuildDirectory().resolve("tmp").resolve("workers").resolve(sanitizePathPart(normalizeWorkerId(workerId))));
    }

    public String workerId() {
        return workerId;
    }

    public String safeWorkerId() {
        return safeWorkerId;
    }

    public Path baseDirectory() {
        return baseDirectory;
    }

    public Path receivedBucketsDirectory() {
        return baseDirectory.resolve("received-buckets");
    }

    public Path demoBucketsDirectory() {
        return baseDirectory.resolve("demo-buckets");
    }

    public Path logsDirectory() {
        return baseDirectory.resolve("logs");
    }

    public void ensureDirectories() {
        try {
            createRuntimeDirectories();
            cleanupOldReceivedJobs();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create worker runtime directories under "
                    + baseDirectory + ": " + exception.getMessage(), exception);
        }
    }

    public void ensureDirectoriesWithoutCleanup() {
        try {
            createRuntimeDirectories();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create worker runtime directories under "
                    + baseDirectory + ": " + exception.getMessage(), exception);
        }
    }

    private void createRuntimeDirectories() throws IOException {
        Files.createDirectories(receivedBucketsDirectory());
        Files.createDirectories(demoBucketsDirectory());
        Files.createDirectories(logsDirectory());
    }

    private void cleanupOldReceivedJobs() throws IOException {
        Path root = receivedBucketsDirectory();
        if (!Files.exists(root)) {
            return;
        }
        List<Path> jobs = new ArrayList<Path>();
        java.util.stream.Stream<Path> stream = Files.list(root);
        try {
            stream.filter(Files::isDirectory).forEach(jobs::add);
        } finally {
            stream.close();
        }
        if (jobs.size() <= maxReceivedJobsToKeep()) {
            return;
        }
        jobs.sort(new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                try {
                    return Files.getLastModifiedTime(right).compareTo(Files.getLastModifiedTime(left));
                } catch (IOException exception) {
                    return right.toString().compareTo(left.toString());
                }
            }
        });
        for (int index = maxReceivedJobsToKeep(); index < jobs.size(); index++) {
            deleteRecursivelyQuietly(jobs.get(index));
        }
    }

    private int maxReceivedJobsToKeep() {
        String value = System.getProperty("sitm.worker.received.jobs.keep",
                String.valueOf(DEFAULT_MAX_RECEIVED_JOBS_TO_KEEP));
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed < 0 ? DEFAULT_MAX_RECEIVED_JOBS_TO_KEEP : parsed;
        } catch (RuntimeException exception) {
            return DEFAULT_MAX_RECEIVED_JOBS_TO_KEEP;
        }
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
                    // Best-effort cleanup; runtime startup should not fail because an old file is locked.
                }
            }
        } catch (IOException ignored) {
            // Best-effort cleanup; runtime startup should not fail because an old file is locked.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static Path resolveBuildDirectory() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if ("WorkerNode".equalsIgnoreCase(current.getFileName().toString())) {
            return current.resolve("build");
        }
        return current.resolve("WorkerNode").resolve("build");
    }

    private static String normalizeWorkerId(String workerId) {
        if (workerId == null || workerId.trim().isEmpty()) {
            return DEFAULT_WORKER_ID;
        }
        return workerId.trim();
    }

    private static String sanitizePathPart(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isEmpty() ? DEFAULT_WORKER_ID : safe;
    }
}
