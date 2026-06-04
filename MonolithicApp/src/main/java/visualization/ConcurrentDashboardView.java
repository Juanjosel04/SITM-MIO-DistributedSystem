package visualization;

import domain.MonthlyRouteAverage;
import domain.Route;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import processing.AverageSpeedProcessingResult;
import processing.benchmark.ProcessingMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic dashboard for the V2 concurrent processing flow.
 */
public class ConcurrentDashboardView {
    private static final String BACKGROUND = "#f4f7fb";
    private static final String PANEL = "#ffffff";
    private static final String BORDER = "#d9e2ef";
    private static final String TEXT = "#1f2937";
    private static final String MUTED = "#64748b";
    private static final String ALL = "Todos";

    private final BorderPane root;
    private final VBox header;
    private final Label statusLabel;
    private final ComboBox<FilterOption> routeFilter;
    private final ComboBox<String> yearFilter;
    private final ComboBox<String> monthFilter;
    private final TableView<MonthlyAverageTableRow> table;
    private final ObservableList<MonthlyAverageTableRow> tableRows;
    private final List<MonthlyAverageTableRow> allRows;

    public ConcurrentDashboardView() {
        this.root = new BorderPane();
        this.header = new VBox(14);
        this.statusLabel = new Label();
        this.routeFilter = new ComboBox<FilterOption>();
        this.yearFilter = new ComboBox<String>();
        this.monthFilter = new ComboBox<String>();
        this.table = new TableView<MonthlyAverageTableRow>();
        this.tableRows = FXCollections.observableArrayList();
        this.allRows = new ArrayList<MonthlyAverageTableRow>();
        configureRoot();
        configureTable();
        showLoadingState("Cargando datos...");
    }

    public Parent createContent() {
        return root;
    }

    public Parent createContent(ProcessingMetrics metrics, int averageCount, String statusText) {
        showMetrics(metrics);
        showStatus(statusText + " - Resultados preparados: " + averageCount);
        return root;
    }

    public void showLoadingState(String message) {
        showMetrics(ProcessingMetrics.empty());
        showStatus(message);
        allRows.clear();
        tableRows.clear();
        configureFilters(Collections.<Route>emptyList(), Collections.<MonthlyAverageTableRow>emptyList());
    }

    public void showResult(AverageSpeedProcessingResult result) {
        showMetrics(result.getMetrics());
        allRows.clear();
        for (MonthlyRouteAverage average : result.getAverages()) {
            allRows.add(new MonthlyAverageTableRow(average));
        }
        configureFilters(result.getRoutes(), allRows);
        applyFilters();
        showStatus("Procesamiento Fork/Join completado");
    }

    public void showError(String message) {
        showMetrics(ProcessingMetrics.empty());
        allRows.clear();
        tableRows.clear();
        configureFilters(Collections.<Route>emptyList(), Collections.<MonthlyAverageTableRow>emptyList());
        showStatus(message);
    }

    private void configureRoot() {
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");
        root.setTop(header);
        root.setCenter(createBody());
    }

    private void showMetrics(ProcessingMetrics metrics) {
        header.getChildren().clear();
        header.setPadding(new Insets(20, 24, 16, 24));

        Label title = new Label("SITM-MIO V2 Concurrente - Fork/Join");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");

        GridPane cards = new GridPane();
        cards.setHgap(10);
        cards.setVgap(10);
        cards.getChildren().addAll(
                metricAt("Dataset", metrics.getDatasetName(), 0, 0),
                metricAt("Rutas", String.valueOf(metrics.getLoadedRoutes()), 1, 0),
                metricAt("Datagramas leidos", String.valueOf(metrics.getReadDatagrams()), 2, 0),
                metricAt("Intervalos validos", String.valueOf(metrics.getValidIntervals()), 3, 0),
                metricAt("Ruta/mes", String.valueOf(metrics.getRouteMonthCombinations()), 0, 1),
                metricAt("Rutas sin datos", String.valueOf(metrics.getRoutesWithoutData()), 1, 1),
                metricAt("Parallelism", String.valueOf(metrics.getParallelism()), 2, 1),
                metricAt("Threshold", String.valueOf(metrics.getThreshold()), 3, 1),
                metricAt("Tiempo F/J", metrics.getForkJoinProcessingTimeMillis() + " ms", 4, 1),
                metricAt("Velocidad global", String.format(java.util.Locale.US, "%.2f km/h", Double.valueOf(metrics.getGlobalAverageSpeedKmh())), 5, 1)
        );

        header.getChildren().addAll(title, cards);
    }

    private Node metricAt(String label, String value, int column, int row) {
        Parent metric = createMetric(label, value);
        GridPane.setConstraints(metric, column, row);
        GridPane.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private Parent createMetric(String label, String value) {
        VBox card = new VBox(4);
        card.setMinWidth(155);
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
        VBox body = new VBox(12);
        body.setPadding(new Insets(0, 24, 24, 24));

        HBox filters = new HBox(10);
        filters.getChildren().addAll(
                createFilterBlock("Ruta", routeFilter),
                createFilterBlock("Anio", yearFilter),
                createFilterBlock("Mes", monthFilter)
        );

        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + MUTED + ";");
        table.setItems(tableRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        body.getChildren().addAll(filters, statusLabel, table);
        return body;
    }

    private Parent createFilterBlock(String labelText, ComboBox<?> comboBox) {
        VBox box = new VBox(4);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        comboBox.setMinWidth(160);
        box.getChildren().addAll(label, comboBox);
        return box;
    }

    private void configureTable() {
        table.getColumns().clear();
        table.getColumns().add(column("Ruta", "route"));
        table.getColumns().add(column("Anio", "yearText"));
        table.getColumns().add(column("Mes", "monthText"));
        table.getColumns().add(column("Velocidad promedio", "averageSpeed"));
        table.getColumns().add(column("Intervalos", "intervalCount"));
        table.getColumns().add(column("Estado", "status"));
    }

    private TableColumn<MonthlyAverageTableRow, String> column(String title, String property) {
        TableColumn<MonthlyAverageTableRow, String> column = new TableColumn<MonthlyAverageTableRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<MonthlyAverageTableRow, String>(property));
        return column;
    }

    private void configureFilters(List<Route> routes, List<MonthlyAverageTableRow> rows) {
        routeFilter.getItems().setAll(routeOptions(routes));
        routeFilter.getSelectionModel().selectFirst();

        yearFilter.getItems().setAll(textOptions(years(rows)));
        yearFilter.getSelectionModel().selectFirst();

        monthFilter.getItems().setAll(textOptions(months(rows)));
        monthFilter.getSelectionModel().selectFirst();

        routeFilter.setOnAction(event -> applyFilters());
        yearFilter.setOnAction(event -> applyFilters());
        monthFilter.setOnAction(event -> applyFilters());
    }

    private List<FilterOption> routeOptions(List<Route> routes) {
        List<FilterOption> options = new ArrayList<FilterOption>();
        options.add(FilterOption.all());
        List<Route> sortedRoutes = new ArrayList<Route>(routes);
        Collections.sort(sortedRoutes, new Comparator<Route>() {
            @Override
            public int compare(Route first, Route second) {
                int labelComparison = first.getDisplayName().compareToIgnoreCase(second.getDisplayName());
                if (labelComparison != 0) {
                    return labelComparison;
                }
                return Integer.compare(first.getId(), second.getId());
            }
        });
        for (Route route : sortedRoutes) {
            options.add(new FilterOption(Integer.valueOf(route.getId()), route.getDisplayName()));
        }
        return options;
    }

    private List<String> textOptions(List<String> values) {
        List<String> options = new ArrayList<String>();
        options.add(ALL);
        options.addAll(values);
        return options;
    }

    private List<String> years(List<MonthlyAverageTableRow> rows) {
        Map<Integer, String> values = new LinkedHashMap<Integer, String>();
        for (MonthlyAverageTableRow row : rows) {
            values.put(Integer.valueOf(row.getYear()), row.getYearText());
        }
        return sortedValues(values);
    }

    private List<String> months(List<MonthlyAverageTableRow> rows) {
        Map<Integer, String> values = new LinkedHashMap<Integer, String>();
        for (MonthlyAverageTableRow row : rows) {
            values.put(Integer.valueOf(row.getMonth()), row.getMonthText());
        }
        return sortedValues(values);
    }

    private List<String> sortedValues(Map<Integer, String> values) {
        List<Integer> keys = new ArrayList<Integer>(values.keySet());
        Collections.sort(keys);
        List<String> sorted = new ArrayList<String>();
        for (Integer key : keys) {
            sorted.add(values.get(key));
        }
        return sorted;
    }

    private void applyFilters() {
        FilterOption route = routeFilter.getValue();
        String year = yearFilter.getValue();
        String month = monthFilter.getValue();
        tableRows.clear();
        for (MonthlyAverageTableRow row : allRows) {
            if (route != null && route.routeId != null && row.getRouteId() != route.routeId.intValue()) {
                continue;
            }
            if (year != null && !ALL.equals(year) && !year.equals(row.getYearText())) {
                continue;
            }
            if (month != null && !ALL.equals(month) && !month.equals(row.getMonthText())) {
                continue;
            }
            tableRows.add(row);
        }
        showStatus("Resultados visibles: " + tableRows.size() + " de " + allRows.size());
    }

    private void showStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    private static final class FilterOption {
        private final Integer routeId;
        private final String label;

        private FilterOption(Integer routeId, String label) {
            this.routeId = routeId;
            this.label = label;
        }

        private static FilterOption all() {
            return new FilterOption(null, ALL);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
