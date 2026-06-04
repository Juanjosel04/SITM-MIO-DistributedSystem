package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import visualization.ConcurrentDashboardView;

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

        primaryStage.setTitle("SITM-MIO - Version Concurrente");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
