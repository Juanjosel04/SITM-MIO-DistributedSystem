package worker.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class WorkerRuntimePaths {
    public static final String DEFAULT_WORKER_ID = "worker-local";

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
            Files.createDirectories(receivedBucketsDirectory());
            Files.createDirectories(demoBucketsDirectory());
            Files.createDirectories(logsDirectory());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create worker runtime directories under "
                    + baseDirectory + ": " + exception.getMessage(), exception);
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
