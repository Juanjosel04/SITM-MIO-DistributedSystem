package app;

import domain.Datagram;
import domain.Route;
import ingestion.ConcurrentDatasetPaths;
import ingestion.RouteLoader;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import processing.AverageSpeedProcessingResult;
import processing.streaming.StreamingBucketedAverageSpeedProcessor;
import visualization.ConcurrentDashboardView;

import java.util.List;

/**
 * Minimal JavaFX shell for the V2 concurrent application.
 */
public class ConcurrentSitmMioApplication extends Application {
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 800;

    @Override
    public void start(Stage primaryStage) {
        ConcurrentDashboardView dashboardView = new ConcurrentDashboardView();
        Scene scene = new Scene(dashboardView.createContent(), INITIAL_WIDTH, INITIAL_HEIGHT);

        primaryStage.setTitle("SITM-MIO V2 Concurrente - Fork/Join");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();

        runProcessing(dashboardView);
    }

    private void runProcessing(ConcurrentDashboardView dashboardView) {
        dashboardView.showProcessingState("Procesando con Fork/Join...");
        Task<LoadedConcurrentResult> task = new Task<LoadedConcurrentResult>() {
            @Override
            protected LoadedConcurrentResult call() throws Exception {
                RouteLoader routeLoader = new RouteLoader();
                String dataset = ConcurrentDatasetPaths.defaultDatagramsFile();

                List<Route> routes = routeLoader.loadDefault();

                StreamingBucketedAverageSpeedProcessor processor = new StreamingBucketedAverageSpeedProcessor(dataset);
                AverageSpeedProcessingResult result = processor.process(routes, null);
                return new LoadedConcurrentResult(result, result.getVisualDatagrams());
            }
        };

        task.setOnSucceeded(event -> {
            LoadedConcurrentResult loadedResult = task.getValue();
            dashboardView.showResult(loadedResult.result, loadedResult.datagrams);
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String message = error == null ? "Error desconocido" : error.getMessage();
            System.err.println("V2 processing failed: " + message);
            dashboardView.showError("No se pudo cargar el procesamiento V2: " + message);
        });

        Thread loaderThread = new Thread(task, "sitm-v2-dashboard-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private static final class LoadedConcurrentResult {
        private final AverageSpeedProcessingResult result;
        private final List<Datagram> datagrams;

        private LoadedConcurrentResult(AverageSpeedProcessingResult result, List<Datagram> datagrams) {
            this.result = result;
            this.datagrams = datagrams;
        }
    }
}
