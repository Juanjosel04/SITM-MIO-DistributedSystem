package monitoring.model;

public class MonitoringMetric {
    private final String name;
    private final String value;
    private final String description;

    public MonitoringMetric(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
