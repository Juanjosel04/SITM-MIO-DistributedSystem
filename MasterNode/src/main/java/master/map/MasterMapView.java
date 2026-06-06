package master.map;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class MasterMapView extends BorderPane {
    private final PauseTransition resizeRefresh = new PauseTransition(Duration.millis(200));
    private final PauseTransition loadSettleRefresh = new PauseTransition(Duration.millis(300));
    private final Timeline playbackTimeline;
    private final MapDataJsonSerializer serializer = new MapDataJsonSerializer();
    private final List<MapPlaybackPoint> playbackPoints = new ArrayList<MapPlaybackPoint>();
    private WebEngine webEngine;
    private boolean ready;
    private int playbackIndex;
    private String routeFilter;

    public MasterMapView() {
        setMinSize(560, 360);
        setStyle(panelStyle());
        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(MapPlaybackSampler.PLAYBACK_INTERVAL_MS),
                event -> playNextBatch()));
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
        configureRefresh();
        initializeWebView();
    }

    public void loadPoints(List<MapPlaybackPoint> points) {
        pause();
        clearMarkers();
        playbackPoints.clear();
        if (points != null) {
            playbackPoints.addAll(points);
        }
        playbackIndex = 0;
        auditPoints("loadPoints", playbackPoints);
        execute("if (typeof loadPoints === 'function') { loadPoints('" + escapeForJavaScript(serializer.toJson(playbackPoints)) + "'); }");
        applyRouteFilterToMap();
    }

    public void play() {
        if (ready && !visiblePlaybackPoints().isEmpty()) {
            playbackTimeline.play();
            execute("if (typeof setPlaybackState === 'function') { setPlaybackState('Playing distributed sample'); }");
        }
    }

    public void pause() {
        playbackTimeline.stop();
        execute("if (typeof setPlaybackState === 'function') { setPlaybackState('Paused'); }");
    }

    public void resetPlayback() {
        pause();
        playbackIndex = 0;
        clearMarkers();
        execute("if (typeof resetPlayback === 'function') { resetPlayback(); }");
        applyRouteFilterToMap();
    }

    public void clearMarkers() {
        execute("if (typeof clearMovingMarkers === 'function') { clearMovingMarkers(); }");
    }

    public int getLoadedPointCount() {
        return playbackPoints.size();
    }

    public int getVisiblePointCount() {
        return visiblePlaybackPoints().size();
    }

    public void filterRoute(String routeId) {
        pause();
        playbackIndex = 0;
        routeFilter = routeId == null || routeId.trim().isEmpty() ? null : routeId.trim();
        System.out.println("Map route filter=" + (routeFilter == null ? "ALL" : routeFilter)
                + "; visiblePoints=" + visiblePlaybackPoints().size());
        clearMarkers();
        applyRouteFilterToMap();
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
                System.out.println("MasterMapView loaded /map/map.html; expected MAP_HTML_VERSION=V3_EXACT_V2_VISUAL_COPY_20260606; resource="
                        + mapResource);
                execute("if (typeof initializeMap === 'function') { initializeMap(); }");
                execute("if (typeof centerOnCali === 'function') { centerOnCali(); }");
                loadSettleRefresh.playFromStart();
                if (!playbackPoints.isEmpty()) {
                    loadPoints(new ArrayList<MapPlaybackPoint>(playbackPoints));
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

    private void playNextBatch() {
        if (!ready || playbackPoints.isEmpty()) {
            playbackTimeline.stop();
            return;
        }
        List<MapPlaybackPoint> visiblePoints = visiblePlaybackPoints();
        if (visiblePoints.isEmpty()) {
            playbackTimeline.stop();
            return;
        }
        if (playbackIndex >= visiblePoints.size()) {
            playbackIndex = 0;
            clearMarkers();
        }
        int end = Math.min(playbackIndex + MapPlaybackSampler.MAX_POINTS_PER_TICK, visiblePoints.size());
        String json = serializer.toJson(visiblePoints.subList(playbackIndex, end));
        playbackIndex = end;
        execute("if (typeof updateMovingMarkers === 'function') { updateMovingMarkers('" + escapeForJavaScript(json) + "'); }");
        execute("if (typeof setPlaybackProgress === 'function') { setPlaybackProgress(" + playbackIndex + ", "
                + visiblePoints.size() + "); }");
    }

    private List<MapPlaybackPoint> visiblePlaybackPoints() {
        if (routeFilter == null || routeFilter.isEmpty()) {
            return new ArrayList<MapPlaybackPoint>(playbackPoints);
        }
        List<MapPlaybackPoint> filtered = new ArrayList<MapPlaybackPoint>();
        for (MapPlaybackPoint point : playbackPoints) {
            if (routeFilter.equals(point.getRouteId())) {
                filtered.add(point);
            }
        }
        return filtered;
    }

    private void applyRouteFilterToMap() {
        String value = routeFilter == null ? "__ALL__" : routeFilter;
        execute("if (typeof filterRoute === 'function') { filterRoute('" + escapeForJavaScript(value) + "'); }");
    }

    private void auditPoints(String phase, List<MapPlaybackPoint> points) {
        int total = points == null ? 0 : points.size();
        Set<String> routes = new TreeSet<String>();
        MapPlaybackPoint first = null;
        if (points != null) {
            for (MapPlaybackPoint point : points) {
                if (first == null) {
                    first = point;
                }
                if (point.getRouteId() != null && !point.getRouteId().trim().isEmpty()) {
                    routes.add(point.getRouteId().trim());
                }
            }
        }
        System.out.println("Map audit phase=" + phase + "; Points sent to WebView=" + total
                + "; uniqueRoutes=" + routes.size() + "; firstRoutes=" + firstRoutes(routes));
        if (first != null) {
            System.out.println("Map audit firstPoint=visualBusKey=" + first.getVisualBusKey()
                    + ", processingBusId=" + first.getBusId()
                    + ", routeId=" + first.getRouteId()
                    + ", lat=" + first.getLatitude()
                    + ", lon=" + first.getLongitude()
                    + ", timestamp=" + first.getTimestamp());
        }
    }

    private String firstRoutes(Set<String> routes) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String route : routes) {
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(route);
            count++;
            if (count >= 10) {
                break;
            }
        }
        return builder.toString();
    }

    private void execute(String script) {
        if (!ready || webEngine == null) {
            return;
        }
        try {
            webEngine.executeScript(script);
        } catch (RuntimeException ignored) {
            // Keep the JavaFX dashboard alive even if WebView reports a transient JavaScript issue.
        }
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
