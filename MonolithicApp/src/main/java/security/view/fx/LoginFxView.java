package security.view.fx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginFxView extends BorderPane {
    private final LoginHandler loginHandler;
    private final TextField nationalIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label messageLabel = new Label("");
    private final Button loginButton = new Button("Ingresar");

    public LoginFxView(LoginHandler loginHandler) {
        this.loginHandler = loginHandler;
        buildLayout();
        configureActions();
    }

    private void buildLayout() {
        setStyle("-fx-background-color: #e2e8f0;");

        VBox wrapper = new VBox(18);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(28));

        VBox panel = new VBox(18);
        panel.setMaxWidth(430);
        panel.setPadding(new Insets(24));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label title = new Label("SITM-MIO - Inicio de sesion");
        title.setTextFill(Color.web("#0f172a"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Ingrese con cedula y contrasena");
        subtitle.setTextFill(Color.web("#64748b"));
        subtitle.setFont(Font.font("System", 13));

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.add(label("Cedula"), 0, 0);
        fields.add(nationalIdField, 1, 0);
        fields.add(label("Contrasena"), 0, 1);
        fields.add(passwordField, 1, 1);
        GridPane.setHgrow(nationalIdField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(passwordField, javafx.scene.layout.Priority.ALWAYS);

        nationalIdField.setPromptText("1001234567");
        passwordField.setPromptText("Contrasena");

        messageLabel.setTextFill(Color.web("#b91c1c"));
        messageLabel.setWrapText(true);
        messageLabel.setMinHeight(24);

        Label help = new Label("Demo: admin 1001234567/admin123 | controller 2001234501/controller123 | driver 3001234567/driver123");
        help.setTextFill(Color.web("#64748b"));
        help.setFont(Font.font("System", 11));
        help.setWrapText(true);

        loginButton.setTextFill(Color.WHITE);
        loginButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        loginButton.setMinWidth(120);
        loginButton.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 4; -fx-padding: 8 14 8 14;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        actions.getChildren().addAll(spacer, loginButton);

        panel.getChildren().addAll(title, subtitle, fields, messageLabel, actions, help);
        wrapper.getChildren().add(panel);
        setCenter(wrapper);
    }

    private void configureActions() {
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> submit());
        passwordField.setOnAction(event -> submit());
        nationalIdField.setOnAction(event -> submit());
        nationalIdField.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> clearMessage());
    }

    private void submit() {
        clearMessage();
        if (loginHandler != null) {
            loginHandler.onLoginRequested(text(nationalIdField), text(passwordField), this);
        }
    }

    public void showError(String message) {
        messageLabel.setText(message == null ? "" : message);
    }

    public void clearMessage() {
        messageLabel.setText("");
    }

    public void reset() {
        nationalIdField.clear();
        passwordField.clear();
        clearMessage();
        requestInitialFocus();
    }

    public void requestInitialFocus() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                nationalIdField.requestFocus();
            }
        });
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#334155"));
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        return label;
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    public interface LoginHandler {
        void onLoginRequested(String nationalId, String password, LoginFxView view);
    }
}
