package analytics.view.fx;

import analytics.controller.AnalyticsController;
import analytics.model.ActiveBusesByRouteStatistic;
import analytics.model.AlertPriorityStatistic;
import analytics.model.AnalyticsFilter;
import analytics.model.AnalyticsSelectionSummary;
import analytics.model.EventRouteStatistic;
import analytics.model.RouteSpeedAnalyticsRow;
import analytics.model.SystemAnalyticsSnapshot;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import monitoring.service.MonitoringStateListener;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFxPanel extends BorderPane {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AnalyticsController analyticsController;
    private final Stage stage = new Stage();
    private final Map<String, Label> summaryValues = new LinkedHashMap<String, Label>();
    private final Label lastRefreshLabel = new Label("Not refreshed yet");
    private final Label filterMessageLabel = new Label("Select a year to enable monthly analysis");
    private final Button refreshButton = new Button("Refresh Analytics");
    private final ComboBox<FilterOption> routeComboBox = new ComboBox<FilterOption>();
    private final ComboBox<FilterOption> yearComboBox = new ComboBox<FilterOption>();
    private final ComboBox<FilterOption> monthComboBox = new ComboBox<FilterOption>();
    private final TableView<RouteSpeedAnalyticsRow> routeSpeedTable = new TableView<RouteSpeedAnalyticsRow>();
    private final TableView<EventRouteStatistic> eventRouteTable = new TableView<EventRouteStatistic>();
    private final TableView<AlertPriorityStatistic> alertPriorityTable = new TableView<AlertPriorityStatistic>();
    private final TableView<ActiveBusesByRouteStatistic> activeBusesTable = new TableView<ActiveBusesByRouteStatistic>();
    private final PauseTransition refreshDelay = new PauseTransition(Duration.millis(350));
    private boolean refreshRunning;
    private boolean refreshQueued;
    private boolean updatingFilters;

    public AnalyticsFxPanel(AnalyticsController analyticsController) {
        this.analyticsController = analyticsController;
        buildLayout();
        configureStage();
        configureRefresh();
        analyticsController.addStateListener(new MonitoringStateListener() {
            @Override
            public void onMonitoringStateChanged() {
                scheduleRefresh();
            }
        });
        scheduleRefresh();
    }

    public void showView() {
        stage.show();
        stage.toFront();
        scheduleRefresh();
    }

    private void configureStage() {
        Scene scene = new Scene(this, 1120, 740);
        stage.setTitle("Analytics & Statistics");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
    }

    private void configureRefresh() {
        refreshDelay.setOnFinished(event -> refreshAnalytics());
        refreshButton.setOnAction(event -> refreshAnalytics());
        routeComboBox.setOnAction(event -> scheduleRefreshFromFilter());
        yearComboBox.setOnAction(event -> {
            FilterOption year = yearComboBox.getValue();
            monthComboBox.setDisable(year == null || year.getValue() == null);
            if (monthComboBox.isDisabled()) {
                monthComboBox.getSelectionModel().selectFirst();
            }
            scheduleRefreshFromFilter();
        });
        monthComboBox.setOnAction(event -> scheduleRefreshFromFilter());
    }

    private void buildLayout() {
        setStyle("-fx-background-color: #e2e8f0;");
        setTop(createHeader());

        VBox content = new VBox(14);
        content.setPadding(new Insets(18));
        content.getChildren().addAll(createFilterBar(), createSummaryGrid(),
                createTableSection("Route speed analytics", routeSpeedTable, 270), createSecondaryTables());

        configureRouteSpeedTable();
        configureEventRouteTable();
        configureAlertPriorityTable();
        configureActiveBusesTable();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scrollPane);
    }

    private HBox createHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Analytics & Statistics");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lastRefreshLabel.setTextFill(Color.web("#cbd5e1"));
        lastRefreshLabel.setFont(Font.font("System", 12));

        refreshButton.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");

        header.getChildren().addAll(title, spacer, lastRefreshLabel, refreshButton);
        return header;
    }

    private VBox createFilterBar() {
        VBox wrapper = new VBox(8);
        wrapper.setPadding(new Insets(12));
        wrapper.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        routeComboBox.setPrefWidth(190);
        yearComboBox.setPrefWidth(150);
        monthComboBox.setPrefWidth(170);
        monthComboBox.setDisable(true);

        row.getChildren().addAll(label("Route"), routeComboBox, label("Year"), yearComboBox,
                label("Month"), monthComboBox);

        filterMessageLabel.setTextFill(Color.web("#64748b"));
        filterMessageLabel.setFont(Font.font("System", 12));

        wrapper.getChildren().addAll(row, filterMessageLabel);
        return wrapper;
    }

    private GridPane createSummaryGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(createSummaryCard("Routes", "0", "Loaded routes"), 0, 0);
        grid.add(createSummaryCard("Active Buses", "0", "Current units"), 1, 0);
        grid.add(createSummaryCard("Events", "0", "Recent events"), 2, 0);
        grid.add(createSummaryCard("Alerts", "0", "Recent alerts"), 3, 0);
        grid.add(createSummaryCard("Monthly Avg Speed", "-", "Selected filter"), 4, 0);
        grid.add(createSummaryCard("Year Avg Speed", "-", "Selected year"), 5, 0);
        grid.add(createSummaryCard("Processed Datagrams", "0", "Samples used"), 6, 0);
        return grid;
    }

    private VBox createSummaryCard(String name, String value, String description) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setMinWidth(136);
        card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.web("#334155"));
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setWrapText(true);

        Label valueLabel = new Label(value);
        valueLabel.setTextFill(Color.web("#1e40af"));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        valueLabel.setWrapText(true);

        Label descriptionLabel = new Label(description);
        descriptionLabel.setTextFill(Color.web("#64748b"));
        descriptionLabel.setFont(Font.font("System", 11));

        summaryValues.put(name, valueLabel);
        card.getChildren().addAll(nameLabel, valueLabel, descriptionLabel);
        return card;
    }

    private GridPane createSecondaryTables() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.add(createTableSection("Events by route", eventRouteTable, 190), 0, 0);
        grid.add(createTableSection("Alerts by priority", alertPriorityTable, 190), 1, 0);
        grid.add(createTableSection("Active buses by route", activeBusesTable, 190), 0, 1, 2, 1);
        return grid;
    }

    private VBox createTableSection(String title, TableView<?> tableView, int minHeight) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#0f172a"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));

        tableView.setMinHeight(minHeight);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        section.getChildren().addAll(titleLabel, tableView);
        return section;
    }

    private void configureRouteSpeedTable() {
        routeSpeedTable.getColumns().clear();
        routeSpeedTable.getColumns().add(stringColumn("Route", value -> "Route " + value.getRouteId()));
        routeSpeedTable.getColumns().add(stringColumn("Year", RouteSpeedAnalyticsRow::getYearLabel));
        routeSpeedTable.getColumns().add(stringColumn("Month", RouteSpeedAnalyticsRow::getMonthLabel));
        routeSpeedTable.getColumns().add(stringColumn("Monthly Avg Speed", value -> speedOrDash(value.getMonthlyAverageSpeed())));
        routeSpeedTable.getColumns().add(stringColumn("Year Avg Speed", value -> speedOrDash(value.getYearlyAverageSpeed())));
        routeSpeedTable.getColumns().add(stringColumn("Processed Datagrams", value -> String.valueOf(value.getProcessedDatagrams())));
    }

    private void configureEventRouteTable() {
        eventRouteTable.getColumns().clear();
        eventRouteTable.getColumns().add(stringColumn("Route", value -> "Route " + value.getRouteId()));
        eventRouteTable.getColumns().add(stringColumn("Total Events", value -> String.valueOf(value.getTotalEvents())));
        eventRouteTable.getColumns().add(stringColumn("High", value -> String.valueOf(value.getHighPriorityEvents())));
        eventRouteTable.getColumns().add(stringColumn("Critical", value -> String.valueOf(value.getCriticalEvents())));
        eventRouteTable.getColumns().add(stringColumn("Most Common", EventRouteStatistic::getMostCommonType));
    }

    private void configureAlertPriorityTable() {
        alertPriorityTable.getColumns().clear();
        alertPriorityTable.getColumns().add(stringColumn("Priority", AlertPriorityStatistic::getPriority));
        alertPriorityTable.getColumns().add(stringColumn("Count", value -> String.valueOf(value.getCount())));
        alertPriorityTable.getColumns().add(stringColumn("Last Alert", AlertPriorityStatistic::getLastAlert));
        alertPriorityTable.getColumns().add(stringColumn("Time", value -> time(value.getLastAlertAt())));
    }

    private void configureActiveBusesTable() {
        activeBusesTable.getColumns().clear();
        activeBusesTable.getColumns().add(stringColumn("Route", value -> "Route " + value.getRouteId()));
        activeBusesTable.getColumns().add(stringColumn("Active Buses", value -> String.valueOf(value.getActiveBuses())));
        activeBusesTable.getColumns().add(stringColumn("Bus Codes", value -> truncate(value.getBusCodes(), 72)));
        activeBusesTable.getColumns().add(stringColumn("Last Position", value -> time(value.getLastPositionAt())));
    }

    private <T> TableColumn<T, String> stringColumn(String title, ValueFormatter<T> formatter) {
        TableColumn<T, String> column = new TableColumn<T, String>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(formatter.format(data.getValue())));
        return column;
    }

    private void scheduleRefreshFromFilter() {
        if (!updatingFilters) {
            scheduleRefresh();
        }
    }

    private void scheduleRefresh() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    scheduleRefresh();
                }
            });
            return;
        }
        refreshDelay.playFromStart();
    }

    private void refreshAnalytics() {
        if (refreshRunning) {
            refreshQueued = true;
            return;
        }
        refreshRunning = true;
        refreshButton.setDisable(true);
        lastRefreshLabel.setText("Refreshing...");
        AnalyticsFilter filter = currentFilter();

        Task<AnalyticsViewData> task = new Task<AnalyticsViewData>() {
            @Override
            protected AnalyticsViewData call() {
                SystemAnalyticsSnapshot snapshot = analyticsController.refreshSnapshot();
                List<Integer> routes = analyticsController.getAvailableRoutes();
                List<Integer> years = analyticsController.getAvailableYears();
                List<Integer> months = filter.getYear() == null
                        ? new ArrayList<Integer>()
                        : analyticsController.getAvailableMonthsForYear(filter.getYear().intValue());
                List<RouteSpeedAnalyticsRow> rows = analyticsController.getFilteredRouteSpeedAnalytics(filter);
                AnalyticsSelectionSummary summary = analyticsController.getSelectionSummary(filter);
                return new AnalyticsViewData(snapshot, routes, years, months, rows, summary, filter);
            }
        };
        task.setOnSucceeded(event -> {
            refreshRunning = false;
            refreshButton.setDisable(false);
            updateView(task.getValue());
            if (refreshQueued) {
                refreshQueued = false;
                scheduleRefresh();
            }
        });
        task.setOnFailed(event -> {
            refreshRunning = false;
            refreshButton.setDisable(false);
            lastRefreshLabel.setText("Refresh failed");
        });

        Thread thread = new Thread(task, "analytics-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private AnalyticsFilter currentFilter() {
        FilterOption route = routeComboBox.getValue();
        FilterOption year = yearComboBox.getValue();
        FilterOption month = monthComboBox.getValue();
        Integer yearValue = year == null ? null : year.getValue();
        Integer monthValue = month == null || yearValue == null ? null : month.getValue();
        return new AnalyticsFilter(route == null ? null : route.getValue(), yearValue, monthValue);
    }

    private void updateView(AnalyticsViewData data) {
        if (data == null || data.getSnapshot() == null) {
            lastRefreshLabel.setText("No analytics data");
            return;
        }
        updateFilters(data);
        updateSummary(data);
        routeSpeedTable.getItems().setAll(data.getRows());
        eventRouteTable.getItems().setAll(data.getSnapshot().getEventRouteStatistics());
        alertPriorityTable.getItems().setAll(data.getSnapshot().getAlertPriorityStatistics());
        activeBusesTable.getItems().setAll(data.getSnapshot().getActiveBusesByRouteStatistics());
        lastRefreshLabel.setText("Updated " + time(data.getSnapshot().getRefreshedAt()));
    }

    private void updateFilters(AnalyticsViewData data) {
        updatingFilters = true;
        setOptions(routeComboBox, "All routes", data.getRoutes(), data.getFilter().getRouteId(), "Route ");
        setOptions(yearComboBox, "All years", data.getYears(), data.getFilter().getYear(), "");
        monthComboBox.setDisable(data.getFilter().getYear() == null);
        setOptions(monthComboBox, "All months", data.getMonths(), data.getFilter().getMonth(), "");
        if (monthComboBox.isDisabled()) {
            monthComboBox.getSelectionModel().selectFirst();
            filterMessageLabel.setText("Select a year to enable monthly analysis");
        } else {
            filterMessageLabel.setText("Month filter is scoped to the selected year");
        }
        updatingFilters = false;
    }

    private void setOptions(ComboBox<FilterOption> comboBox, String allLabel, List<Integer> values,
                            Integer selectedValue, String prefix) {
        comboBox.getItems().clear();
        comboBox.getItems().add(new FilterOption(allLabel, null));
        for (Integer value : values) {
            String label = prefix.length() == 0 && comboBox == monthComboBox
                    ? monthName(value.intValue())
                    : prefix + value;
            comboBox.getItems().add(new FilterOption(label, value));
        }
        int selectedIndex = 0;
        if (selectedValue != null) {
            for (int i = 0; i < comboBox.getItems().size(); i++) {
                FilterOption option = comboBox.getItems().get(i);
                if (selectedValue.equals(option.getValue())) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        comboBox.getSelectionModel().select(selectedIndex);
    }

    private void updateSummary(AnalyticsViewData data) {
        SystemAnalyticsSnapshot snapshot = data.getSnapshot();
        AnalyticsSelectionSummary summary = data.getSummary();
        setSummary("Routes", String.valueOf(snapshot.getTotalRoutes()));
        setSummary("Active Buses", String.valueOf(snapshot.getTotalActiveBuses()));
        setSummary("Events", String.valueOf(snapshot.getTotalEvents()));
        setSummary("Alerts", String.valueOf(snapshot.getTotalAlerts()));
        setSummary("Monthly Avg Speed", speedOrDash(summary.getMonthlyAverageSpeed()));
        setSummary("Year Avg Speed", speedOrDash(summary.getYearlyAverageSpeed()));
        setSummary("Processed Datagrams", String.valueOf(summary.getProcessedDatagrams()));
    }

    private void setSummary(String key, String value) {
        Label label = summaryValues.get(key);
        if (label != null) {
            label.setText(value);
        }
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#334155"));
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        return label;
    }

    private String speedOrDash(Double value) {
        return value == null ? "-" : String.format(Locale.US, "%.1f", value.doubleValue());
    }

    private String time(LocalDateTime value) {
        return value == null ? "No data" : TIME_FORMATTER.format(value);
    }

    private String monthName(int month) {
        String value = Month.of(month).name().toLowerCase();
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private interface ValueFormatter<T> {
        String format(T value);
    }

    private static class FilterOption {
        private final String label;
        private final Integer value;

        private FilterOption(String label, Integer value) {
            this.label = label;
            this.value = value;
        }

        private Integer getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class AnalyticsViewData {
        private final SystemAnalyticsSnapshot snapshot;
        private final List<Integer> routes;
        private final List<Integer> years;
        private final List<Integer> months;
        private final List<RouteSpeedAnalyticsRow> rows;
        private final AnalyticsSelectionSummary summary;
        private final AnalyticsFilter filter;

        private AnalyticsViewData(SystemAnalyticsSnapshot snapshot, List<Integer> routes, List<Integer> years,
                                  List<Integer> months, List<RouteSpeedAnalyticsRow> rows,
                                  AnalyticsSelectionSummary summary, AnalyticsFilter filter) {
            this.snapshot = snapshot;
            this.routes = new ArrayList<Integer>(routes);
            this.years = new ArrayList<Integer>(years);
            this.months = new ArrayList<Integer>(months);
            this.rows = new ArrayList<RouteSpeedAnalyticsRow>(rows);
            this.summary = summary;
            this.filter = filter;
        }

        private SystemAnalyticsSnapshot getSnapshot() {
            return snapshot;
        }

        private List<Integer> getRoutes() {
            return routes;
        }

        private List<Integer> getYears() {
            return years;
        }

        private List<Integer> getMonths() {
            return months;
        }

        private List<RouteSpeedAnalyticsRow> getRows() {
            return rows;
        }

        private AnalyticsSelectionSummary getSummary() {
            return summary;
        }

        private AnalyticsFilter getFilter() {
            return filter;
        }
    }
}
