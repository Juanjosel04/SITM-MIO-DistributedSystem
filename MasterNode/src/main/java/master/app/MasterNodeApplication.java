package master.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import master.ui.MasterNodeView;

public class MasterNodeApplication extends Application {
    private static final int INITIAL_WIDTH = 1180;
    private static final int INITIAL_HEIGHT = 760;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        MasterNodeView view = new MasterNodeView();
        Scene scene = new Scene(view.createContent(), INITIAL_WIDTH, INITIAL_HEIGHT);

        primaryStage.setTitle("Master Node - Reconstrucción Distribuida SITM-MIO");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
