package security.auth;

import persistence.connection.DatabaseConfig;
import persistence.connection.DatabaseConnectionManager;
import security.model.AccessScope;
import security.model.AuthenticatedUser;
import security.model.UserCredentials;
import security.permissions.ControllerAssignmentService;
import security.repository.ControllerAssignmentRepository;
import security.repository.UserRepository;
import security.session.UserSession;
import shared.exceptions.RepositoryException;

import java.util.Optional;

public class AuthService {
    private static final String DEMO_PREFIX = "DEMO:";

    private final UserRepository userRepository;
    private final InMemoryAuthProvider inMemoryAuthProvider;
    private final ControllerAssignmentService assignmentService;

    public AuthService() {
        DatabaseConnectionManager connectionManager = new DatabaseConnectionManager(new DatabaseConfig());
        InMemoryAuthProvider provider = new InMemoryAuthProvider();
        this.userRepository = new UserRepository(connectionManager);
        this.inMemoryAuthProvider = provider;
        this.assignmentService = new ControllerAssignmentService(
                new ControllerAssignmentRepository(connectionManager), provider);
    }

    public AuthService(UserRepository userRepository,
                       InMemoryAuthProvider inMemoryAuthProvider,
                       ControllerAssignmentService assignmentService) {
        this.userRepository = userRepository;
        this.inMemoryAuthProvider = inMemoryAuthProvider;
        this.assignmentService = assignmentService;
    }

    public AuthResult authenticate(String nationalId, String password) {
        String normalizedNationalId = nationalId == null ? "" : nationalId.trim();
        if (normalizedNationalId.isEmpty()) {
            return AuthResult.failure("La cédula es obligatoria.");
        }
        if (password == null || password.trim().isEmpty()) {
            return AuthResult.failure("La contraseña es obligatoria.");
        }
        if (!normalizedNationalId.matches("\\d{10}")) {
            return AuthResult.failure("La cédula debe contener exactamente 10 dígitos numéricos.");
        }

        Optional<UserCredentials> databaseCredentials = findDatabaseCredentials(normalizedNationalId);
        if (databaseCredentials.isPresent()) {
            return authenticateCredentials(databaseCredentials.get(), password);
        }

        Optional<UserCredentials> fallbackCredentials = inMemoryAuthProvider.findByNationalId(normalizedNationalId);
        if (fallbackCredentials.isPresent()) {
            return authenticateCredentials(fallbackCredentials.get(), password);
        }

        return AuthResult.failure("Credenciales inválidas.");
    }

    private Optional<UserCredentials> findDatabaseCredentials(String nationalId) {
        if (userRepository == null) {
            return Optional.empty();
        }
        try {
            return userRepository.findByNationalId(nationalId);
        } catch (RepositoryException exception) {
            return Optional.empty();
        }
    }

    private AuthResult authenticateCredentials(UserCredentials credentials, String password) {
        AuthenticatedUser user = credentials.toAuthenticatedUser();
        if (!user.isActive()) {
            return AuthResult.failure("El usuario está inactivo.");
        }
        if (!matchesPassword(password, credentials.getPasswordHash())) {
            return AuthResult.failure("Credenciales inválidas.");
        }
        AccessScope accessScope = assignmentService == null
                ? AccessScope.noMonitoringAccess()
                : assignmentService.buildAccessScope(user);
        return AuthResult.success(new UserSession(user, accessScope));
    }

    private boolean matchesPassword(String rawPassword, String storedPasswordHash) {
        if (storedPasswordHash == null) {
            return false;
        }
        if (storedPasswordHash.startsWith(DEMO_PREFIX)) {
            return rawPassword.equals(storedPasswordHash.substring(DEMO_PREFIX.length()));
        }
        return false;
    }
}
