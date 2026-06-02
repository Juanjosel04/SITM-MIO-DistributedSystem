package security.roles;

public enum UserRole {
    ADMIN,
    CONTROLLER,
    DRIVER;

    public static UserRole fromDatabaseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("User role is required.");
        }
        String normalized = value.trim().toUpperCase();
        for (UserRole role : values()) {
            if (role.name().equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported user role: " + value);
    }

    public boolean isAdmin() {
        return ADMIN == this;
    }

    public boolean isController() {
        return CONTROLLER == this;
    }

    public boolean isDriver() {
        return DRIVER == this;
    }
}
