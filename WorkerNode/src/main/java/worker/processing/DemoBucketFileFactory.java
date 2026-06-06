package worker.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import worker.runtime.WorkerRuntimePaths;

public final class DemoBucketFileFactory {
    private final WorkerRuntimePaths runtimePaths;

    public DemoBucketFileFactory(WorkerRuntimePaths runtimePaths) {
        this.runtimePaths = runtimePaths;
    }

    public Path createDemoBucket() throws IOException {
        runtimePaths.ensureDirectories();
        Path directory = runtimePaths.demoBucketsDirectory();
        Files.createDirectories(directory);
        Path file = directory.resolve("bucket-worker-demo.csv");
        Files.write(file, demoContent().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private String demoContent() {
        return "busId,routeId,odometer,timestamp,latitude,longitude\n"
                + "BUS-001,P10A,1000,2023-04-01T10:00:00,3.4516,-76.5320\n"
                + "BUS-001,P10A,1200,2023-04-01T10:01:00,3.4520,-76.5325\n"
                + "BUS-001,P10A,1300,2023-04-01T10:02:00,3.4525,-76.5330\n"
                + "BUS-001,T31,1500,2023-04-01T10:03:00,3.4530,-76.5335\n"
                + "BUS-002,T31,5000,2023-04-01T10:00:30,3.4600,-76.5400\n"
                + "BUS-002,T31,5400,2023-04-01T10:02:30,3.4610,-76.5410\n"
                + "BUS-003,P47A,1000,2023-04-01T10:00:00,3.4700,-76.5500\n"
                + "BUS-003,P47A,10000,2023-04-01T10:01:00,3.4710,-76.5510\n"
                + "BUS-004,-1,7000,2023-04-01T10:05:00,3.4800,-76.5600\n";
    }
}
