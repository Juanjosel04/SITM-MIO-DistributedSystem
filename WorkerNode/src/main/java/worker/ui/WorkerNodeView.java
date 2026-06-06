package worker.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import worker.processing.PartialSpeedResult;
import worker.processing.ProcessingCounters;
import worker.processing.RouteMonthKey;
import worker.processing.SpeedAccumulator;
import worker.runtime.WorkerRuntimePaths;
import worker.transfer.ReceivedBucketInfo;

import java.nio.file.Path;
import java.util.Map;

public class WorkerNodeView {
    private static final String WAITING = "Waiting for processing order";
    private static final String RECEIVING = "Receiving bucket";
    private static final String PROCESSING = "Processing bucket-0";
    private static final String COMPLETED = "Processing completed";
    private static final String ERROR = "Error";

    private final String workerId;
    private final int port;
    private final WorkerRuntimePaths runtimePaths;
    private final Label currentState = new Label(WAITING);
    private final Label statusBadge = new Label("WAITING");
    private final ProgressBar workerProgress = new ProgressBar(0);
    private final TextArea activityLog = new TextArea();
    private final ObservableList<String> chunkItems = FXCollections.observableArrayList();
    private final Label currentJob = new Label("none");
    private final Label currentBucket = new Label("none");
    private final Label endpoint = new Label("0.0.0.0:10001");
    private final Label runtimeDirectory = new Label("none");
    private final Label receivedBucketsDirectory = new Label("none");
    private final Label demoBucketsDirectory = new Label("none");
    private final Label receivedChunks = new Label("0");
    private final Label receivedBytes = new Label("0");
    private final Label receivedBuckets = new Label("0");
    private final Label localBucketPath = new Label("none");
    private final Label processedBuckets = new Label("0");
    private final Label activeTasks = new Label("0");
    private final Label validIntervals = new Label("0");
    private final Label discards = new Label("0");
    private final Label routeChanged = new Label("0");
    private final Label invalidTime = new Label("0");
    private final Label invalidDistance = new Label("0");
    private final Label speedTooHigh = new Label("0");
    private final Label routeMonthKeys = new Label("0");
    private final Label partialAverages = new Label("0");
    private final Label localTime = new Label("0 ms");
    private final Label errors = new Label("0");
    private Timeline simulationTimeline;
    private Runnable processBucketsHandler;

    public WorkerNodeView(String workerId, int port, WorkerRuntimePaths runtimePaths) {
        this.workerId = workerId == null || workerId.trim().isEmpty() ? "worker-local" : workerId.trim();
        this.port = port;
        this.runtimePaths = runtimePaths;
        this.endpoint.setText("0.0.0.0:" + this.port);
        this.runtimeDirectory.setText(display(runtimePaths.baseDirectory()));
        this.receivedBucketsDirectory.setText(display(runtimePaths.receivedBucketsDirectory()));
        this.demoBucketsDirectory.setText(display(runtimePaths.demoBucketsDirectory()));
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #eef3f8;");

        root.setTop(createHeader());
        root.setCenter(createDashboardBody());

        resetChunks();
        appendLog("Worker initialized.");
        appendLog("Worker runtime directory: " + runtimeDirectory.getText());
        appendLog("Received buckets directory: " + receivedBucketsDirectory.getText());
        appendLog("Demo buckets directory: " + demoBucketsDirectory.getText());
        appendLog("Waiting for processing order.");
        appendLog("UI ready for bucket chunk reception.");

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #eef3f8; -fx-background: #eef3f8;");
        return scrollPane;
    }

    private Parent createHeader() {
        Label title = new Label("SITM-MIO Worker Node");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #18212f;");

        Label subtitle = new Label("Worker ID: " + workerId + "   |   Puerto ICE: " + port);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        statusBadge.setStyle(badgeStyle("#e2e8f0", "#334155"));
        VBox copy = new VBox(4, title, subtitle);
        HBox header = new HBox(14, copy, spacer(), statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 12, 22));
        return header;
    }

    private Parent createDashboardBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(0, 22, 22, 22));
        body.getChildren().addAll(createStatePanel(), createMetricsPanel(), createChunkPanel(), createLogPanel());
        return body;
    }

    private Parent createStatePanel() {
        currentState.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        workerProgress.setMaxWidth(Double.MAX_VALUE);

        StackPane stateBox = new StackPane(currentState);
        stateBox.setAlignment(Pos.CENTER_LEFT);
        stateBox.setPadding(new Insets(16));
        stateBox.setMinHeight(78);
        stateBox.setStyle(panelStyle());

        return new VBox(10, stateBox, workerProgress);
    }

    private Parent createMetricsPanel() {
        GridPane grid = metricGrid();
        addMetric(grid, 0, "Worker ID", new Label(workerId));
        addMetric(grid, 1, "Port", new Label(String.valueOf(port)));
        addMetric(grid, 2, "Endpoint", endpoint);
        addMetric(grid, 3, "Runtime directory", runtimeDirectory);
        addMetric(grid, 4, "Current job", currentJob);
        addMetric(grid, 5, "Current bucket", currentBucket);
        addMetric(grid, 6, "Chunks received", receivedChunks);
        addMetric(grid, 7, "Buckets received", receivedBuckets);
        addMetric(grid, 8, "Buckets processed", processedBuckets);
        addMetric(grid, 9, "Valid intervals", validIntervals);
        addMetric(grid, 10, "Partial averages", partialAverages);
        addMetric(grid, 11, "Local time", localTime);
        addMetric(grid, 12, "Errors", errors);

        VBox panel = new VBox(12, sectionTitle("Worker summary"), grid);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createChunkPanel() {
        ListView<String> chunks = new ListView<String>(chunkItems);
        chunks.setPrefHeight(150);
        VBox panel = new VBox(10, sectionTitle("Bucket activity"), chunks);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createControlPanel() {
        Button processLocal = actionButton("Process Local Buckets");
        processLocal.setOnAction(event -> {
            if (processBucketsHandler != null) {
                processBucketsHandler.run();
            }
        });

        Button waiting = actionButton("Simulate Waiting");
        waiting.setOnAction(event -> simulateWaiting());

        Button receiving = actionButton("Simulate Receiving");
        receiving.setOnAction(event -> simulateReceiving());

        Button processing = actionButton("Simulate Processing");
        processing.setOnAction(event -> simulateProcessing());

        Button completed = actionButton("Simulate Completed");
        completed.setOnAction(event -> simulateCompleted());

        Button error = actionButton("Simulate Error");
        error.setOnAction(event -> simulateError());

        Button clear = actionButton("Clear Log");
        clear.setOnAction(event -> activityLog.clear());

        VBox panel = new VBox(10, sectionTitle("Local Controls"), processLocal, waiting, receiving, processing, completed, error, clear);
        panel.setPrefWidth(280);
        panel.setPadding(new Insets(0, 0, 0, 16));
        return panel;
    }

    private Parent createLogPanel() {
        activityLog.setEditable(false);
        activityLog.setWrapText(true);
        activityLog.setPrefRowCount(7);
        activityLog.setStyle("-fx-font-size: 12px;");

        VBox panel = new VBox(10, sectionTitle("Worker Activity Log"), activityLog);
        panel.setPadding(new Insets(14, 0, 0, 0));
        return panel;
    }

    private void simulateWaiting() {
        stopTimeline();
        setState(WAITING, "WAITING", "#e2e8f0", "#334155");
        workerProgress.setProgress(0);
        currentJob.setText("none");
        currentBucket.setText("none");
        activeTasks.setText("0");
        resetChunks();
        appendLog("Worker returned to waiting state.");
    }

    public void showIceActive(String host, int port) {
        endpoint.setText(host + ":" + port);
        setState(WAITING, "ICE ACTIVE", "#dcfce7", "#166534");
        appendLog("ICE WorkerService active on " + host + ":" + port + ".");
        appendLog("Runtime storage ready under " + runtimeDirectory.getText() + ".");
    }

    public void showIceError(String message) {
        errors.setText(String.valueOf(Integer.parseInt(errors.getText()) + 1));
        setState(ERROR, "ERROR", "#fee2e2", "#991b1b");
        appendLog("ICE startup failed: " + message);
    }

    public void setProcessBucketsHandler(Runnable processBucketsHandler) {
        this.processBucketsHandler = processBucketsHandler;
    }

    public void showBucketTransferStarted(ReceivedBucketInfo info) {
        stopTimeline();
        currentJob.setText(info.getJobId());
        currentBucket.setText(info.getBucketId());
        receivedChunks.setText("0 / " + info.getTotalChunks());
        receivedBytes.setText("0 / " + info.getTotalBytes());
        localBucketPath.setText(info.getLocalPath() == null ? "pending" : info.getLocalPath().toString());
        workerProgress.setProgress(0);
        resetChunks(info.getTotalChunks());
        setState(RECEIVING, "RECEIVING", "#dbeafe", "#1d4ed8");
        appendLog("Receiving bucket " + info.getBucketId() + " for job " + info.getJobId() + ".");
    }

    public void showBucketChunkReceived(ReceivedBucketInfo info) {
        currentJob.setText(info.getJobId());
        currentBucket.setText(info.getBucketId());
        receivedChunks.setText(info.getReceivedChunks() + " / " + info.getTotalChunks());
        receivedBytes.setText(info.getReceivedBytes() + " / " + info.getTotalBytes());
        double progress = info.getTotalChunks() == 0 ? 0 : (double) info.getReceivedChunks() / (double) info.getTotalChunks();
        workerProgress.setProgress(progress);
        setState("Loading chunk " + info.getReceivedChunks() + "/" + info.getTotalChunks(), "RECEIVING", "#dbeafe", "#1d4ed8");
        updateChunk(info.getReceivedChunks() - 1, "Chunk " + info.getReceivedChunks() + "/" + info.getTotalChunks() + " received");
        appendLog("Loading chunk " + info.getReceivedChunks() + "/" + info.getTotalChunks() + ".");
    }

    public void showBucketTransferFinished(ReceivedBucketInfo info) {
        currentJob.setText(info.getJobId());
        currentBucket.setText(info.getBucketId());
        receivedChunks.setText(info.getReceivedChunks() + " / " + info.getTotalChunks());
        receivedBytes.setText(info.getReceivedBytes() + " / " + info.getTotalBytes());
        localBucketPath.setText(info.getLocalPath() == null ? "none" : info.getLocalPath().toString());
        receivedBuckets.setText(String.valueOf(Integer.parseInt(receivedBuckets.getText()) + 1));
        workerProgress.setProgress(1.0);
        setState("Bucket received", "RECEIVED", "#dcfce7", "#166534");
        updateChunk(info.getReceivedChunks(), "Bucket " + info.getBucketId() + " stored");
        appendLog("Bucket " + info.getBucketId() + " stored at " + localBucketPath.getText() + ".");
        appendLog("Bucket stored under worker runtime directory.");
    }

    public void showBucketTransferFailed(ReceivedBucketInfo info) {
        errors.setText(String.valueOf(Integer.parseInt(errors.getText()) + 1));
        if (info != null) {
            currentJob.setText(info.getJobId());
            currentBucket.setText(info.getBucketId());
            receivedChunks.setText(info.getReceivedChunks() + " / " + info.getTotalChunks());
            receivedBytes.setText(info.getReceivedBytes() + " / " + info.getTotalBytes());
        }
        setState(ERROR, "ERROR", "#fee2e2", "#991b1b");
        appendLog(info == null ? "Bucket transfer failed." : info.getMessage());
    }

    public void showProcessingStarted() {
        stopTimeline();
        workerProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        activeTasks.setText("ForkJoin");
        validIntervals.setText("0");
        discards.setText("0");
        routeChanged.setText("0");
        invalidTime.setText("0");
        invalidDistance.setText("0");
        speedTooHigh.setText("0");
        routeMonthKeys.setText("0");
        partialAverages.setText("0");
        localTime.setText("running");
        chunkItems.setAll("Starting local Fork/Join bucket processing.");
        setState("Processing datagrams", "PROCESSING", "#dcfce7", "#166534");
        appendLog("Starting local Fork/Join bucket processing.");
        appendLog("Processing buckets from: " + receivedBucketsDirectory.getText());
        appendLog("Demo buckets directory: " + demoBucketsDirectory.getText());
    }

    public void showRemoteProcessingStarted(String jobId) {
        showProcessingStarted();
        currentJob.setText(jobId);
        appendLog("Remote processing requested for job " + jobId + ".");
        appendLog("Worker processing jobId=" + jobId + ".");
        appendLog("Worker received bucket dir=" + receivedBucketsDirectory.getText()
                + java.io.File.separator + jobId + ".");
        setState("Remote processing requested", "REMOTE", "#dbeafe", "#1d4ed8");
    }

    public void showRemoteProcessingFinished(String jobId, PartialSpeedResult result) {
        currentJob.setText(jobId);
        appendLog("Remote processing completed for job " + jobId + ".");
        showProcessingFinished(result);
    }

    public void showProcessingFinished(PartialSpeedResult result) {
        ProcessingCounters counters = result.getCounters();
        processedBuckets.setText(String.valueOf(result.getProcessedBuckets().size()));
        activeTasks.setText("0");
        validIntervals.setText(String.valueOf(counters.getValidIntervals()));
        discards.setText(String.valueOf(counters.discardedIntervals()));
        routeChanged.setText(String.valueOf(counters.getRouteChanged()));
        invalidTime.setText(String.valueOf(counters.getNonPositiveDeltaTime() + counters.getExcessiveDeltaTime()));
        invalidDistance.setText(String.valueOf(counters.getNonPositiveDistance() + counters.getNegativeOdometer()));
        speedTooHigh.setText(String.valueOf(counters.getSpeedTooHigh()));
        routeMonthKeys.setText(String.valueOf(result.getAccumulators().size()));
        partialAverages.setText(String.valueOf(result.getAccumulators().size()));
        localTime.setText(result.getElapsedMillis() + " ms");
        workerProgress.setProgress(result.isSuccess() ? 1.0 : 0);
        chunkItems.clear();
        chunkItems.add("Processed bucket files: " + result.getProcessedBuckets().size());
        chunkItems.add("Lines read: " + counters.getTotalLinesRead());
        chunkItems.add("Valid intervals: " + counters.getValidIntervals());
        chunkItems.add("Discarded intervals: " + counters.discardedIntervals());
        appendLog(result.getMessage());
        int shown = 0;
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : result.getAccumulators().entrySet()) {
            SpeedAccumulator accumulator = entry.getValue();
            String row = entry.getKey().display()
                    + " | distance=" + format(accumulator.getTotalDistanceMeters())
                    + " m | time=" + format(accumulator.getTotalTimeSeconds())
                    + " s | intervals=" + accumulator.getValidIntervals()
                    + " | avg=" + format(accumulator.averageKmh()) + " km/h";
            chunkItems.add(row);
            appendLog("Partial result: " + row);
            shown++;
            if (shown >= 8) {
                break;
            }
        }
        if (result.isSuccess()) {
            appendLog("Partial result generated with " + result.getAccumulators().size() + " route-month keys.");
            appendLog("Processing completed in " + result.getElapsedMillis() + " ms.");
            setState(COMPLETED, "DONE", "#dcfce7", "#166534");
        } else {
            errors.setText(String.valueOf(Integer.parseInt(errors.getText()) + 1));
            appendLog(result.getMessage());
            setState(ERROR, "ERROR", "#fee2e2", "#991b1b");
        }
    }

    private void simulateReceiving() {
        stopTimeline();
        currentJob.setText("visual-job-01");
        currentBucket.setText("route-group-01");
        receivedChunks.setText("0");
        receivedBuckets.setText("0");
        resetChunks();
        setState(RECEIVING, "RECEIVING", "#dbeafe", "#1d4ed8");
        appendLog("Receiving bucket metadata.");

        simulationTimeline = new Timeline(
                chunkStep(0, 0.20, 0, "Loading chunk 1", "Loading chunk 1."),
                chunkStep(1, 0.45, 1, "Loading chunk 2", "Loading chunk 2."),
                chunkStep(2, 0.70, 2, "Loading chunk 3", "Loading chunk 3."),
                chunkStep(3, 1.00, 3, "Bucket received", "Bucket received.")
        );
        simulationTimeline.setOnFinished(event -> {
            receivedBuckets.setText("1");
            setState("Bucket received", "RECEIVED", "#dcfce7", "#166534");
            updateChunk(3, "Bucket route-group-01 ready");
        });
        simulationTimeline.playFromStart();
    }

    private KeyFrame chunkStep(int seconds, double progress, int receivedCount, String state, String log) {
        return new KeyFrame(Duration.seconds(seconds), event -> {
            workerProgress.setProgress(progress);
            receivedChunks.setText(String.valueOf(receivedCount));
            setState(state, "RECEIVING", "#dbeafe", "#1d4ed8");
            if (receivedCount > 0) {
                updateChunk(receivedCount - 1, "Chunk 0" + receivedCount + " received");
            }
            appendLog(log);
        });
    }

    private void simulateProcessing() {
        stopTimeline();
        currentJob.setText("visual-job-01");
        currentBucket.setText("route-group-01");
        activeTasks.setText("4");
        setState("Starting datagram processing", "PROCESSING", "#dcfce7", "#166534");
        appendLog("Starting local datagram processing.");

        simulationTimeline = new Timeline(
                processStep(0, 0.20, PROCESSING, "1", "0", "0", "Processing bucket-0."),
                processStep(1, 0.50, "Processing bucket-1", "2", "0", "0", "Processing bucket-1."),
                processStep(2, 0.80, "Building partial result", "2", "48", "3", "Building partial result."),
                processStep(3, 1.00, COMPLETED, "0", "96", "5", "Worker returned to waiting state.")
        );
        simulationTimeline.setOnFinished(event -> {
            processedBuckets.setText("2");
            partialAverages.setText("4");
            localTime.setText("1240 ms");
            setState(COMPLETED, "DONE", "#dcfce7", "#166534");
        });
        simulationTimeline.playFromStart();
    }

    private KeyFrame processStep(int seconds, double progress, String state, String tasks, String intervals,
                                 String discardCount, String log) {
        return new KeyFrame(Duration.seconds(seconds), event -> {
            workerProgress.setProgress(progress);
            activeTasks.setText(tasks);
            validIntervals.setText(intervals);
            discards.setText(discardCount);
            setState(state, "PROCESSING", "#dcfce7", "#166534");
            appendLog(log);
        });
    }

    private void simulateCompleted() {
        stopTimeline();
        workerProgress.setProgress(1.0);
        processedBuckets.setText("2");
        partialAverages.setText("4");
        localTime.setText("1240 ms");
        setState(COMPLETED, "DONE", "#dcfce7", "#166534");
        appendLog("Processing completed.");
        appendLog("Partial result available locally.");
    }

    private void simulateError() {
        stopTimeline();
        errors.setText(String.valueOf(Integer.parseInt(errors.getText()) + 1));
        setState(ERROR, "ERROR", "#fee2e2", "#991b1b");
        appendLog("Error state simulated locally.");
    }

    private void resetChunks() {
        chunkItems.setAll(
                "Chunk 01 pending",
                "Chunk 02 pending",
                "Chunk 03 pending",
                "Bucket route-group-01 pending"
        );
    }

    private void resetChunks(int totalChunks) {
        chunkItems.clear();
        int visibleChunks = Math.max(1, Math.min(totalChunks, 20));
        for (int index = 1; index <= visibleChunks; index++) {
            chunkItems.add("Chunk " + index + "/" + totalChunks + " pending");
        }
        if (totalChunks > visibleChunks) {
            chunkItems.add("Additional chunks pending: " + (totalChunks - visibleChunks));
        }
        chunkItems.add("Bucket pending");
    }

    private void updateChunk(int index, String text) {
        if (index >= 0 && index < chunkItems.size()) {
            chunkItems.set(index, text);
        }
    }

    private void stopTimeline() {
        if (simulationTimeline != null) {
            simulationTimeline.stop();
        }
    }

    private void setState(String state, String badge, String background, String textColor) {
        currentState.setText(state);
        statusBadge.setText(badge);
        statusBadge.setStyle(badgeStyle(background, textColor));
    }

    private void appendLog(String message) {
        activityLog.appendText(message + System.lineSeparator());
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String display(Path path) {
        return path == null ? "none" : path.toString();
    }

    private GridPane metricGrid() {
        GridPane grid = new GridPane();
        grid.setVgap(9);
        grid.setHgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setMinWidth(120);
        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    private void addMetric(GridPane grid, int row, String name, Label value) {
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: #475569;");
        value.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private Button actionButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: #1f4f82; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 5; -fx-padding: 9 12;");
        return button;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        return label;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String panelStyle() {
        return "-fx-background-color: #ffffff;"
                + "-fx-border-color: #d8e1ec;"
                + "-fx-border-radius: 7;"
                + "-fx-background-radius: 7;";
    }

    private String badgeStyle(String background, String textColor) {
        return "-fx-background-color: " + background + ";"
                + "-fx-text-fill: " + textColor + ";"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 999;"
                + "-fx-padding: 8 14;";
    }
}
