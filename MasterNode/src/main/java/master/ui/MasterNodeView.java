package master.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MasterNodeView {
    private final Label statusLabel = new Label("Estado: Esperando workers");
    private final Label actionLabel = new Label("V3.1 estructura base creada. Funcionalidad distribuida pendiente.");

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f6f8fb;");

        root.setTop(createHeader());
        root.setLeft(createControlPanel());
        root.setCenter(createMainPanel());

        return root;
    }

    private Parent createHeader() {
        Label title = new Label("Master Node - Reconstrucción Distribuida SITM-MIO");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #162033;");

        statusLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #334155;");
        actionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        VBox header = new VBox(6, title, statusLabel, actionLabel);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Parent createControlPanel() {
        Label workersTitle = sectionTitle("Workers detectados: 0");

        Button detectWorkersButton = new Button("Detectar workers");
        detectWorkersButton.setMaxWidth(Double.MAX_VALUE);
        detectWorkersButton.setOnAction(event -> actionLabel.setText("Funcionalidad pendiente para V3.3"));

        Button startButton = new Button("Iniciar reconstrucción de rutas");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(event -> actionLabel.setText("Procesamiento distribuido pendiente para fases posteriores"));

        VBox progressRows = new VBox(8,
                metric("Buckets totales:", "0"),
                metric("Buckets pendientes:", "0"),
                metric("Buckets en proceso:", "0"),
                metric("Buckets completados:", "0")
        );

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        VBox panel = new VBox(16, workersTitle, detectWorkersButton, startButton, sectionTitle("Progreso"), progressRows, progressBar);
        panel.setPrefWidth(300);
        panel.setPadding(new Insets(18));
        panel.setStyle(cardStyle());
        return panel;
    }

    private Parent createMainPanel() {
        StackPane mapPlaceholder = placeholder("Mapa distribuido / reconstrucción visual pendiente");
        mapPlaceholder.setMinHeight(360);

        StackPane tablePlaceholder = placeholder("Tabla de promedios pendiente");
        tablePlaceholder.setMinHeight(180);

        VBox main = new VBox(16, mapPlaceholder, tablePlaceholder);
        main.setPadding(new Insets(0, 0, 0, 18));
        VBox.setVgrow(mapPlaceholder, Priority.ALWAYS);
        VBox.setVgrow(tablePlaceholder, Priority.ALWAYS);
        return main;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        return label;
    }

    private Parent metric(String name, String value) {
        Label nameLabel = new Label(name);
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-weight: bold;");

        GridPane row = new GridPane();
        row.setHgap(8);
        row.add(nameLabel, 0, 0);
        row.add(valueLabel, 1, 0);
        return row;
    }

    private StackPane placeholder(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569;");

        StackPane pane = new StackPane(label);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(24));
        pane.setStyle(cardStyle());
        HBox.setHgrow(pane, Priority.ALWAYS);
        return pane;
    }

    private String cardStyle() {
        return "-fx-background-color: #ffffff;"
                + "-fx-border-color: #d9e2ef;"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;";
    }
}
