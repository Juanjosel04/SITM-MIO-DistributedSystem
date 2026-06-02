package monitoring.map.fx;

import core.utils.AppLogger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import monitoring.controller.MonitoringController;
import monitoring.model.BusMarker;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapFxView extends BorderPane {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final double COORDINATE_EPSILON = 0.000001;
    private static final double MIN_CALI_LATITUDE = 2.8;
    private static final double MAX_CALI_LATITUDE = 4.2;
    private static final double MIN_CALI_LONGITUDE = -77.2;
    private static final double MAX_CALI_LONGITUDE = -75.8;

    private final MonitoringController monitoringController;
    private final Map<String, BusMarker> pendingMarkers = new LinkedHashMap<String, BusMarker>();
    private final Map<String, BusMarker> latestMarkers = new LinkedHashMap<String, BusMarker>();
    private final PauseTransition resizeRefresh = new PauseTransition(Duration.millis(150));
    private WebEngine webEngine;
    private Label fallbackCountLabel;
    private ListView<String> fallbackBusList;
    private boolean mapReady;
    private boolean fallbackActive;

    public MapFxView() {
        this(null);
    }

    public MapFxView(MonitoringController monitoringController) {
        this.monitoringController = monitoringController;
        setMinWidth(440);
        setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        configureResizeRefresh();
        initializeWebView();
    }

    public void updateBuses(List<BusMarker> buses) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    updateBuses(buses);
                }
            });
            return;
        }

        latestMarkers.clear();
        if (buses == null) {
            updateFallbackContent();
            return;
        }
        for (BusMarker bus : buses) {
            if (isValidMarker(bus)) {
                latestMarkers.put(bus.getBusCode(), bus);
            }
        }

        if (mapReady && !fallbackActive) {
            for (BusMarker bus : latestMarkers.values()) {
                if (!sendMarker(bus)) {
                    break;
                }
            }
            scheduleMapSizeRefresh();
        } else {
            pendingMarkers.clear();
            pendingMarkers.putAll(latestMarkers);
            updateFallbackContent();
        }
    }

    private void initializeWebView() {
        AppLogger.info("JavaFX WebView map initialization started.");
        WebView webView = new WebView();
        webEngine = webView.getEngine();

        URL mapResource = getClass().getResource("/map/map.html");
        if (mapResource == null) {
            AppLogger.warn("JavaFX WebView map resource was not found.");
            activateFallback("Map resource could not be loaded.");
            return;
        }

        AppLogger.info("JavaFX WebView map resource found.");
        setCenter(webView);
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                handleMapLoaded();
            } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                AppLogger.warn("JavaFX WebView map load did not complete: " + newState.name());
                activateFallback("Map could not be loaded in WebView.");
            }
        });
        webEngine.load(mapResource.toExternalForm());
    }

    private void configureResizeRefresh() {
        resizeRefresh.setOnFinished(event -> executeMapScript("refreshMapSize()", false));
        widthProperty().addListener((observable, oldValue, newValue) -> scheduleMapSizeRefresh());
        heightProperty().addListener((observable, oldValue, newValue) -> scheduleMapSizeRefresh());
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        scheduleMapSizeRefresh();
                    }
                });
            }
        });
    }

    private void handleMapLoaded() {
        try {
            Object ready = webEngine.executeScript(
                    "typeof initializeMap === 'function' && " +
                            "(initializeMap(), typeof isSitmMapReady === 'function' && isSitmMapReady())");
            if (!Boolean.TRUE.equals(ready)) {
                activateFallback("Map scripts loaded, but Leaflet was not initialized.");
                return;
            }

            mapReady = true;
            fallbackActive = false;
            AppLogger.info("Map initialized centered on Cali.");
            executeMapScript("centerOnCali()", true);
            scheduleMapSizeRefresh();
            flushPendingMarkers();
            scheduleMapSizeRefresh();
        } catch (RuntimeException exception) {
            activateFallback("Map scripts failed during initialization.");
        }
    }

    private void flushPendingMarkers() {
        List<BusMarker> markers = new ArrayList<BusMarker>(pendingMarkers.values());
        pendingMarkers.clear();
        for (BusMarker marker : markers) {
            if (!sendMarker(marker)) {
                break;
            }
        }
        scheduleMapSizeRefresh();
    }

    private boolean sendMarker(BusMarker marker) {
        try {
            String time = marker.getLastUpdate() == null ? "" : TIME_FORMATTER.format(marker.getLastUpdate());
            String script = String.format(Locale.US,
                    "updateBus('%s', %.8f, %.8f, '%s', '%s')",
                    escapeJavaScript(marker.getBusCode()),
                    marker.getLatitude(),
                    marker.getLongitude(),
                    escapeJavaScript(routeFilterLabel(marker.getRouteId())),
                    escapeJavaScript(time));
            Object updated = webEngine.executeScript(script);
            if (!Boolean.TRUE.equals(updated)) {
                throw new IllegalStateException("Map marker update returned false.");
            }
            return true;
        } catch (RuntimeException exception) {
            if (!fallbackActive) {
                AppLogger.warn("JavaFX WebView map marker update failed.");
            }
            activateFallback("Map loaded, but bus markers could not be updated.");
            return false;
        }
    }

    private boolean isValidMarker(BusMarker marker) {
        if (marker == null || marker.getBusCode() == null || marker.getBusCode().trim().isEmpty()) {
            return false;
        }
        double latitude = marker.getLatitude();
        double longitude = marker.getLongitude();
        if (Double.isNaN(latitude) || Double.isNaN(longitude) ||
                Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
            return false;
        }
        if (Math.abs(latitude) < COORDINATE_EPSILON && Math.abs(longitude) < COORDINATE_EPSILON) {
            return false;
        }
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            return false;
        }
        return latitude >= MIN_CALI_LATITUDE && latitude <= MAX_CALI_LATITUDE &&
                longitude >= MIN_CALI_LONGITUDE && longitude <= MAX_CALI_LONGITUDE;
    }

    private void activateFallback(String message) {
        if (fallbackActive && fallbackCountLabel != null && fallbackBusList != null) {
            updateFallbackContent();
            return;
        }
        mapReady = false;
        fallbackActive = true;
        AppLogger.warn("Map fallback activated.");
        setCenter(createFallbackView(message));
        updateFallbackContent();
    }

    private VBox createFallbackView(String message) {
        VBox fallback = new VBox(10);
        fallback.setAlignment(Pos.CENTER);
        fallback.setPadding(new Insets(24));
        fallback.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label title = new Label("Map unavailable");
        title.setTextFill(Color.web("#0f172a"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label detail = new Label(message);
        detail.setTextFill(Color.web("#475569"));
        detail.setFont(Font.font("System", 13));
        detail.setWrapText(true);

        fallbackCountLabel = new Label("Active buses: 0");
        fallbackCountLabel.setTextFill(Color.web("#1e40af"));
        fallbackCountLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        fallbackBusList = new ListView<String>();
        fallbackBusList.setFocusTraversable(false);
        fallbackBusList.setFixedCellSize(36);
        fallbackBusList.setMaxWidth(420);
        fallbackBusList.setPlaceholder(new Label("Waiting for valid coordinates"));

        fallback.getChildren().addAll(title, detail, fallbackCountLabel, fallbackBusList);
        VBox.setVgrow(fallbackBusList, Priority.ALWAYS);
        return fallback;
    }

    private void updateFallbackContent() {
        if (!fallbackActive || fallbackCountLabel == null || fallbackBusList == null) {
            return;
        }
        fallbackCountLabel.setText("Active buses: " + latestMarkers.size());
        List<String> rows = new ArrayList<String>();
        int count = 0;
        for (BusMarker marker : latestMarkers.values()) {
            if (count >= 10) {
                break;
            }
            rows.add(marker.getBusCode() + " | route " + routeFilterLabel(marker.getRouteId()) + " | " +
                    String.format(Locale.US, "%.5f, %.5f", marker.getLatitude(), marker.getLongitude()));
            count++;
        }
        fallbackBusList.getItems().setAll(rows);
    }

    private String escapeJavaScript(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String routeFilterLabel(int routeId) {
        return monitoringController == null
                ? "Route " + routeId
                : monitoringController.getRouteFilterLabel(routeId);
    }

    private void scheduleMapSizeRefresh() {
        if (!mapReady || fallbackActive) {
            return;
        }
        resizeRefresh.playFromStart();
    }

    private void executeMapScript(String script, boolean fallbackOnFailure) {
        if (!mapReady || fallbackActive || webEngine == null) {
            return;
        }
        try {
            webEngine.executeScript(script);
        } catch (RuntimeException exception) {
            if (fallbackOnFailure) {
                activateFallback("Map JavaScript call failed.");
            }
        }
    }
}
