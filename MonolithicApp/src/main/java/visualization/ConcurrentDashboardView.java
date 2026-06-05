package visualization;

import domain.Datagram;
import domain.MonthlyRouteAverage;
import domain.Route;
import domain.RouteMonthKey;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import processing.AverageSpeedProcessingResult;
import processing.benchmark.ProcessingMetrics;

import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConcurrentDashboardView {
    private static final String BACKGROUND = "#eef3f8";
    private static final String PANEL = "#ffffff";
    private static final String BORDER = "#d7e0ea";
    private static final String TEXT = "#18212f";
    private static final String MUTED = "#64748b";
    private static final String ACCENT = "#0f766e";
    private static final String ALL = "Todos";
    private static final DateTimeFormatter POPUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BorderPane root;
    private final Label statusLabel;
    private final VBox metricsCard;
    private final ConcurrentMapView mapView;
    private final ComboBox<FilterOption> routeFilter;
    private final ComboBox<String> yearFilter;
    private final ComboBox<String> monthFilter;
    private final Button clearFiltersButton;
    private final TableView<MonthlyAverageTableRow> table;
    private final ObservableList<MonthlyAverageTableRow> tableRows;
    private final List<MonthlyAverageTableRow> allRows;
    private final NumberFormat integerFormat;
    private final NumberFormat decimalFormat;

    public ConcurrentDashboardView() {
        this.root = new BorderPane();
        this.statusLabel = new Label();
        this.metricsCard = new VBox(10);
        this.mapView = new ConcurrentMapView();
        this.routeFilter = new ComboBox<FilterOption>();
        this.yearFilter = new ComboBox<String>();
        this.monthFilter = new ComboBox<String>();
        this.clearFiltersButton = new Button("Limpiar filtros");
        this.table = new TableView<MonthlyAverageTableRow>();
        this.tableRows = FXCollections.observableArrayList();
        this.allRows = new ArrayList<MonthlyAverageTableRow>();
        this.integerFormat = NumberFormat.getIntegerInstance(Locale.US);
        this.decimalFormat = NumberFormat.getNumberInstance(Locale.US);
        this.decimalFormat.setMaximumFractionDigits(2);
        this.decimalFormat.setMinimumFractionDigits(2);
        configureRoot();
        configureTable();
        showLoadingState("Cargando datos...");
    }

    public Parent createContent() {
        return root;
    }

    public void showLoadingState(String message) {
        updateMetrics(ProcessingMetrics.empty(), "Calculando...");
        showStatus(message);
        mapView.stopPlayback();
        mapView.clearMarkers();
        allRows.clear();
        tableRows.clear();
        configureFilters(Collections.<Route>emptyList(), Collections.<MonthlyAverageTableRow>emptyList());
    }

    public void showProcessingState(String message) {
        updateMetrics(ProcessingMetrics.empty(), "Procesando...");
        showStatus(message);
    }

    public void showResult(AverageSpeedProcessingResult result) {
        showResult(result, Collections.<Datagram>emptyList());
    }

    public void showResult(AverageSpeedProcessingResult result, List<Datagram> datagrams) {
        updateMetrics(result.getMetrics(), "Calculo completado");
        allRows.clear();
        for (MonthlyRouteAverage average : result.getAverages()) {
            allRows.add(new MonthlyAverageTableRow(average));
        }
        configureFilters(result.getRoutes(), allRows);
        applyFilters();
        mapView.startPlayback(buildPlaybackPoints(datagrams, result));
        showStatus("Calculo completado");
    }

    public void showError(String message) {
        updateMetrics(ProcessingMetrics.empty(), "Error");
        mapView.stopPlayback();
        mapView.clearMarkers();
        allRows.clear();
        tableRows.clear();
        configureFilters(Collections.<Route>emptyList(), Collections.<MonthlyAverageTableRow>emptyList());
        showStatus(message);
    }

    private void configureRoot() {
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");
        root.setTop(createHeader());
        root.setCenter(createDashboardBody());
    }

    private Parent createHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(18, 22, 12, 22));

        Label title = new Label("SITM-MIO V2 Concurrente - Fork/Join");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");

        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + MUTED + ";");
        header.getChildren().addAll(title, statusLabel);
        return header;
    }

    private Parent createDashboardBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(0, 22, 22, 22));

        HBox mainArea = new HBox(14);
        mapView.setPrefHeight(390);
        HBox.setHgrow(mapView, Priority.ALWAYS);

        metricsCard.setPrefWidth(360);
        metricsCard.setMinWidth(320);
        metricsCard.setMaxWidth(420);
        updateMetrics(ProcessingMetrics.empty(), "Calculando...");

        mainArea.getChildren().addAll(mapView, metricsCard);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        VBox lowerArea = new VBox(10);
        lowerArea.getChildren().addAll(createFilters(), table);
        VBox.setVgrow(table, Priority.ALWAYS);

        body.getChildren().addAll(mainArea, lowerArea);
        VBox.setVgrow(lowerArea, Priority.ALWAYS);
        return body;
    }

    private Parent createFilters() {
        HBox filters = new HBox(10);
        filters.setPadding(new Insets(12));
        filters.setStyle(panelStyle());
        filters.getChildren().addAll(
                createFilterBlock("Ruta", routeFilter, 240),
                createFilterBlock("Anio", yearFilter, 130),
                createFilterBlock("Mes", monthFilter, 130),
                createButtonBlock(clearFiltersButton)
        );
        clearFiltersButton.setOnAction(event -> clearFilters());
        return filters;
    }

    private Parent createFilterBlock(String labelText, ComboBox<?> comboBox, double width) {
        VBox block = new VBox(4);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        comboBox.setPrefWidth(width);
        block.getChildren().addAll(label, comboBox);
        return block;
    }

    private Parent createButtonBlock(Button button) {
        VBox block = new VBox(4);
        Label spacer = new Label(" ");
        button.setStyle("-fx-background-color: #e6f4f1; -fx-text-fill: " + ACCENT + "; -fx-font-weight: 700;");
        block.getChildren().addAll(spacer, button);
        return block;
    }

    private void updateMetrics(ProcessingMetrics metrics, String state) {
        metricsCard.getChildren().clear();
        metricsCard.setPadding(new Insets(16));
        metricsCard.setStyle(panelStyle());

        Label title = new Label("Procesamiento V2 (Fork/Join)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");
        Label subtitle = new Label("Nucleo concurrente - odometro + tiempo");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        Label stateLabel = new Label(state);
        stateLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + ACCENT + ";");

        VBox mainMetrics = new VBox(7);
        mainMetrics.getChildren().addAll(
                metricRow("Tiempo Fork/Join", formatMillis(metrics.getForkJoinProcessingTimeMillis())),
                metricRow("Tiempo total", formatMillis(metrics.getTotalProcessingTimeMillis())),
                metricRow("Throughput datagramas/s", formatDecimal(metrics.getThroughputDatagramsPerSecond())),
                metricRow("Throughput intervalos/s", formatDecimal(metrics.getThroughputIntervalsPerSecond())),
                metricRow("Datagramas leidos", formatInteger(metrics.getReadDatagrams())),
                metricRow("Intervalos validos", formatInteger(metrics.getValidIntervals())),
                metricRow("Buses procesados", formatInteger(metrics.getProcessedBuses())),
                metricRow("Rutas con resultado", formatInteger(metrics.getRoutesWithResult()) + " / " + formatInteger(metrics.getLoadedRoutes())),
                metricRow("Rutas sin datos", formatInteger(metrics.getRoutesWithoutData())),
                metricRow("Velocidad global", formatDecimal(metrics.getGlobalAverageSpeedKmh()) + " km/h"),
                metricRow("Parallelism", String.valueOf(metrics.getParallelism())),
                metricRow("Threshold", String.valueOf(metrics.getThreshold())),
                metricRow("Dataset", shortDataset(metrics.getDatasetName()))
        );

        VBox discards = new VBox(6);
        discards.getChildren().addAll(
                sectionLabel("Descartes"),
                metricRow("Sin ruta/inactiva", formatInteger(metrics.getNoRouteOrInactive())),
                metricRow("Sin punto previo", formatInteger(metrics.getDiscardNoPreviousPoint())),
                metricRow("Cambio de ruta", formatInteger(metrics.getDiscardRouteChanged())),
                metricRow("dt <= 0", formatInteger(metrics.getDiscardDeltaTimeInvalid())),
                metricRow("dt > 600s", formatInteger(metrics.getDiscardDeltaTimeTooLong())),
                metricRow("Odometro invalido", formatInteger(metrics.getDiscardBadOdometer())),
                metricRow("Sin avance", formatInteger(metrics.getDiscardNoDistanceGain())),
                metricRow("Velocidad > 120 km/h", formatInteger(metrics.getDiscardSpeedTooHigh()))
        );

        ScrollPane scroll = new ScrollPane(new VBox(10, mainMetrics, new Separator(), discards));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        metricsCard.getChildren().addAll(title, subtitle, stateLabel, scroll);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");
        return label;
    }

    private Parent metricRow(String name, String value) {
        HBox row = new HBox(8);
        Label label = new Label(name);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        Label metric = new Label(value);
        metric.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + TEXT + ";");
        HBox.setHgrow(label, Priority.ALWAYS);
        row.getChildren().addAll(label, metric);
        return row;
    }

    private void configureTable() {
        table.getColumns().clear();
        table.getColumns().add(column("Ruta", "route"));
        table.getColumns().add(column("Anio", "yearText"));
        table.getColumns().add(column("Mes", "monthText"));
        table.getColumns().add(column("Velocidad promedio", "averageSpeed"));
        table.getColumns().add(column("Intervalos validos", "intervalCount"));
        table.getColumns().add(column("Estado", "status"));
        table.setItems(tableRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Sin resultados para mostrar"));
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

    private void clearFilters() {
        routeFilter.getSelectionModel().selectFirst();
        yearFilter.getSelectionModel().selectFirst();
        monthFilter.getSelectionModel().selectFirst();
        applyFilters();
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
        showStatus("Resultados visibles: " + formatInteger(tableRows.size()) + " de " + formatInteger(allRows.size()));
    }

    private List<MapPlaybackPoint> buildPlaybackPoints(List<Datagram> datagrams, AverageSpeedProcessingResult result) {
        if (datagrams == null || datagrams.isEmpty()) {
            return Collections.<MapPlaybackPoint>emptyList();
        }

        RouteDisplayService routeDisplayService = new RouteDisplayService(result.getRoutes());
        Map<RouteMonthKey, MonthlyRouteAverage> averagesByKey = new HashMap<RouteMonthKey, MonthlyRouteAverage>();
        for (MonthlyRouteAverage average : result.getAverages()) {
            averagesByKey.put(new RouteMonthKey(average.getRouteId(), average.getYear(), average.getMonth()), average);
        }

        List<Datagram> sorted = new ArrayList<Datagram>();
        for (Datagram datagram : datagrams) {
            if (isValidMapDatagram(datagram)) {
                sorted.add(datagram);
            }
        }
        Collections.sort(sorted, new Comparator<Datagram>() {
            @Override
            public int compare(Datagram first, Datagram second) {
                return first.getTimestamp().compareTo(second.getTimestamp());
            }
        });

        List<MapPlaybackPoint> points = new ArrayList<MapPlaybackPoint>();
        for (Datagram datagram : sorted) {
            RouteMonthKey key = RouteMonthKey.from(datagram);
            MonthlyRouteAverage average = averagesByKey.get(key);
            String averageText = average == null || !average.hasData()
                    ? "Sin datos"
                    : formatDecimal(average.getAverageSpeed()) + " km/h";
            points.add(new MapPlaybackPoint(
                    datagram.getBusId(),
                    datagram.getRouteId(),
                    routeDisplayService.labelFor(datagram.getRouteId()),
                    datagram.getLatitude().doubleValue(),
                    datagram.getLongitude().doubleValue(),
                    POPUP_TIME_FORMAT.format(datagram.getTimestamp()),
                    averageText
            ));
        }
        return points;
    }

    private boolean isValidMapDatagram(Datagram datagram) {
        if (datagram == null || datagram.getRouteId() == -1 || datagram.getTimestamp() == null || !datagram.hasPosition()) {
            return false;
        }
        double latitude = datagram.getLatitude().doubleValue();
        double longitude = datagram.getLongitude().doubleValue();
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && !Double.isInfinite(latitude)
                && !Double.isInfinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }

    private void showStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    private String formatMillis(long millis) {
        return formatInteger(millis) + " ms";
    }

    private String formatInteger(long value) {
        return integerFormat.format(value);
    }

    private String formatDecimal(double value) {
        return decimalFormat.format(value);
    }

    private String shortDataset(String dataset) {
        if (dataset == null || dataset.trim().isEmpty()) {
            return "Not loaded";
        }
        try {
            return Paths.get(dataset).getFileName().toString();
        } catch (RuntimeException exception) {
            return dataset;
        }
    }

    private String panelStyle() {
        return "-fx-background-color: " + PANEL + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;";
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
