package master.ice;

public final class WorkerEndpointConfig {
    private final String logicalName;
    private final String proxyString;
    private final String configuredHost;
    private final int configuredPort;

    public WorkerEndpointConfig(String logicalName, String proxyString) {
        this.logicalName = logicalName == null ? "" : logicalName.trim();
        this.proxyString = proxyString == null ? "" : proxyString.trim();
        this.configuredHost = parseHost(this.proxyString);
        this.configuredPort = parsePort(this.proxyString);
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getProxyString() {
        return proxyString;
    }

    public String getConfiguredHost() {
        return configuredHost;
    }

    public int getConfiguredPort() {
        return configuredPort;
    }

    private static String parseHost(String proxy) {
        String[] parts = proxy.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("-h".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return "localhost";
    }

    private static int parsePort(String proxy) {
        String[] parts = proxy.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("-p".equals(parts[i])) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException ignored) {
                    return 10001;
                }
            }
        }
        return 10001;
    }
}
