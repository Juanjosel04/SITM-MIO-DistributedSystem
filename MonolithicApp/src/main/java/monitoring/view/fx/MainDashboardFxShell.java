package monitoring.view.fx;

import analytics.controller.AnalyticsController;
import analytics.view.fx.AnalyticsFxPanel;
import core.speed.MonolithicSpeedJob;
import core.speed.RouteCatalog;
import core.speed.RouteMonthAccumulator;
import events.model.OperationalEvent;
import events.service.EventService;
import events.view.fx.BusConsoleFxView;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import monitoring.controller.MonitoringController;
import monitoring.map.fx.MapFxView;
import monitoring.model.AlertPanelModel;
import monitoring.model.BusMarker;
import monitoring.model.MonitoringMetric;
import monitoring.service.MonitoringStateListener;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainDashboardFxShell extends BorderPane {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Configurable via -Dsitm.speed.datagrams=... / -Dsitm.speed.lines=...
    private static final String V1_DATAGRAMS = System.getProperty(
            "sitm.speed.datagrams", "datasets/mini/datagrams-MiniPilot.csv");
    private static final String V1_LINES = System.getProperty(
            "sitm.speed.lines", "datasets/mini/lines-241-ActiveGT.csv");

    private final MonitoringController controller;
    private final EventService eventService;
    private final AnalyticsController analyticsController;
    private final Runnable logoutAction;
    private final Label statusLabel = new Label("Idle");
    private final MapFxView mapView;
    private final Map<String, Label> metricValues = new LinkedHashMap<String, Label>();
    // Lists kept for data refresh logic (not shown in the center panel)
    private final ListView<String> busList = new ListView<String>();
    private final ListView<String> alertList = new ListView<String>();
    private final ListView<String> eventList = new ListView<String>();
    private final ListView<String> logList = new ListView<String>();
    private final List<String> recentLogs = new ArrayList<String>();

    // ── V1 card state ────────────────────────────────────────────────────────
    private volatile boolean v1Started       = false;
    private final Label v1StatusLabel        = new Label("Calculando...");
    private final Label v1ComputeMsLabel     = new Label("–");
    private final Label v1ThroughputLabel    = new Label("–");
    private final Label v1TotalReadLabel     = new Label("–");
    private final Label v1ValidIntervals     = new Label("–");
    private final Label v1RoutesResult       = new Label("–");
    private final Label v1RoutesSinDatos     = new Label("–");
    private final Label v1AvgSpeed           = new Label("–");
    private final Label v1DatasetLabel       = new Label("–");

    public MainDashboardFxShell(MonitoringController controller, EventService eventService,
                                AnalyticsController analyticsController) {
        this(controller, eventService, analyticsController, null);
    }

    public MainDashboardFxShell(MonitoringController controller, EventService eventService,
                                AnalyticsController analyticsController, Runnable logoutAction) {
        this.controller = controller;
        this.eventService = eventService;
        this.analyticsController = analyticsController;
        this.logoutAction = logoutAction;
        this.mapView = new MapFxView(controller);
        buildLayout();
        controller.addStateListener(new MonitoringStateListener() {
            @Override
            public void onMonitoringStateChanged() {
                refresh();
            }
        });
        addLog("JavaFX dashboard initialized");
        refresh();
    }

    private void buildLayout() {
        setStyle("-fx-background-color: #e2e8f0;");
        setTop(createTopSection());
        setCenter(createCenterSection());
        setBottom(createLogSection());
    }

    private VBox createTopSection() {
        VBox top = new VBox(14);
        top.getChildren().addAll(createHeader(), createMetricsGrid());
        return top;
    }

    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("SITM-MIO Monitoring Center");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 26));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button consoleButton = new Button("Open Bus Console");
        consoleButton.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
        consoleButton.setOnAction(event -> openBusConsole());

        Button analyticsButton = new Button("Open Analytics");
        analyticsButton.setStyle("-fx-background-color: #bfdbfe; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
        analyticsButton.setOnAction(event -> openAnalytics());

        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setPadding(new Insets(8, 16, 8, 16));
        statusLabel.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 4;");

        header.getChildren().addAll(title, spacer, analyticsButton, consoleButton, statusLabel);
        if (logoutAction != null) {
            Button logoutButton = new Button("Logout");
            logoutButton.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
            logoutButton.setOnAction(event -> logoutAction.run());
            header.getChildren().add(logoutButton);
        }
        return header;
    }

    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 18, 0, 18));
        grid.setHgap(12);
        grid.setVgap(12);

        List<MonitoringMetric> metrics = controller.getMetrics();
        for (int i = 0; i < metrics.size(); i++) {
            MonitoringMetric metric = metrics.get(i);
            grid.add(createMetricCard(metric), i, 0);
        }
        return grid;
    }

    private VBox createMetricCard(MonitoringMetric metric) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(14));
        card.setMinWidth(128);
        card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label name = new Label(metric.getName());
        name.setTextFill(Color.web("#334155"));
        name.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label value = new Label(metric.getValue());
        value.setTextFill(Color.web("#1e40af"));
        value.setFont(Font.font("System", FontWeight.BOLD, 25));

        Label description = new Label(metric.getDescription());
        description.setTextFill(Color.web("#64748b"));
        description.setFont(Font.font("System", 11));

        metricValues.put(metric.getName(), value);
        card.getChildren().addAll(name, value, description);
        return card;
    }

    // Map fills available width; V1 card keeps its preferred/minimum width.
    private HBox createCenterSection() {
        HBox center = new HBox(14);
        center.setPadding(new Insets(18));
        center.getChildren().addAll(mapView, createV1Panel());
        HBox.setHgrow(center.getChildren().get(0), Priority.ALWAYS);
        return center;
    }

    // ── V1 summary card ──────────────────────────────────────────────────────

    private VBox createV1Panel() {
        VBox card = new VBox(10);
        card.setMinWidth(380);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1;"
                + " -fx-border-radius: 4; -fx-background-radius: 4;");

        Label title = new Label("Procesamiento V1 (monolítica)");
        title.setTextFill(Color.web("#0f172a"));
        title.setFont(Font.font("System", FontWeight.BOLD, 15));

        Label subtitle = new Label("Núcleo headless · sin pipeline de monitoreo");
        subtitle.setTextFill(Color.web("#64748b"));
        subtitle.setFont(Font.font("System", 11));

        v1StatusLabel.setTextFill(Color.web("#2563eb"));
        v1StatusLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        card.getChildren().addAll(title, subtitle, new Separator(), v1StatusLabel, buildV1MetricsGrid());

        if (!v1Started) {
            v1Started = true;
            runV1Computation();
        }
        return card;
    }

    private GridPane buildV1MetricsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(9);
        grid.setPadding(new Insets(4, 0, 0, 0));

        styleV1Value(v1ComputeMsLabel);
        styleV1Value(v1ThroughputLabel);
        styleV1Value(v1TotalReadLabel);
        styleV1Value(v1ValidIntervals);
        styleV1Value(v1RoutesResult);
        styleV1Value(v1RoutesSinDatos);
        styleV1Value(v1AvgSpeed);
        styleV1Value(v1DatasetLabel);

        addV1Row(grid, 0, "Tiempo cómputo:",        v1ComputeMsLabel);
        addV1Row(grid, 1, "Throughput:",             v1ThroughputLabel);
        addV1Row(grid, 2, "Datagramas procesados:",  v1TotalReadLabel);
        addV1Row(grid, 3, "Intervalos válidos:",     v1ValidIntervals);
        addV1Row(grid, 4, "Rutas con resultado:",    v1RoutesResult);
        addV1Row(grid, 5, "Rutas sin datos:",        v1RoutesSinDatos);
        addV1Row(grid, 6, "Velocidad prom. global:", v1AvgSpeed);
        addV1Row(grid, 7, "Dataset:",                v1DatasetLabel);

        return grid;
    }

    private static void addV1Row(GridPane grid, int row, String text, Label valueLabel) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#334155"));
        lbl.setFont(Font.font("System", 12));
        grid.add(lbl, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private static void styleV1Value(Label label) {
        label.setTextFill(Color.web("#1e40af"));
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
    }

    private void runV1Computation() {
        Task<Object[]> task = new Task<Object[]>() {
            @Override
            protected Object[] call() throws Exception {
                File datagramsFile = new File(V1_DATAGRAMS);
                if (!datagramsFile.exists()) {
                    return null; // signals "file not found" to the success handler
                }
                RouteCatalog catalog = RouteCatalog.load(V1_LINES);
                MonolithicSpeedJob job = new MonolithicSpeedJob(catalog);
                long t0 = System.currentTimeMillis();
                job.run(V1_DATAGRAMS);
                long computeMs = System.currentTimeMillis() - t0;
                return new Object[]{ catalog, job, computeMs };
            }
        };

        task.setOnSucceeded(event -> {
            Object[] result = task.getValue();
            Platform.runLater(() -> {
                if (result == null) {
                    v1StatusLabel.setTextFill(Color.web("#dc2626"));
                    v1StatusLabel.setText("Dataset no encontrado: " + new File(V1_DATAGRAMS).getName());
                } else {
                    updateV1Card((RouteCatalog) result[0], (MonolithicSpeedJob) result[1], (Long) result[2]);
                }
            });
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                v1StatusLabel.setTextFill(Color.web("#dc2626"));
                v1StatusLabel.setText("Error: " + (ex != null ? ex.getMessage() : "desconocido"));
            });
        });

        Thread thread = new Thread(task, "v1-speed-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateV1Card(RouteCatalog catalog, MonolithicSpeedJob job, long computeMs) {
        MonolithicSpeedJob.Counters c = job.counters();
        Map<String, RouteMonthAccumulator> accs = job.accumulators();

        long throughput = computeMs > 0 ? (c.totalRead * 1000L / computeMs) : 0;

        Set<Integer> routesWithData = new HashSet<Integer>();
        double totalDist = 0.0, totalTime = 0.0;
        for (Map.Entry<String, RouteMonthAccumulator> e : accs.entrySet()) {
            String key = e.getKey();
            int us = key.indexOf('_');
            if (us > 0) {
                try { routesWithData.add(Integer.parseInt(key.substring(0, us))); }
                catch (NumberFormatException ignored) {}
            }
            totalDist += e.getValue().sumDistanceMeters();
            totalTime += e.getValue().sumTimeSeconds();
        }
        int withResult = routesWithData.size();
        int sinDatos   = Math.max(0, catalog.size() - withResult);
        double avgKmh  = totalTime > 0.0 ? (totalDist / totalTime) * 3.6 : 0.0;

        v1StatusLabel.setTextFill(Color.web("#16a34a"));
        v1StatusLabel.setText("Cálculo completado");
        v1ComputeMsLabel.setText(String.format(Locale.ROOT, "%,d ms", computeMs));
        v1ThroughputLabel.setText(String.format(Locale.ROOT, "%,d d/s", throughput));
        v1TotalReadLabel.setText(String.format(Locale.ROOT, "%,d", c.totalRead));
        v1ValidIntervals.setText(String.format(Locale.ROOT, "%,d", c.validIntervals));
        v1RoutesResult.setText(withResult + " / " + catalog.size());
        v1RoutesSinDatos.setText(String.valueOf(sinDatos));
        v1AvgSpeed.setText(String.format(Locale.ROOT, "%.2f km/h", avgKmh));
        v1DatasetLabel.setText(new File(V1_DATAGRAMS).getName());
    }

    // ── Existing sections (unchanged) ─────────────────────────────────────────

    private VBox createSection(String title, ListView<String> listView) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#0f172a"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));

        listView.setFocusTraversable(false);
        listView.setFixedCellSize(44);
        listView.setStyle("-fx-control-inner-background: white; -fx-font-size: 12px;");
        listView.setPlaceholder(new Label("Waiting for updates"));
        listView.setCellFactory(view -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setPadding(new Insets(7, 8, 7, 8));
                setTextFill(Color.web("#1f2937"));
                setWrapText(false);
            }
        });

        section.getChildren().addAll(titleLabel, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return section;
    }

    private VBox createLogSection() {
        VBox section = createSection("System activity", logList);
        section.setPadding(new Insets(12));
        BorderPane.setMargin(section, new Insets(0, 18, 18, 18));
        logList.setPrefHeight(120);
        return section;
    }

    private void openBusConsole() {
        BusConsoleFxView console = new BusConsoleFxView(eventService, controller);
        console.showView();
        addLog("JavaFX bus console opened");
    }

    private void openAnalytics() {
        AnalyticsFxPanel analyticsPanel = new AnalyticsFxPanel(analyticsController);
        analyticsPanel.showView();
        addLog("Analytics view opened");
    }

    private void refresh() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                refreshMetrics();
                refreshBuses();
                refreshAlerts();
                refreshEvents();
                refreshLogs();
            }
        });
    }

    private void refreshMetrics() {
        statusLabel.setText(controller.getStreamStatus());
        for (MonitoringMetric metric : controller.getMetrics()) {
            Label value = metricValues.get(metric.getName());
            if (value != null) {
                value.setText(metric.getValue());
            }
        }
    }

    private void refreshBuses() {
        List<String> rows = new ArrayList<String>();
        for (BusMarker bus : controller.getCurrentBuses()) {
            String speed = bus.getSpeed() == null ? "N/A" : String.format("%.1f", bus.getSpeed());
            String time = bus.getLastUpdate() == null ? "" : TIME_FORMATTER.format(bus.getLastUpdate());
            rows.add(bus.getBusCode() + " | route " + controller.getRouteDisplayName(bus.getRouteId()) + " | " +
                    String.format("%.5f", bus.getLatitude()) + ", " +
                    String.format("%.5f", bus.getLongitude()) + " | speed " + speed + " | " + time);
        }
        busList.getItems().setAll(rows);
        mapView.updateBuses(controller.getCurrentBuses());
    }

    private void refreshAlerts() {
        List<String> rows = new ArrayList<String>();
        for (AlertPanelModel alert : controller.getAlerts()) {
            String time = alert.getTimestamp() == null ? "" : TIME_FORMATTER.format(alert.getTimestamp());
            rows.add(alert.getLevel().name() + " | " + alert.getTitle() + " | " + alert.getMessage() + " | " + time);
        }
        alertList.getItems().setAll(rows);
    }

    private void refreshEvents() {
        List<String> rows = new ArrayList<String>();
        for (OperationalEvent event : controller.getRecentEvents()) {
            String bus = event.getBusCode() == null ? "N/A" : event.getBusCode();
            String time = event.getTimestamp() == null ? "" : TIME_FORMATTER.format(event.getTimestamp());
            rows.add(event.getEventType().name() + " | " + event.getPriority().name() + " | " +
                    event.getSourceType().name() + " | bus " + bus + " | route " +
                    controller.getRouteFilterLabel(event.getRouteId()) + " | " + time);
        }
        eventList.getItems().setAll(rows);
    }

    private void refreshLogs() {
        addLog("Status " + controller.getStreamStatus() +
                " | buses " + controller.getCurrentBuses().size() +
                " | alerts " + controller.getAlerts().size() +
                " | events " + controller.getRecentEvents().size());
        logList.getItems().setAll(recentLogs);
    }

    private void addLog(String message) {
        String row = TIME_FORMATTER.format(java.time.LocalTime.now()) + "  " + message;
        recentLogs.add(0, row);
        if (recentLogs.size() > 12) {
            recentLogs.remove(recentLogs.size() - 1);
        }
    }
}
