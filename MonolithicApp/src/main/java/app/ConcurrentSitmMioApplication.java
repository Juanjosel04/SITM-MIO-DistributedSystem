package app;

import domain.Datagram;
import domain.Route;
import ingestion.ConcurrentDatasetPaths;
import ingestion.DatagramLoader;
import ingestion.RouteLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import processing.AverageSpeedProcessingResult;
import processing.benchmark.ProcessingMetrics;
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
        Scene scene = new Scene(loadInitialContent(), INITIAL_WIDTH, INITIAL_HEIGHT);

        primaryStage.setTitle("SITM-MIO - Version Concurrente");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Parent loadInitialContent() {
        ConcurrentDashboardView dashboardView = new ConcurrentDashboardView();
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
            return dashboardView.createContent(
                    result.getMetrics(),
                    result.getAverages().size(),
                    "Nucleo Fork/Join ejecutado"
            );
        } catch (Exception exception) {
            return dashboardView.createContent(
                    ProcessingMetrics.empty(),
                    0,
                    "No se pudo cargar el procesamiento V2"
            );
        }
    }
}
