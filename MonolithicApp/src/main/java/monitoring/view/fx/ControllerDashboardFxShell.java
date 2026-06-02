package monitoring.view.fx;

import analytics.controller.AnalyticsController;
import analytics.view.fx.AnalyticsFxPanel;
import events.model.OperationalEvent;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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
import monitoring.service.MonitoringStateListener;
import security.model.AccessScope;
import security.model.AuthenticatedUser;
import security.session.UserSession;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ControllerDashboardFxShell extends BorderPane {
    private static final DateTimeFormatter LOGIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ROW_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final UserSession session;
    private final MonitoringController monitoringController;
    private final AnalyticsController analyticsController;
    private final Runnable logoutAction;
    private final MapFxView mapView;
    private final Label routeCountValue = summaryValue("-");
    private final Label busCountValue = summaryValue("-");
    private final Label eventCountValue = summaryValue("-");
    private final Label alertCountValue = summaryValue("-");
    private final Label accessValue = summaryValue("-");
    private final ListView<String> busList = new ListView<String>();
    private final ListView<String> eventList = new ListView<String>();
    private final ListView<String> alertList = new ListView<String>();

    public ControllerDashboardFxShell(UserSession session, Runnable logoutAction) {
        this(session, null, null, logoutAction);
    }

    public ControllerDashboardFxShell(UserSession session, MonitoringController monitoringController,
                                      Runnable logoutAction) {
        this(session, monitoringController, null, logoutAction);
    }

    public ControllerDashboardFxShell(UserSession session, MonitoringController monitoringController,
                                      AnalyticsController analyticsController, Runnable logoutAction) {
        this.session = session;
        this.monitoringController = monitoringController;
        this.analyticsController = analyticsController;
        this.logoutAction = logoutAction;
        this.mapView = new MapFxView(monitoringController);
        buildLayout();
        registerRefreshListener();
        refresh();
    }

    private void buildLayout() {
        setStyle("-fx-background-color: #e2e8f0;");
        setTop(createHeader());
        setCenter(createContent());
    }

    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #0f172a;");

        VBox identity = new VBox(4);
        Label title = new Label("SITM-MIO - Panel del Controlador");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label userLine = new Label(userName());
        userLine.setTextFill(Color.web("#e2e8f0"));
        userLine.setFont(Font.font("System", FontWeight.BOLD, 13));

        Label sessionLine = new Label("Cedula: " + nationalId() + " | Rol: " + roleName() + " | Inicio: " + loginTime());
        sessionLine.setTextFill(Color.web("#cbd5e1"));
        sessionLine.setFont(Font.font("System", 12));
        identity.getChildren().addAll(title, userLine, sessionLine);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scopeBadge = new Label(scopeBadgeText());
        scopeBadge.setTextFill(Color.WHITE);
        scopeBadge.setFont(Font.font("System", FontWeight.BOLD, 12));
        scopeBadge.setPadding(new Insets(7, 12, 7, 12));
        scopeBadge.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 4;");

        Button refreshButton = new Button("Actualizar");
        refreshButton.setStyle("-fx-background-color: #bfdbfe; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
        refreshButton.setOnAction(event -> refresh());

        Button analyticsButton = new Button("Ver analitica operacional");
        analyticsButton.setDisable(analyticsController == null || safeScope() == null);
        analyticsButton.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
        analyticsButton.setOnAction(event -> openControllerAnalytics());

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4;");
        logoutButton.setOnAction(event -> runLogout());

        header.getChildren().addAll(identity, spacer, scopeBadge, analyticsButton, refreshButton, logoutButton);
        return header;
    }

    private ScrollPane createContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(18));
        content.getChildren().addAll(
                createRestrictionBanner(),
                createSummaryGrid(),
                createAssignedRoutesSection(),
                createOperationalGrid(),
                createAnalyticsAccessSection());

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private VBox createRestrictionBanner() {
        VBox banner = new VBox(5);
        banner.setPadding(new Insets(14, 16, 14, 16));
        banner.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label title = sectionTitle("Alcance operativo del controlador");
        Label detail = bodyLabel(scopeWarningText());
        banner.getChildren().addAll(title, detail);
        return banner;
    }

    private GridPane createSummaryGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(createSummaryCard("Rutas asignadas", routeCountValue, "Asociadas a tu perfil"), 0, 0);
        grid.add(createSummaryCard("Buses supervisados", busCountValue, "Activos en rutas asignadas"), 1, 0);
        grid.add(createSummaryCard("Eventos recientes", eventCountValue, "Asociados al alcance operativo"), 2, 0);
        grid.add(createSummaryCard("Alertas visibles", alertCountValue, "Disponibilidad por alcance"), 3, 0);
        grid.add(createSummaryCard("Acceso", accessValue, "Informacion restringida por rutas"), 0, 1);

        return grid;
    }

    private VBox createSummaryCard(String title, Label valueLabel, String description) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setMinWidth(170);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        card.getChildren().addAll(smallTitle(title), valueLabel, mutedLabel(description));
        return card;
    }

    private VBox createAssignedRoutesSection() {
        VBox section = cardContainer();
        HBox header = sectionHeader("Rutas asignadas", "Rutas disponibles para este controlador");

        FlowPane routes = new FlowPane(10, 10);
        routes.setPadding(new Insets(4, 0, 0, 0));
        List<Integer> routeIds = assignedRouteIds();
        if (routeIds.isEmpty()) {
            routes.getChildren().add(emptyLabel("No hay rutas asignadas para este controlador."));
        } else {
            for (Integer routeId : routeIds) {
                routes.getChildren().add(createRouteChip(routeId));
            }
        }

        section.getChildren().addAll(header, new Separator(), routes);
        return section;
    }

    private GridPane createOperationalGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        VBox mapSection = createMapSection();
        VBox busesSection = createListSection("Buses supervisados", busList,
                "No hay buses activos en las rutas asignadas.");
        VBox eventsSection = createListSection("Eventos recientes", eventList,
                "No hay eventos recientes en las rutas asignadas.");
        VBox alertsSection = createListSection("Alertas visibles", alertList,
                "Alertas operativas no disponibles para este alcance.");

        grid.add(mapSection, 0, 0, 2, 1);
        grid.add(busesSection, 0, 1);
        grid.add(eventsSection, 1, 1);
        grid.add(alertsSection, 0, 2, 2, 1);
        return grid;
    }

    private VBox createMapSection() {
        VBox section = cardContainer();
        section.setMinHeight(320);
        section.getChildren().addAll(
                sectionHeader("Mapa operativo", "Muestra buses asociados a tus rutas asignadas"),
                new Separator(),
                mapView);
        mapView.setMinHeight(260);
        VBox.setVgrow(mapView, Priority.ALWAYS);
        return section;
    }

    private VBox createListSection(String title, ListView<String> listView, String emptyMessage) {
        VBox section = cardContainer();
        section.setMinHeight(230);
        section.setPrefWidth(440);
        section.getChildren().addAll(sectionTitle(title), new Separator(), listView);
        configureList(listView, emptyMessage);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return section;
    }

    private void configureList(ListView<String> listView, String emptyMessage) {
        listView.setFocusTraversable(false);
        listView.setFixedCellSize(54);
        listView.setPlaceholder(new Label(emptyMessage));
        listView.setStyle("-fx-control-inner-background: white; -fx-font-size: 12px;");
        listView.setCellFactory(view -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setPadding(new Insets(7, 8, 7, 8));
                setTextFill(Color.web("#1f2937"));
                setWrapText(true);
            }
        });
    }

    private VBox createAnalyticsAccessSection() {
        VBox note = new VBox(6);
        note.setPadding(new Insets(14, 16, 14, 16));
        note.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        note.getChildren().addAll(
                sectionTitle("Analitica operacional"),
                bodyLabel("Consulta indicadores historicos y operativos limitados a tus rutas asignadas."));
        return note;
    }

    private void refresh() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
            return;
        }

        AccessScope scope = safeScope();
        List<BusMarker> filteredBuses = filteredBuses(scope);
        List<OperationalEvent> filteredEvents = filteredEvents(scope);
        List<AlertPanelModel> filteredAlerts = filteredAlerts(scope);

        routeCountValue.setText(String.valueOf(assignedRouteIds().size()));
        busCountValue.setText(String.valueOf(filteredBuses.size()));
        eventCountValue.setText(String.valueOf(filteredEvents.size()));
        alertCountValue.setText(canShowRouteFilteredAlerts(scope) ? String.valueOf(filteredAlerts.size()) : "No disponible");
        accessValue.setText(accessText(scope));

        busList.getItems().setAll(busRows(filteredBuses));
        eventList.getItems().setAll(eventRows(filteredEvents));
        alertList.getItems().setAll(alertRows(filteredAlerts, scope));
        mapView.updateBuses(filteredBuses);
    }

    private List<BusMarker> filteredBuses(AccessScope scope) {
        if (monitoringController == null || scope == null) {
            return Collections.emptyList();
        }
        return monitoringController.getCurrentBuses(scope);
    }

    private List<OperationalEvent> filteredEvents(AccessScope scope) {
        if (monitoringController == null || scope == null) {
            return Collections.emptyList();
        }
        return monitoringController.getRecentEvents(scope);
    }

    private List<AlertPanelModel> filteredAlerts(AccessScope scope) {
        if (monitoringController == null || scope == null) {
            return Collections.emptyList();
        }
        return monitoringController.getAlerts(scope);
    }

    private List<String> busRows(List<BusMarker> buses) {
        List<String> rows = new ArrayList<String>();
        for (BusMarker bus : buses) {
            String speed = bus.getSpeed() == null ? "N/A" : String.format(Locale.US, "%.1f", bus.getSpeed());
            String time = bus.getLastUpdate() == null ? "" : " | " + ROW_TIME_FORMATTER.format(bus.getLastUpdate());
            rows.add(bus.getBusCode() + " | " + routeLabel(Integer.valueOf(bus.getRouteId())) +
                    " | speed " + speed +
                    " | " + String.format(Locale.US, "%.5f, %.5f", bus.getLatitude(), bus.getLongitude()) +
                    " | " + safeText(bus.getStatus()) + time);
        }
        return rows;
    }

    private List<String> eventRows(List<OperationalEvent> events) {
        List<String> rows = new ArrayList<String>();
        for (OperationalEvent event : events) {
            String time = event.getTimestamp() == null ? "" : " | " + ROW_TIME_FORMATTER.format(event.getTimestamp());
            rows.add(enumName(event.getEventType()) + " | " + enumName(event.getPriority()) +
                    " | " + routeLabel(Integer.valueOf(event.getRouteId())) +
                    " | bus " + safeText(event.getBusCode()) +
                    " | " + safeText(event.getDescription()) + time);
        }
        return rows;
    }

    private List<String> alertRows(List<AlertPanelModel> alerts, AccessScope scope) {
        List<String> rows = new ArrayList<String>();
        if (!canShowRouteFilteredAlerts(scope)) {
            rows.add("Alertas operativas no disponibles para este alcance.");
            return rows;
        }
        for (AlertPanelModel alert : alerts) {
            String time = alert.getTimestamp() == null ? "" : " | " + ROW_TIME_FORMATTER.format(alert.getTimestamp());
            rows.add(enumName(alert.getLevel()) + " | " + safeText(alert.getTitle()) + " | " +
                    safeText(alert.getMessage()) + time);
        }
        return rows;
    }

    private boolean canShowRouteFilteredAlerts(AccessScope scope) {
        return scope != null && scope.canViewAllRoutes();
    }

    private void registerRefreshListener() {
        if (monitoringController == null) {
            return;
        }
        monitoringController.addStateListener(new MonitoringStateListener() {
            @Override
            public void onMonitoringStateChanged() {
                refresh();
            }
        });
    }

    private HBox sectionHeader(String title, String subtitle) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(3);
        text.getChildren().addAll(sectionTitle(title), mutedLabel(subtitle));
        row.getChildren().add(text);
        return row;
    }

    private VBox cardContainer() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        return card;
    }

    private Label createRouteChip(Integer routeId) {
        Label chip = new Label(routeLabel(routeId));
        chip.setTextFill(Color.web("#0f172a"));
        chip.setFont(Font.font("System", FontWeight.BOLD, 13));
        chip.setPadding(new Insets(8, 12, 8, 12));
        chip.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        return chip;
    }

    private static Label summaryValue(String value) {
        Label label = new Label(value);
        label.setTextFill(Color.web("#1e40af"));
        label.setFont(Font.font("System", FontWeight.BOLD, 22));
        label.setWrapText(true);
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#0f172a"));
        label.setFont(Font.font("System", FontWeight.BOLD, 16));
        label.setWrapText(true);
        return label;
    }

    private Label smallTitle(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#334155"));
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setWrapText(true);
        return label;
    }

    private Label bodyLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#334155"));
        label.setFont(Font.font("System", 13));
        label.setWrapText(true);
        return label;
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#64748b"));
        label.setFont(Font.font("System", 12));
        label.setWrapText(true);
        return label;
    }

    private Label emptyLabel(String text) {
        Label label = bodyLabel(text);
        label.setPadding(new Insets(12));
        label.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        return label;
    }

    private List<Integer> assignedRouteIds() {
        AccessScope scope = safeScope();
        List<Integer> routeIds = new ArrayList<Integer>();
        if (scope != null) {
            routeIds.addAll(scope.getAllowedRouteIds());
        }
        Collections.sort(routeIds);
        return routeIds;
    }

    private AccessScope safeScope() {
        return session == null ? null : session.getAccessScope();
    }

    private String routeLabel(Integer routeId) {
        if (routeId == null) {
            return "Route -";
        }
        if (monitoringController != null) {
            String label = monitoringController.getRouteFilterLabel(routeId.intValue());
            if (label != null && label.trim().length() > 0) {
                return label.trim();
            }
        }
        return "Route " + routeId;
    }

    private String scopeBadgeText() {
        if (session == null || session.getUser() == null) {
            return "Sin sesion";
        }
        return assignedRouteIds().size() + " rutas asignadas";
    }

    private String accessText(AccessScope scope) {
        if (scope == null) {
            return "Sin alcance";
        }
        if (scope.canViewAllRoutes()) {
            return "Global";
        }
        if (scope.isEmpty()) {
            return "Sin rutas";
        }
        return "Restringido";
    }

    private String scopeWarningText() {
        AccessScope scope = safeScope();
        if (scope == null) {
            return "No hay sesion activa o alcance disponible. La informacion operativa no se muestra.";
        }
        if (scope.isEmpty()) {
            return "No hay rutas asignadas para este controlador.";
        }
        return "Este panel muestra solo datos asociados a las rutas asignadas al controlador.";
    }

    private AuthenticatedUser user() {
        return session == null ? null : session.getUser();
    }

    private String userName() {
        AuthenticatedUser user = user();
        return user == null ? "No hay sesion activa" : user.getFullName();
    }

    private String nationalId() {
        AuthenticatedUser user = user();
        return user == null ? "-" : user.getNationalId();
    }

    private String roleName() {
        AuthenticatedUser user = user();
        return user == null || user.getRole() == null ? "-" : user.getRole().name();
    }

    private String loginTime() {
        return session == null || session.getLoginTime() == null ? "-" : LOGIN_TIME_FORMATTER.format(session.getLoginTime());
    }

    private String enumName(Enum<?> value) {
        return value == null ? "N/A" : value.name();
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "N/A" : value.trim();
    }

    private void runLogout() {
        if (logoutAction != null) {
            logoutAction.run();
        }
    }

    private void openControllerAnalytics() {
        AccessScope scope = safeScope();
        if (analyticsController == null || scope == null) {
            return;
        }
        AnalyticsFxPanel analyticsPanel = new AnalyticsFxPanel(analyticsController, scope);
        analyticsPanel.showView();
    }
}
