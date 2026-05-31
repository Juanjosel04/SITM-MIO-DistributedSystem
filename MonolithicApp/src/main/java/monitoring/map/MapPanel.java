package monitoring.map;

import monitoring.model.BusMarker;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MapPanel extends JPanel {
    private static final DateTimeFormatter MAP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final List<BusMarker> markers = new ArrayList<BusMarker>();
    private Object webEngine;
    private boolean htmlMapAvailable;
    private boolean htmlMapReady;

    public MapPanel() {
        setPreferredSize(new Dimension(560, 360));
        setBackground(new Color(239, 246, 255));
        setLayout(new BorderLayout());
        loadFxMap();
    }

    public synchronized void updateMarkers(List<BusMarker> updatedMarkers) {
        markers.clear();
        markers.addAll(updatedMarkers);
        if (htmlMapAvailable) {
            updateHtmlMarkers();
        } else {
            repaint();
        }
    }

    @Override
    protected synchronized void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (htmlMapAvailable) {
            return;
        }
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintGrid(g2);
        paintMarkers(g2);
        g2.dispose();
    }

    private void paintGrid(Graphics2D g2) {
        int width = getWidth();
        int height = getHeight();
        g2.setColor(new Color(219, 234, 254));
        for (int x = 40; x < width; x += 72) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 36; y < height; y += 58) {
            g2.drawLine(0, y, width, y);
        }
        g2.setColor(new Color(30, 64, 175));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(14, 14, width - 28, height - 28, 18, 18);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Realtime route field", 28, 38);
    }

    private void paintMarkers(Graphics2D g2) {
        if (markers.isEmpty()) {
            g2.setColor(new Color(71, 85, 105));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.drawString("Waiting for bus positions...", 28, 70);
            return;
        }

        Bounds bounds = Bounds.from(markers);
        for (BusMarker marker : markers) {
            int x = bounds.x(marker.getLongitude(), getWidth());
            int y = bounds.y(marker.getLatitude(), getHeight());
            g2.setColor(new Color(14, 165, 233));
            g2.fillOval(x - 7, y - 7, 14, 14);
            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString(marker.getBusCode(), x + 10, y - 8);
            g2.setColor(new Color(30, 64, 175));
            g2.drawOval(x - 7, y - 7, 14, 14);
        }
    }

    private void loadFxMap() {
        URL mapResource = getClass().getResource("/map/map.html");
        if (mapResource == null) {
            return;
        }

        try {
            Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
            final JComponent fxPanel = (JComponent) jfxPanelClass.newInstance();
            add(fxPanel, BorderLayout.CENTER);
            htmlMapAvailable = true;
            initializeWebView(fxPanel, mapResource);
        } catch (Exception exception) {
            activateFallback();
        }
    }

    private void initializeWebView(final JComponent fxPanel, final URL mapResource) throws Exception {
        Class<?> platformClass = Class.forName("javafx.application.Platform");
        try {
            platformClass.getMethod("setImplicitExit", boolean.class).invoke(null, Boolean.FALSE);
        } catch (Exception ignored) {
        }
        platformClass.getMethod("runLater", Runnable.class).invoke(null, new Runnable() {
            @Override
            public void run() {
                createWebView(fxPanel, mapResource);
            }
        });
    }

    private void createWebView(JComponent fxPanel, URL mapResource) {
        try {
            Class<?> webViewClass = Class.forName("javafx.scene.web.WebView");
            Object webView = webViewClass.newInstance();
            webEngine = webViewClass.getMethod("getEngine").invoke(webView);
            registerLoadListener(webEngine);

            Class<?> parentClass = Class.forName("javafx.scene.Parent");
            Class<?> sceneClass = Class.forName("javafx.scene.Scene");
            Constructor<?> sceneConstructor = sceneClass.getConstructor(parentClass);
            Object scene = sceneConstructor.newInstance(webView);
            fxPanel.getClass().getMethod("setScene", sceneClass).invoke(fxPanel, scene);
            webEngine.getClass().getMethod("load", String.class).invoke(webEngine, mapResource.toExternalForm());
        } catch (Exception exception) {
            activateFallbackOnSwingThread();
        }
    }

    private void registerLoadListener(final Object engine) {
        try {
            Object worker = engine.getClass().getMethod("getLoadWorker").invoke(engine);
            Object stateProperty = worker.getClass().getMethod("stateProperty").invoke(worker);
            Class<?> listenerClass = Class.forName("javafx.beans.value.ChangeListener");
            Object listener = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("changed".equals(method.getName()) && args != null && args.length == 3) {
                                handleLoadState(String.valueOf(args[2]));
                            }
                            return null;
                        }
                    });
            stateProperty.getClass().getMethod("addListener", listenerClass).invoke(stateProperty, listener);
        } catch (Exception exception) {
            htmlMapReady = true;
            updateHtmlMarkers();
        }
    }

    private void handleLoadState(String state) {
        if ("SUCCEEDED".equals(state)) {
            htmlMapReady = true;
            updateHtmlMarkers();
        } else if ("FAILED".equals(state) || "CANCELLED".equals(state)) {
            activateFallbackOnSwingThread();
        }
    }

    private void updateHtmlMarkers() {
        if (!htmlMapAvailable || !htmlMapReady || webEngine == null) {
            return;
        }
        final List<BusMarker> markerSnapshot;
        synchronized (this) {
            markerSnapshot = new ArrayList<BusMarker>(markers);
        }
        runOnFxThread(new Runnable() {
            @Override
            public void run() {
                for (BusMarker marker : markerSnapshot) {
                    executeUpdateBus(marker);
                }
            }
        });
    }

    private void executeUpdateBus(BusMarker marker) {
        try {
            String time = marker.getLastUpdate() == null ? "" : MAP_TIME_FORMATTER.format(marker.getLastUpdate());
            String script = "updateBus('" + escapeJavaScript(marker.getBusCode()) + "', " +
                    marker.getLatitude() + ", " +
                    marker.getLongitude() + ", '" +
                    escapeJavaScript(String.valueOf(marker.getRouteId())) + "', '" +
                    escapeJavaScript(time) + "')";
            webEngine.getClass().getMethod("executeScript", String.class).invoke(webEngine, script);
        } catch (Exception exception) {
            activateFallbackOnSwingThread();
        }
    }

    private void runOnFxThread(Runnable runnable) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            platformClass.getMethod("runLater", Runnable.class).invoke(null, runnable);
        } catch (Exception exception) {
            activateFallbackOnSwingThread();
        }
    }

    private String escapeJavaScript(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void activateFallbackOnSwingThread() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                activateFallback();
            }
        });
    }

    private synchronized void activateFallback() {
        removeAll();
        htmlMapAvailable = false;
        htmlMapReady = false;
        webEngine = null;
        revalidate();
        repaint();
    }

    private static class Bounds {
        private final double minLatitude;
        private final double maxLatitude;
        private final double minLongitude;
        private final double maxLongitude;

        private Bounds(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
        }

        static Bounds from(List<BusMarker> markers) {
            double minLatitude = Double.MAX_VALUE;
            double maxLatitude = -Double.MAX_VALUE;
            double minLongitude = Double.MAX_VALUE;
            double maxLongitude = -Double.MAX_VALUE;
            for (BusMarker marker : markers) {
                minLatitude = Math.min(minLatitude, marker.getLatitude());
                maxLatitude = Math.max(maxLatitude, marker.getLatitude());
                minLongitude = Math.min(minLongitude, marker.getLongitude());
                maxLongitude = Math.max(maxLongitude, marker.getLongitude());
            }
            if (minLatitude == maxLatitude) {
                minLatitude -= 0.01;
                maxLatitude += 0.01;
            }
            if (minLongitude == maxLongitude) {
                minLongitude -= 0.01;
                maxLongitude += 0.01;
            }
            return new Bounds(minLatitude, maxLatitude, minLongitude, maxLongitude);
        }

        int x(double longitude, int width) {
            double ratio = (longitude - minLongitude) / (maxLongitude - minLongitude);
            return 34 + (int) Math.round(ratio * Math.max(1, width - 68));
        }

        int y(double latitude, int height) {
            double ratio = (latitude - minLatitude) / (maxLatitude - minLatitude);
            return height - 34 - (int) Math.round(ratio * Math.max(1, height - 68));
        }
    }
}
