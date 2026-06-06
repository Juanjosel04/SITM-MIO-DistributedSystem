package master.ice;

public final class WorkerConnectionResult {
    private final boolean up;
    private final String logicalName;
    private final String proxy;
    private final String workerId;
    private final String status;
    private final String host;
    private final int port;
    private final String message;
    private final String checkedAt;

    private WorkerConnectionResult(boolean up, String logicalName, String proxy, String workerId, String status,
                                   String host, int port, String message, String checkedAt) {
        this.up = up;
        this.logicalName = logicalName;
        this.proxy = proxy;
        this.workerId = workerId;
        this.status = status;
        this.host = host;
        this.port = port;
        this.message = message;
        this.checkedAt = checkedAt;
    }

    public static WorkerConnectionResult up(WorkerEndpointConfig endpoint, String workerId, String status,
                                            String host, int port, String message, String checkedAt) {
        return new WorkerConnectionResult(true, endpoint.getLogicalName(), endpoint.getProxyString(), workerId,
                status, host, port, message, checkedAt);
    }

    public static WorkerConnectionResult down(WorkerEndpointConfig endpoint, String message, String checkedAt) {
        return new WorkerConnectionResult(false, endpoint.getLogicalName(), endpoint.getProxyString(),
                endpoint.getLogicalName(), "DOWN", endpoint.getConfiguredHost(), endpoint.getConfiguredPort(),
                message, checkedAt);
    }

    public boolean isUp() {
        return up;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getProxy() {
        return proxy;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getStatus() {
        return status;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getMessage() {
        return message;
    }

    public String getCheckedAt() {
        return checkedAt;
    }
}
