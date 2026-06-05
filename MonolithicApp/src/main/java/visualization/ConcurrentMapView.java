package visualization;

import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ConcurrentMapView extends BorderPane {
    private final PauseTransition resizeRefresh = new PauseTransition(Duration.millis(200));
    private final PauseTransition loadSettleRefresh = new PauseTransition(Duration.millis(300));
    private final Timeline playbackTimeline;
    private final List<MapPlaybackPoint> playbackPoints = new ArrayList<MapPlaybackPoint>();
    private WebEngine webEngine;
    private boolean ready;
    private int playbackIndex;

    public ConcurrentMapView() {
        setMinSize(520, 360);
        setStyle(panelStyle());
        this.playbackTimeline = new Timeline(new KeyFrame(Duration.millis(MapPlaybackSampler.PLAYBACK_INTERVAL_MS), event -> playNextBatch()));
        this.playbackTimeline.setCycleCount(Timeline.INDEFINITE);
        configureRefresh();
        initializeWebView();
    }

    public void startPlayback(List<MapPlaybackPoint> points) {
        stopPlayback();
        clearMarkers();
        playbackPoints.clear();
        playbackPoints.addAll(sample(points));
        playbackIndex = 0;
        if (ready && !playbackPoints.isEmpty()) {
            playbackTimeline.playFromStart();
        }
    }

    public void stopPlayback() {
        playbackTimeline.stop();
    }

    public void clearMarkers() {
        if (ready) {
            execute("if (typeof clearMovingMarkers === 'function') { clearMovingMarkers(); }");
        }
    }

    private void initializeWebView() {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webEngine = webView.getEngine();

        URL mapResource = getClass().getResource("/map/map.html");
        if (mapResource == null) {
            showFallback("No se encontro el recurso del mapa.");
            return;
        }

        setCenter(webView);
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                ready = true;
                execute("if (typeof initializeMap === 'function') { initializeMap(); }");
                execute("if (typeof centerOnCali === 'function') { centerOnCali(); }");
                loadSettleRefresh.playFromStart();
                if (!playbackPoints.isEmpty()) {
                    playbackTimeline.playFromStart();
                }
            } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                showFallback("No se pudo cargar el mapa en WebView.");
            }
        });
        webEngine.load(mapResource.toExternalForm());
    }

    private void configureRefresh() {
        resizeRefresh.setOnFinished(event -> execute("if (typeof fixMapSize === 'function') { fixMapSize(); }"));
        loadSettleRefresh.setOnFinished(event -> execute("if (typeof fixMapSize === 'function') { fixMapSize(); }"));
        widthProperty().addListener((observable, oldValue, newValue) -> scheduleRefresh());
        heightProperty().addListener((observable, oldValue, newValue) -> scheduleRefresh());
    }

    private void scheduleRefresh() {
        if (ready) {
            resizeRefresh.playFromStart();
        }
    }

    private void execute(String script) {
        if (!ready || webEngine == null) {
            return;
        }
        try {
            webEngine.executeScript(script);
        } catch (RuntimeException ignored) {
            // The map has a visible fallback path; transient JavaScript errors should not crash V2.
        }
    }

    private void playNextBatch() {
        if (!ready || playbackPoints.isEmpty()) {
            playbackTimeline.stop();
            return;
        }
        if (playbackIndex >= playbackPoints.size()) {
            playbackIndex = 0;
        }

        int end = Math.min(playbackIndex + MapPlaybackSampler.MAX_POINTS_PER_TICK, playbackPoints.size());
        String json = toJson(playbackPoints.subList(playbackIndex, end));
        playbackIndex = end;
        execute("if (typeof updateMovingMarkers === 'function') { updateMovingMarkers('" + escapeForJavaScript(json) + "'); }");
    }

    private List<MapPlaybackPoint> sample(List<MapPlaybackPoint> points) {
        if (points == null || points.isEmpty()) {
            return Collections.<MapPlaybackPoint>emptyList();
        }
        return new MapPlaybackSampler().sample(points);
    }

    private String toJson(List<MapPlaybackPoint> points) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < points.size(); i++) {
            MapPlaybackPoint point = points.get(i);
            if (i > 0) {
                builder.append(",");
            }
            String popup = "Bus: " + point.getBusId()
                    + "<br>Ruta: " + point.getRouteLabel()
                    + "<br>Promedio ruta/mes: " + point.getAverageSpeedText()
                    + "<br>Fecha: " + point.getTimestampText();
            builder.append("{")
                    .append("\"busId\":\"").append(escapeJson(point.getBusId())).append("\",")
                    .append("\"routeId\":").append(point.getRouteId()).append(",")
                    .append("\"routeLabel\":\"").append(escapeJson(point.getRouteLabel())).append("\",")
                    .append("\"latitude\":").append(String.format(Locale.US, "%.8f", Double.valueOf(point.getLatitude()))).append(",")
                    .append("\"longitude\":").append(String.format(Locale.US, "%.8f", Double.valueOf(point.getLongitude()))).append(",")
                    .append("\"popupText\":\"").append(escapeJson(popup)).append("\"")
                    .append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String escapeForJavaScript(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void showFallback(String message) {
        ready = false;
        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
        BorderPane fallback = new BorderPane(label);
        fallback.setPadding(new Insets(24));
        BorderPane.setAlignment(label, Pos.CENTER);
        fallback.setStyle(panelStyle());
        setCenter(fallback);
    }

    private String panelStyle() {
        return "-fx-background-color: #ffffff;"
                + "-fx-border-color: #d9e2ef;"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;";
    }
}
