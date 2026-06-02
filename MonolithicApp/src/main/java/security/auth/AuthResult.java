package security.auth;

import security.session.UserSession;

public class AuthResult {
    private final boolean success;
    private final UserSession session;
    private final String message;

    private AuthResult(boolean success, UserSession session, String message) {
        this.success = success;
        this.session = session;
        this.message = message;
    }

    public static AuthResult success(UserSession session) {
        return new AuthResult(true, session, "Autenticación exitosa.");
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public UserSession getSession() {
        return session;
    }

    public String getMessage() {
        return message;
    }
}
