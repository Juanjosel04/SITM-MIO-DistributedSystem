package worker.ice;

public final class WorkerIceConfig {
    private final String workerId;
    private final String host;
    private final int port;

    public WorkerIceConfig(String workerId, String host, int port) {
        this.workerId = workerId == null || workerId.trim().isEmpty() ? "worker-local" : workerId.trim();
        this.host = host == null || host.trim().isEmpty() ? "0.0.0.0" : host.trim();
        this.port = port <= 0 ? 10001 : port;
    }

    public static WorkerIceConfig defaults(String workerId, int port) {
        return new WorkerIceConfig(workerId, "0.0.0.0", port);
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String endpoints() {
        return "tcp -h " + host + " -p " + port;
    }
}
