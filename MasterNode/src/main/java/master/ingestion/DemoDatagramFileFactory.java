package master.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DemoDatagramFileFactory {
    public Path createDemoDataset() throws IOException {
        Path directory = resolveBuildDirectory().resolve("tmp").resolve("demo-datasets");
        Files.createDirectories(directory);
        Path file = directory.resolve("datagrams-demo.csv");
        Files.write(file, demoContent().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private Path resolveBuildDirectory() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if ("MasterNode".equalsIgnoreCase(current.getFileName().toString())) {
            return current.resolve("build");
        }
        return current.resolve("MasterNode").resolve("build");
    }

    private String demoContent() {
        return "c0,c1,c2,odometer,latitude,longitude,c6,routeId,c8,c9,datagramDate,busId\n"
                + "x,x,x,1000,3.4516,-76.5320,x,P10A,x,x,2023-04-01T10:00:00,BUS-001\n"
                + "x,x,x,1200,3.4520,-76.5325,x,P10A,x,x,2023-04-01T10:01:00,BUS-001\n"
                + "x,x,x,5000,3.4600,-76.5400,x,T31,x,x,2023-04-01T10:00:30,BUS-002\n"
                + "x,x,x,5400,3.4610,-76.5410,x,T31,x,x,2023-04-01T10:02:30,BUS-002\n"
                + "x,x,x,7000,3.4700,-76.5500,x,P47A,x,x,2023-04-01T10:03:00,BUS-003\n";
    }
}
