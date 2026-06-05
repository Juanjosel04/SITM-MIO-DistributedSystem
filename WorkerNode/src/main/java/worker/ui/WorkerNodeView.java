package worker.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WorkerNodeView {
    private static final String[] FUTURE_STATES = {
            "Esperando orden de procesamiento",
            "Recibiendo bucket",
            "Cargando chunk 1",
            "Cargando chunk 2",
            "Cargando chunk 3",
            "Bucket recibido",
            "Iniciando procesamiento de datagramas",
            "Procesando bucket-0",
            "Procesando bucket-1",
            "Procesamiento finalizado",
            "Enviando resultado al Master",
            "Error"
    };

    private final String workerId;
    private final int port;

    public WorkerNodeView(String workerId, int port) {
        this.workerId = workerId == null || workerId.trim().isEmpty() ? "worker-local" : workerId.trim();
        this.port = port;
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f7f9fc;");

        root.setTop(createHeader());
        root.setCenter(createStatusPanel());
        root.setBottom(createLogPanel());

        return root;
    }

    private Parent createHeader() {
        Label title = new Label("Worker Node");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #172033;");

        Label idLabel = new Label("Worker ID: " + workerId);
        Label portLabel = new Label("Puerto ICE: " + port);
        idLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
        portLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");

        VBox header = new VBox(6, title, idLabel, portLabel);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Parent createStatusPanel() {
        Label state = new Label("Estado: Esperando orden de procesamiento");
        state.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        GridPane metrics = new GridPane();
        metrics.setHgap(18);
        metrics.setVgap(12);
        addMetric(metrics, 0, "Job actual:", "ninguno");
        addMetric(metrics, 1, "Bucket actual:", "ninguno");
        addMetric(metrics, 2, "Buckets recibidos:", "0");
        addMetric(metrics, 3, "Buckets procesados:", "0");
        addMetric(metrics, 4, "Intervalos válidos:", "0");
        addMetric(metrics, 5, "Descartes:", "0");
        addMetric(metrics, 6, "Tiempo local:", "0 ms");
        addMetric(metrics, 7, "Errores:", "0");

        Label statesLabel = new Label("Estados futuros: " + String.join(" | ", FUTURE_STATES));
        statesLabel.setWrapText(true);
        statesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox panel = new VBox(18, state, progressBar, metrics, statesLabel);
        panel.setPadding(new Insets(18));
        panel.setStyle(cardStyle());
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private Parent createLogPanel() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(7);
        logArea.setText("Worker inicializado.\nEsperando orden de procesamiento.");

        VBox panel = new VBox(8, sectionTitle("Log de eventos"), logArea);
        panel.setPadding(new Insets(18, 0, 0, 0));
        BorderPane.setAlignment(panel, Pos.CENTER);
        return panel;
    }

    private void addMetric(GridPane grid, int row, String name, String value) {
        Label nameLabel = new Label(name);
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-weight: bold;");
        grid.add(nameLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        return label;
    }

    private String cardStyle() {
        return "-fx-background-color: #ffffff;"
                + "-fx-border-color: #d9e2ef;"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;";
    }
}
