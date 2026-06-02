package security.session;

import security.model.AccessScope;
import security.model.AuthenticatedUser;

import java.time.LocalDateTime;

public class UserSession {
    private final AuthenticatedUser user;
    private final AccessScope accessScope;
    private final LocalDateTime loginTime;
    private boolean active = true;

    public UserSession(AuthenticatedUser user, AccessScope accessScope) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }
        if (accessScope == null) {
            throw new IllegalArgumentException("Access scope is required.");
        }
        this.user = user;
        this.accessScope = accessScope;
        this.loginTime = LocalDateTime.now();
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public AccessScope getAccessScope() {
        return accessScope;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public boolean isActive() {
        return active;
    }

    public void logout() {
        active = false;
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }

    public boolean isController() {
        return user.isController();
    }

    public boolean isDriver() {
        return user.isDriver();
    }
}
