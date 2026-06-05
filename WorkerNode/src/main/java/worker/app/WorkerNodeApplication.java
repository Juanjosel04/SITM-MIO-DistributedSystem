package worker.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import worker.ui.WorkerNodeView;

public class WorkerNodeApplication extends Application {
    private static final int INITIAL_WIDTH = 900;
    private static final int INITIAL_HEIGHT = 660;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String workerId = System.getProperty("sitm.worker.id", "worker-local");
        int port = readPort();
        WorkerNodeView view = new WorkerNodeView(workerId, port);
        Scene scene = new Scene(view.createContent(), INITIAL_WIDTH, INITIAL_HEIGHT);

        primaryStage.setTitle("Worker Node - SITM-MIO");
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(560);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private int readPort() {
        String value = System.getProperty("sitm.worker.port", "10001");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 10001;
        }
    }
}
