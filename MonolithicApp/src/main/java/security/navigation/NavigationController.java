package security.navigation;

import analytics.controller.AnalyticsController;
import events.service.EventService;
import events.view.fx.BusConsoleFxView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import monitoring.controller.MonitoringController;
import monitoring.view.fx.ControllerDashboardFxShell;
import monitoring.view.fx.MainDashboardFxShell;
import security.auth.AuthResult;
import security.auth.AuthService;
import security.session.UserSession;
import security.view.fx.LoginFxView;

public class NavigationController implements LoginFxView.LoginHandler {
    private final Stage stage;
    private final AuthService authService;
    private final MonitoringController monitoringController;
    private final EventService eventService;
    private final AnalyticsController analyticsController;
    private UserSession currentSession;

    public NavigationController(Stage stage, AuthService authService, MonitoringController monitoringController,
                                EventService eventService, AnalyticsController analyticsController) {
        this.stage = stage;
        this.authService = authService;
        this.monitoringController = monitoringController;
        this.eventService = eventService;
        this.analyticsController = analyticsController;
    }

    public void showLogin() {
        showLogin(null);
    }

    private void showLogin(String initialError) {
        currentSession = null;
        LoginFxView loginView = new LoginFxView(this);
        stage.setTitle("SITM-MIO - Login");
        stage.setScene(new Scene(loginView, 720, 520));
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.show();
        loginView.reset();
        if (initialError != null && initialError.trim().length() > 0) {
            loginView.showError(initialError);
        }
    }

    @Override
    public void onLoginRequested(String nationalId, String password, LoginFxView view) {
        AuthResult result = authService.authenticate(nationalId, password);
        if (!result.isSuccess()) {
            view.showError(result.getMessage());
            return;
        }
        onLoginSuccess(result.getSession());
    }

    public void onLoginSuccess(UserSession session) {
        currentSession = session;
        if (session.isAdmin()) {
            showAdminDashboard(session);
        } else if (session.isController()) {
            showControllerDashboard(session);
        } else if (session.isDriver()) {
            showDriverConsole(session);
        } else {
            session.logout();
            showLogin("El rol autenticado no tiene navegacion configurada.");
        }
    }

    public void showAdminDashboard(UserSession session) {
        MainDashboardFxShell shell = new MainDashboardFxShell(monitoringController, eventService, analyticsController,
                new Runnable() {
                    @Override
                    public void run() {
                        logout();
                    }
                });
        stage.setTitle("SITM-MIO - Dashboard Admin");
        stage.setScene(new Scene(shell, 1120, 720));
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.show();
    }

    public void showControllerDashboard(UserSession session) {
        ControllerDashboardFxShell shell = new ControllerDashboardFxShell(session, monitoringController,
                analyticsController, new Runnable() {
            @Override
            public void run() {
                logout();
            }
        });
        stage.setTitle("SITM-MIO - Panel del Controlador");
        stage.setScene(new Scene(shell, 920, 620));
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.show();
    }

    public void showDriverConsole(UserSession session) {
        BusConsoleFxView console = new BusConsoleFxView(eventService, monitoringController);
        BorderPane wrapper = new BorderPane();
        wrapper.setStyle("-fx-background-color: #e2e8f0;");
        wrapper.setTop(createSessionHeader("Conductor", session));
        wrapper.setCenter(console.getView());
        stage.setTitle("SITM-MIO - Consola del Conductor");
        stage.setScene(new Scene(wrapper, 560, 680));
        stage.setMinWidth(500);
        stage.setMinHeight(620);
        stage.show();
    }

    public void logout() {
        if (currentSession != null) {
            currentSession.logout();
        }
        currentSession = null;
        showLogin();
    }

    private HBox createSessionHeader(String title, UserSession session) {
        HBox header = new HBox(12);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #0f172a;");

        Label titleLabel = new Label(title + " - " + session.getUser().getFullName());
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label nationalIdLabel = new Label(session.getUser().getNationalId());
        nationalIdLabel.setTextFill(Color.web("#cbd5e1"));
        nationalIdLabel.setFont(Font.font("System", 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
        logoutButton.setOnAction(event -> logout());

        header.getChildren().addAll(titleLabel, nationalIdLabel, spacer, logoutButton);
        return header;
    }
}
