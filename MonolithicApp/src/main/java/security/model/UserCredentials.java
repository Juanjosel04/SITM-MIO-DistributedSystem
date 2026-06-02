package security.model;

import security.roles.UserRole;

public class UserCredentials {
    private final Long id;
    private final String nationalId;
    private final String fullName;
    private final String passwordHash;
    private final UserRole role;
    private final String status;

    public UserCredentials(Long id, String nationalId, String fullName, String passwordHash,
                           UserRole role, String status) {
        this.id = id;
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, nationalId, fullName, role, status);
    }
}
