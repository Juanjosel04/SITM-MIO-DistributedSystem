package master.ice;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class WorkerRegistry {
    private static final String DEFAULT_WORKERS = "worker1";
    private static final String DEFAULT_WORKER1 = "WorkerService:tcp -h localhost -p 10001";

    public List<WorkerEndpointConfig> loadConfiguredWorkers() {
        Properties properties = loadProperties();
        String workerList = properties.getProperty("workers", DEFAULT_WORKERS);
        List<WorkerEndpointConfig> endpoints = new ArrayList<WorkerEndpointConfig>();
        for (String rawName : workerList.split(",")) {
            String logicalName = rawName.trim();
            if (logicalName.isEmpty()) {
                continue;
            }
            String proxy = properties.getProperty(logicalName);
            if (proxy == null || proxy.trim().isEmpty()) {
                continue;
            }
            endpoints.add(new WorkerEndpointConfig(logicalName, proxy));
        }
        if (endpoints.isEmpty()) {
            endpoints.add(new WorkerEndpointConfig("worker1", DEFAULT_WORKER1));
        }
        return Collections.unmodifiableList(endpoints);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        InputStream stream = WorkerRegistry.class.getResourceAsStream("/master.cfg");
        if (stream == null) {
            properties.setProperty("workers", DEFAULT_WORKERS);
            properties.setProperty("worker1", DEFAULT_WORKER1);
            return properties;
        }
        try {
            properties.load(stream);
            return properties;
        } catch (IOException exception) {
            Properties fallback = new Properties();
            fallback.setProperty("workers", DEFAULT_WORKERS);
            fallback.setProperty("worker1", DEFAULT_WORKER1);
            return fallback;
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // Fallback values keep the registry usable.
            }
        }
    }
}
