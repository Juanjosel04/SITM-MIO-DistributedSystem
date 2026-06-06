package worker.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import worker.ice.WorkerIceConfig;
import worker.ice.WorkerIceServer;
import worker.processing.BucketProcessingService;
import worker.processing.PartialSpeedResult;
import worker.processing.RemoteProcessingListener;
import worker.runtime.WorkerRuntimePaths;
import worker.transfer.BucketTransferListener;
import worker.transfer.ReceivedBucketInfo;
import worker.ui.WorkerNodeView;

import java.util.concurrent.CompletableFuture;

public class WorkerNodeApplication extends Application {
    private static final int INITIAL_WIDTH = 1120;
    private static final int INITIAL_HEIGHT = 760;
    private WorkerIceServer iceServer;
    private WorkerRuntimePaths runtimePaths;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        runtimePaths = WorkerRuntimePaths.fromSystemProperties();
        String workerId = runtimePaths.workerId();
        int port = readPort();
        WorkerNodeView view = new WorkerNodeView(workerId, port, runtimePaths);
        view.setProcessBucketsHandler(() -> processLocalBuckets(view));
        Scene scene = new Scene(view.createContent(), INITIAL_WIDTH, INITIAL_HEIGHT);

        primaryStage.setTitle("Worker Node - SITM-MIO");
        primaryStage.setMinWidth(940);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> stopIceServer());
        primaryStage.show();

        startIceServer(view, workerId, port);
    }

    @Override
    public void stop() {
        stopIceServer();
    }

    private int readPort() {
        String value = System.getProperty("sitm.worker.port", "10001");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 10001;
        }
    }

    private void startIceServer(WorkerNodeView view, String workerId, int port) {
        try {
            runtimePaths.ensureDirectories();
            WorkerIceConfig config = WorkerIceConfig.defaults(workerId, port);
            iceServer = new WorkerIceServer(config, new UiBucketTransferListener(view),
                    new UiRemoteProcessingListener(view), runtimePaths);
            iceServer.start();
            view.showIceActive(config.getHost(), config.getPort());
        } catch (RuntimeException exception) {
            view.showIceError(exception.getMessage());
        }
    }

    private void stopIceServer() {
        if (iceServer != null) {
            iceServer.close();
            iceServer = null;
        }
    }

    private void processLocalBuckets(WorkerNodeView view) {
        view.showProcessingStarted();
        CompletableFuture
                .supplyAsync(() -> new BucketProcessingService(runtimePaths).processLocalBuckets())
                .thenAccept(result -> Platform.runLater(() -> view.showProcessingFinished(result)))
                .exceptionally(exception -> {
                    PartialSpeedResult failure = PartialSpeedResult.failure("Bucket processing failed: " + exception.getMessage());
                    Platform.runLater(() -> view.showProcessingFinished(failure));
                    return null;
                });
    }

    private static final class UiBucketTransferListener implements BucketTransferListener {
        private final WorkerNodeView view;

        private UiBucketTransferListener(WorkerNodeView view) {
            this.view = view;
        }

        @Override
        public void transferStarted(ReceivedBucketInfo info) {
            Platform.runLater(() -> view.showBucketTransferStarted(info));
        }

        @Override
        public void chunkReceived(ReceivedBucketInfo info) {
            Platform.runLater(() -> view.showBucketChunkReceived(info));
        }

        @Override
        public void transferFinished(ReceivedBucketInfo info) {
            Platform.runLater(() -> view.showBucketTransferFinished(info));
        }

        @Override
        public void transferFailed(ReceivedBucketInfo info) {
            Platform.runLater(() -> view.showBucketTransferFailed(info));
        }
    }

    private static final class UiRemoteProcessingListener implements RemoteProcessingListener {
        private final WorkerNodeView view;

        private UiRemoteProcessingListener(WorkerNodeView view) {
            this.view = view;
        }

        @Override
        public void remoteProcessingStarted(String jobId) {
            Platform.runLater(() -> view.showRemoteProcessingStarted(jobId));
        }

        @Override
        public void remoteProcessingFinished(String jobId, PartialSpeedResult result) {
            Platform.runLater(() -> view.showRemoteProcessingFinished(jobId, result));
        }
    }
}
