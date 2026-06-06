package master.transfer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestBucketFileFactory {
    public static final int DEFAULT_CHUNK_SIZE_BYTES = 256 * 1024;
    public static final String DEMO_JOB_ID = "job-demo-001";
    public static final String DEMO_BUCKET_ID = "bucket-demo-001";

    public BucketTransferRequest createDemoBucket() throws IOException {
        Path directory = resolveBuildDirectory().resolve("tmp").resolve("test-buckets");
        Files.createDirectories(directory);
        Path file = directory.resolve(DEMO_BUCKET_ID + ".csv");
        Files.write(file, demoContent().getBytes(StandardCharsets.UTF_8));
        long totalBytes = Files.size(file);
        return new BucketTransferRequest(DEMO_JOB_ID, DEMO_BUCKET_ID, file, DEFAULT_CHUNK_SIZE_BYTES, totalBytes);
    }

    private Path resolveBuildDirectory() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if ("MasterNode".equalsIgnoreCase(current.getFileName().toString())) {
            return current.resolve("build");
        }
        return current.resolve("MasterNode").resolve("build");
    }

    private String demoContent() {
        return "busId,routeId,odometer,timestamp,latitude,longitude\n"
                + "BUS-001,P10A,1000,2023-04-01T10:00:00,-76.5320,3.4516\n"
                + "BUS-001,P10A,1200,2023-04-01T10:01:00,-76.5325,3.4520\n"
                + "BUS-002,T31,5000,2023-04-01T10:00:30,-76.5400,3.4600\n"
                + "BUS-002,T31,5400,2023-04-01T10:02:30,-76.5410,3.4610\n";
    }
}
