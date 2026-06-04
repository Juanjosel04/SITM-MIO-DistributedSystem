package app;

import domain.Datagram;
import domain.Route;
import ingestion.ConcurrentDatasetPaths;
import ingestion.DatagramLoader;
import ingestion.RouteLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import processing.AverageSpeedProcessingResult;
import processing.forkjoin.ForkJoinAverageSpeedProcessor;
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
        try {
            RouteLoader routeLoader = new RouteLoader();
            DatagramLoader datagramLoader = new DatagramLoader();
            String dataset = ConcurrentDatasetPaths.defaultDatagramsFile();

            List<Route> routes = routeLoader.loadDefault();
            List<Datagram> datagrams = datagramLoader.load(dataset);

            ForkJoinAverageSpeedProcessor processor = new ForkJoinAverageSpeedProcessor(
                    ForkJoinAverageSpeedProcessor.DEFAULT_PARALLELISM,
                    ForkJoinAverageSpeedProcessor.DEFAULT_THRESHOLD,
                    dataset
            );
            AverageSpeedProcessingResult result = processor.process(routes, datagrams);
            dashboardView.showResult(result);
        } catch (Exception exception) {
            Throwable error = exception;
            String message = error == null ? "Error desconocido" : error.getMessage();
            System.err.println("V2 processing failed: " + message);
            dashboardView.showError("No se pudo cargar el procesamiento V2: " + message);
        }
    }
}
