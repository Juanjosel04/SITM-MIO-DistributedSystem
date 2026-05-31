package persistence.connection;

import shared.constants.DatabaseConstants;

public class DatabaseConfig {
    private final String url;
    private final String user;
    private final String password;
    private final String driverClassName;

    public DatabaseConfig() {
        this.url = readValue("SITM_DB_URL", "sitm.db.url", DatabaseConstants.DEFAULT_URL);
        this.user = readValue("SITM_DB_USER", "sitm.db.user", DatabaseConstants.DEFAULT_USER);
        this.password = readValue("SITM_DB_PASSWORD", "sitm.db.password", DatabaseConstants.DEFAULT_PASSWORD);
        this.driverClassName = DatabaseConstants.DRIVER_CLASS_NAME;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    private String readValue(String environmentName, String propertyName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue;
        }
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue;
        }
        return defaultValue;
    }
}
