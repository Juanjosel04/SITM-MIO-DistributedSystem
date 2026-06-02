package security.model;

import security.roles.UserRole;

public class AuthenticatedUser {
    private final Long id;
    private final String nationalId;
    private final String fullName;
    private final UserRole role;
    private final String status;

    public AuthenticatedUser(Long id, String nationalId, String fullName, UserRole role, String status) {
        if (!isValidNationalId(nationalId)) {
            throw new IllegalArgumentException("National id must contain exactly 10 numeric digits.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        this.id = id;
        this.nationalId = nationalId.trim();
        this.fullName = fullName.trim();
        this.role = role;
        this.status = status == null ? "INACTIVE" : status.trim().toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isAdmin() {
        return role.isAdmin();
    }

    public boolean isController() {
        return role.isController();
    }

    public boolean isDriver() {
        return role.isDriver();
    }

    private boolean isValidNationalId(String value) {
        return value != null && value.trim().matches("\\d{10}");
    }
}
