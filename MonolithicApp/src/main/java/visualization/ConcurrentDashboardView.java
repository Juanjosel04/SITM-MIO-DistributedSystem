package visualization;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import processing.benchmark.ProcessingMetrics;

/**
 * Minimal placeholder for the future V2 concurrent dashboard.
 */
public class ConcurrentDashboardView {
    private static final String BACKGROUND = "#f4f7fb";
    private static final String PANEL = "#ffffff";
    private static final String BORDER = "#d9e2ef";
    private static final String TEXT = "#1f2937";
    private static final String MUTED = "#64748b";

    public Parent createContent() {
        ProcessingMetrics metrics = ProcessingMetrics.empty();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");
        root.setTop(createHeader(metrics));
        root.setCenter(createBody());
        return root;
    }

    private Parent createHeader(ProcessingMetrics metrics) {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(20, 24, 16, 24));

        Label title = new Label("SITM-MIO - Version Concurrente Fork/Join");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");

        HBox cards = new HBox(12);
        cards.getChildren().addAll(
                createMetric("Tiempo", metrics.getProcessingTime().toMillis() + " ms"),
                createMetric("Datagramas", String.valueOf(metrics.getProcessedDatagrams())),
                createMetric("Rutas activas", String.valueOf(metrics.getActiveRoutes())),
                createMetric("Paralelismo", String.valueOf(metrics.getParallelism())),
                createMetric("Dataset", metrics.getDatasetName())
        );
        HBox.setHgrow(cards, Priority.ALWAYS);

        wrapper.getChildren().addAll(title, cards);
        return wrapper;
    }

    private Parent createMetric(String label, String value) {
        VBox card = new VBox(4);
        card.setMinWidth(150);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle(
                "-fx-background-color: " + PANEL + ";"
                        + "-fx-border-color: " + BORDER + ";"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );

        Label name = new Label(label);
        name.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");

        Label number = new Label(value);
        number.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");

        card.getChildren().addAll(name, number);
        return card;
    }

    private Parent createBody() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 24, 24, 24));
        grid.setHgap(16);
        grid.setVgap(16);

        StackPane mapPlaceholder = createPanel("Mapa V2");
        StackPane tablePlaceholder = createPanel("Promedios por ruta y mes");

        grid.add(mapPlaceholder, 0, 0);
        grid.add(tablePlaceholder, 1, 0);

        GridPane.setHgrow(mapPlaceholder, Priority.ALWAYS);
        GridPane.setVgrow(mapPlaceholder, Priority.ALWAYS);
        GridPane.setHgrow(tablePlaceholder, Priority.ALWAYS);
        GridPane.setVgrow(tablePlaceholder, Priority.ALWAYS);

        mapPlaceholder.setMinSize(620, 460);
        tablePlaceholder.setMinSize(420, 460);
        return grid;
    }

    private StackPane createPanel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: " + MUTED + ";");

        StackPane pane = new StackPane(label);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(18));
        pane.setStyle(
                "-fx-background-color: " + PANEL + ";"
                        + "-fx-border-color: " + BORDER + ";"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return pane;
    }
}
