package master.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import master.bucket.BucketInfo;
import master.bucket.BucketizationConfig;
import master.bucket.BucketizationResult;
import master.distribution.BucketDistributionItem;
import master.distribution.BucketDistributionStatus;
import master.distribution.DistributionResult;
import master.ice.WorkerEndpointConfig;
import master.ice.WorkerConnectionResult;
import master.map.MasterMapView;
import master.merge.GlobalMergedResultRow;
import master.merge.GlobalMergeResult;
import master.results.RemoteProcessingSummary;
import master.results.RemoteRouteMonthPartial;
import master.results.RemoteWorkerPartialResult;
import master.transfer.BucketTransferResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MasterNodeView {
    private final Label stateLabel = new Label("Waiting for workers");
    private final Label stateBadge = new Label("WAITING");
    private final Label workersDetected = new Label("0");
    private final Label activeWorkers = new Label("0");
    private final Label downWorkers = new Label("0");
    private final Label lastScanStatus = new Label("not scanned");
    private final Label bucketsPrepared = new Label("0");
    private final Label bucketsAssigned = new Label("0");
    private final Label bucketsInProgress = new Label("0");
    private final Label bucketsCompleted = new Label("0");
    private final Label failedBuckets = new Label("0");
    private final Label pendingBuckets = new Label("0");
    private final Label transferTarget = new Label("none");
    private final Label transferBucket = new Label("none");
    private final Label transferChunks = new Label("0 / 0");
    private final Label transferBytes = new Label("0");
    private final Label transferResult = new Label("not started");
    private final Label bucketizationJob = new Label("none");
    private final Label recordsRead = new Label("0");
    private final Label validRecords = new Label("0");
    private final Label invalidRecords = new Label("0");
    private final Label bucketOutputDirectory = new Label("none");
    private final Label bucketElapsed = new Label("0 ms");
    private final Label distributionJob = new Label("none");
    private final Label distributionWorkers = new Label("0");
    private final Label currentDistributionWorker = new Label("none");
    private final Label currentDistributionBucket = new Label("none");
    private final Label distributionResult = new Label("not started");
    private final Label remoteProcessingJob = new Label("none");
    private final Label remoteWorkersRequested = new Label("0");
    private final Label remoteWorkersSucceeded = new Label("0");
    private final Label remoteWorkersFailed = new Label("0");
    private final Label remotePartialRows = new Label("0");
    private final Label remoteProcessingElapsed = new Label("0 ms");
    private final Label globalMergeJob = new Label("none");
    private final Label globalWorkersMerged = new Label("0");
    private final Label globalWorkersFailed = new Label("0");
    private final Label globalRows = new Label("0");
    private final Label globalValidIntervals = new Label("0");
    private final Label globalDistance = new Label("0 m");
    private final Label globalTime = new Label("0 s");
    private final Label globalMergeElapsed = new Label("0 ms");
    private final Label mapStatus = new Label("waiting for bucketization");
    private final Label mapSampleKept = new Label("0");
    private final Label mapSampleSeen = new Label("0");
    private final Label mapSampleStep = new Label("1");
    private final Label mapSampleLimit = new Label("0");
    private final Label visualRouteLabel = new Label("Ruta visual: Sin rutas visuales disponibles");
    private final ComboBox<String> routeFilterComboBox = new ComboBox<String>();
    private final MasterMapView mapView = new MasterMapView();
    private final Button runPipelineButton = new Button("Iniciar procesamiento de rutas");
    private final Label pipelineStatus = new Label("Listo para iniciar");
    private final Label pipelineStep = new Label("Paso actual: pendiente");
    private final ProgressBar pipelineProgress = new ProgressBar(0);
    private final ProgressBar globalProgress = new ProgressBar(0);
    private final TextArea activityLog = new TextArea();
    private final TextArea pipelineLog = new TextArea();
    private final ObservableList<WorkerRow> workers = FXCollections.observableArrayList();
    private final ObservableList<ResultRow> results = FXCollections.observableArrayList();
    private Timeline processingTimeline;
    private Runnable runFullPipelineHandler;
    private Runnable detectWorkersHandler;
    private Runnable transferBucketHandler;
    private Runnable generateBucketsHandler;
    private Runnable distributeBucketsHandler;
    private Runnable processRemoteBucketsHandler;
    private Runnable mergeGlobalResultsHandler;

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #eef3f8;");

        root.setTop(createHeader());
        root.setCenter(createCenterArea());
        root.setRight(createRightPanel());
        root.setBottom(createResultsPanel());

        appendLog("Master node initialized.");
        appendLog("Waiting for worker registration.");
        appendLog("Distributed dashboard ready.");
        appendLog("No distributed job running.");
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #eef3f8;");
        return scrollPane;
    }

    private Parent createHeader() {
        Label title = new Label("SITM-MIO Distributed Processing");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #182235;");

        Label subtitle = new Label("Procesamiento distribuido de rutas con workers ICE y mapa visual");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        stateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
        stateBadge.setStyle(badgeStyle("#dbeafe", "#1d4ed8"));

        VBox copy = new VBox(5, title, subtitle, stateLabel);
        HBox header = new HBox(14, copy, spacer(), stateBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 16, 0));
        return header;
    }

    private Parent createCenterArea() {
        VBox center = new VBox(14);
        center.getChildren().addAll(createMapPanel(), createWorkerPanel(), createActivityPanel());
        VBox.setVgrow(center.getChildren().get(0), Priority.ALWAYS);
        return center;
    }

    private Parent createMapPanel() {
        Label title = new Label("Distributed Map Playback");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Button play = actionButton("Play Map");
        play.setOnAction(event -> playMap());

        Button pause = actionButton("Pause Map");
        pause.setOnAction(event -> pauseMap());

        Button reset = actionButton("Reset Map");
        reset.setOnAction(event -> resetMap());

        routeFilterComboBox.setDisable(true);
        routeFilterComboBox.setPrefWidth(190);
        routeFilterComboBox.setOnAction(event -> applyRouteFilter());
        visualRouteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        VBox routeFilter = new VBox(4, new Label("Filtro de ruta"), routeFilterComboBox, visualRouteLabel);
        routeFilter.setAlignment(Pos.CENTER_LEFT);

        HBox controls = new HBox(8, play, pause, reset, spacer(), routeFilter);
        controls.setAlignment(Pos.CENTER_LEFT);

        GridPane metrics = metricGrid();
        addMetric(metrics, 0, "Map status", mapStatus);
        addMetric(metrics, 1, "Sample kept", mapSampleKept);
        addMetric(metrics, 2, "Sample seen", mapSampleSeen);
        addMetric(metrics, 3, "Sample step", mapSampleStep);
        addMetric(metrics, 4, "Sample limit", mapSampleLimit);

        mapView.setMinHeight(360);
        VBox.setVgrow(mapView, Priority.ALWAYS);

        VBox panel = new VBox(12, title, mapView, controls, metrics);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createWorkerPanel() {
        Label title = sectionTitle("Distributed Workers");
        TableView<WorkerRow> table = new TableView<WorkerRow>(workers);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(165);
        table.getColumns().add(workerColumn("Name", "logicalName"));
        table.getColumns().add(workerColumn("Worker ID", "workerId"));
        table.getColumns().add(workerColumn("Status", "status"));
        table.getColumns().add(workerColumn("Proxy", "endpoint"));
        table.getColumns().add(workerColumn("Host/Port", "hostPort"));
        table.getColumns().add(workerColumn("Message", "message"));
        table.getColumns().add(workerColumn("Checked", "checkedAt"));
        table.getColumns().add(workerColumn("Progress", "progress"));

        VBox panel = new VBox(10, title, table);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createActivityPanel() {
        activityLog.setEditable(false);
        activityLog.setWrapText(true);
        activityLog.setPrefRowCount(6);
        activityLog.setStyle("-fx-font-size: 12px;");

        VBox panel = new VBox(10, sectionTitle("Recent Activity"), activityLog);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createRightPanel() {
        VBox right = new VBox(14);
        right.setPrefWidth(330);
        right.setPadding(new Insets(0, 0, 0, 16));
        right.getChildren().addAll(createControlPanel(), createMetricsPanel(), createQueuePanel());
        return right;
    }

    private Parent createControlPanel() {
        runPipelineButton.setMaxWidth(Double.MAX_VALUE);
        runPipelineButton.setStyle("-fx-background-color: #0f766e; -fx-text-fill: white; -fx-font-size: 15px;"
                + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 13 16;");
        runPipelineButton.setOnAction(event -> {
            if (runFullPipelineHandler != null) {
                runFullPipelineHandler.run();
            }
        });

        pipelineStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        pipelineStep.setStyle("-fx-text-fill: #475569;");
        pipelineProgress.setMaxWidth(Double.MAX_VALUE);
        pipelineLog.setEditable(false);
        pipelineLog.setWrapText(true);
        pipelineLog.setPrefRowCount(8);
        pipelineLog.setStyle("-fx-font-size: 12px;");

        Button detect = actionButton("Detect Workers");
        detect.setOnAction(event -> {
            if (detectWorkersHandler == null) {
                detectWorkers();
            } else {
                detectWorkersHandler.run();
            }
        });

        Button start = actionButton("Start Distributed Processing");
        start.setOnAction(event -> startProcessingSimulation());

        Button transfer = actionButton("Transfer Test Bucket");
        transfer.setOnAction(event -> {
            if (transferBucketHandler == null) {
                showNoActiveWorkerForTransfer();
            } else {
                transferBucketHandler.run();
            }
        });

        Button generateBuckets = actionButton("Generate Buckets");
        generateBuckets.setOnAction(event -> {
            if (generateBucketsHandler != null) {
                generateBucketsHandler.run();
            }
        });

        Button distributeBuckets = actionButton("Distribute Buckets");
        distributeBuckets.setOnAction(event -> {
            if (distributeBucketsHandler != null) {
                distributeBucketsHandler.run();
            }
        });

        Button processRemote = actionButton("Process Remote Buckets");
        processRemote.setOnAction(event -> {
            if (processRemoteBucketsHandler != null) {
                processRemoteBucketsHandler.run();
            }
        });

        Button mergeGlobal = actionButton("Merge Global Results");
        mergeGlobal.setOnAction(event -> {
            if (mergeGlobalResultsHandler != null) {
                mergeGlobalResultsHandler.run();
            }
        });

        Button pause = actionButton("Pause");
        pause.setOnAction(event -> pauseSimulation());

        Button reset = actionButton("Reset");
        reset.setOnAction(event -> resetSimulation());

        Button simulate = actionButton("Simulate Workers");
        simulate.setOnAction(event -> simulateWorkers());

        Button clear = actionButton("Clear Logs");
        clear.setOnAction(event -> activityLog.clear());

        VBox advancedBox = new VBox(10, detect, generateBuckets, distributeBuckets, processRemote,
                mergeGlobal, transfer, start, pause, reset, simulate, clear);
        TitledPane advanced = new TitledPane("Avanzado", advancedBox);
        advanced.setExpanded(false);
        advanced.setCollapsible(true);

        VBox panel = new VBox(10, sectionTitle("Pipeline principal"), runPipelineButton, pipelineProgress,
                pipelineStatus, pipelineStep, pipelineLog, advanced);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createMetricsPanel() {
        GridPane grid = metricGrid();
        addMetric(grid, 0, "Configured workers", workersDetected);
        addMetric(grid, 1, "Active workers", activeWorkers);
        addMetric(grid, 2, "Down workers", downWorkers);
        addMetric(grid, 3, "Last scan", lastScanStatus);
        addMetric(grid, 4, "Buckets prepared", bucketsPrepared);
        addMetric(grid, 5, "Buckets assigned", bucketsAssigned);
        addMetric(grid, 6, "Buckets in progress", bucketsInProgress);
        addMetric(grid, 7, "Buckets completed", bucketsCompleted);
        addMetric(grid, 8, "Failed buckets", failedBuckets);
        addMetric(grid, 9, "Transfer target", transferTarget);
        addMetric(grid, 10, "Transfer bucket", transferBucket);
        addMetric(grid, 11, "Chunks sent", transferChunks);
        addMetric(grid, 12, "Bytes sent", transferBytes);
        addMetric(grid, 13, "Transfer result", transferResult);
        addMetric(grid, 14, "Bucket job", bucketizationJob);
        addMetric(grid, 15, "Lines read", recordsRead);
        addMetric(grid, 16, "Valid records", validRecords);
        addMetric(grid, 17, "Invalid lines", invalidRecords);
        addMetric(grid, 18, "Output directory", bucketOutputDirectory);
        addMetric(grid, 19, "Bucket elapsed", bucketElapsed);
        addMetric(grid, 20, "Distribution job", distributionJob);
        addMetric(grid, 21, "Workers used", distributionWorkers);
        addMetric(grid, 22, "Current worker", currentDistributionWorker);
        addMetric(grid, 23, "Current bucket", currentDistributionBucket);
        addMetric(grid, 24, "Distribution result", distributionResult);
        addMetric(grid, 25, "Remote job", remoteProcessingJob);
        addMetric(grid, 26, "Remote workers", remoteWorkersRequested);
        addMetric(grid, 27, "Remote success", remoteWorkersSucceeded);
        addMetric(grid, 28, "Remote failed", remoteWorkersFailed);
        addMetric(grid, 29, "Partial rows", remotePartialRows);
        addMetric(grid, 30, "Remote elapsed", remoteProcessingElapsed);
        addMetric(grid, 31, "Global job", globalMergeJob);
        addMetric(grid, 32, "Workers merged", globalWorkersMerged);
        addMetric(grid, 33, "Merge failed workers", globalWorkersFailed);
        addMetric(grid, 34, "Global rows", globalRows);
        addMetric(grid, 35, "Global valid intervals", globalValidIntervals);
        addMetric(grid, 36, "Global distance", globalDistance);
        addMetric(grid, 37, "Global time", globalTime);
        addMetric(grid, 38, "Merge elapsed", globalMergeElapsed);

        globalProgress.setMaxWidth(Double.MAX_VALUE);
        VBox panel = new VBox(12, sectionTitle("Control Metrics"), grid, new Label("Global progress"), globalProgress);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createQueuePanel() {
        GridPane grid = metricGrid();
        addMetric(grid, 0, "Pending", pendingBuckets);
        addMetric(grid, 1, "Assigned", bucketsAssigned);
        addMetric(grid, 2, "Processing", bucketsInProgress);
        addMetric(grid, 3, "Completed", bucketsCompleted);

        VBox panel = new VBox(12, sectionTitle("Distribution Queue"), grid);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createResultsPanel() {
        TableView<ResultRow> table = new TableView<ResultRow>(results);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(170);
        table.getColumns().add(resultColumn("Item", "route"));
        table.getColumns().add(resultColumn("Job", "year"));
        table.getColumns().add(resultColumn("Period/Bucket", "month"));
        table.getColumns().add(resultColumn("Distance/Records", "averageSpeed"));
        table.getColumns().add(resultColumn("Time/Bytes", "intervals"));
        table.getColumns().add(resultColumn("Intervals/Source", "sourceWorkers"));
        table.getColumns().add(resultColumn("Status", "status"));

        VBox panel = new VBox(10, sectionTitle("Distributed Output Preview"), table);
        panel.setPadding(new Insets(16, 0, 0, 0));
        return panel;
    }

    private void detectWorkers() {
        workers.clear();
        workers.add(new WorkerRow("worker1", "worker-01", "tcp -h localhost -p 10001", "localhost:10001", "READY", "Visual simulation", "demo", "0%"));
        workers.add(new WorkerRow("worker2", "worker-02", "tcp -h localhost -p 10002", "localhost:10002", "READY", "Visual simulation", "demo", "0%"));
        workers.add(new WorkerRow("worker3", "worker-03", "tcp -h localhost -p 10003", "localhost:10003", "IDLE", "Visual simulation", "demo", "0%"));
        workersDetected.setText("3");
        activeWorkers.setText("3");
        downWorkers.setText("0");
        lastScanStatus.setText("simulation");
        setState("Prepared for distributed processing", "READY", "#dcfce7", "#166534");
        appendLog("Simulated worker worker-01 registered.");
        appendLog("Simulated worker worker-02 registered.");
        appendLog("Simulated worker worker-03 registered.");
    }

    public void setDetectWorkersHandler(Runnable detectWorkersHandler) {
        this.detectWorkersHandler = detectWorkersHandler;
    }

    public void setRunFullPipelineHandler(Runnable runFullPipelineHandler) {
        this.runFullPipelineHandler = runFullPipelineHandler;
    }

    public void setTransferBucketHandler(Runnable transferBucketHandler) {
        this.transferBucketHandler = transferBucketHandler;
    }

    public void setGenerateBucketsHandler(Runnable generateBucketsHandler) {
        this.generateBucketsHandler = generateBucketsHandler;
    }

    public void setDistributeBucketsHandler(Runnable distributeBucketsHandler) {
        this.distributeBucketsHandler = distributeBucketsHandler;
    }

    public void setProcessRemoteBucketsHandler(Runnable processRemoteBucketsHandler) {
        this.processRemoteBucketsHandler = processRemoteBucketsHandler;
    }

    public void setMergeGlobalResultsHandler(Runnable mergeGlobalResultsHandler) {
        this.mergeGlobalResultsHandler = mergeGlobalResultsHandler;
    }

    public void showPipelineStarted() {
        stopTimeline();
        runPipelineButton.setDisable(true);
        pipelineLog.clear();
        pipelineProgress.setProgress(0);
        pipelineStatus.setText("Iniciando procesamiento distribuido...");
        pipelineStep.setText("Paso actual: preparando pipeline");
        appendPipelineLog("Iniciando procesamiento distribuido...");
        appendLog("Full distributed pipeline requested.");
        setState("Iniciando procesamiento distribuido", "RUNNING", "#dbeafe", "#1d4ed8");
    }

    public void showPipelineStep(int step, String message) {
        pipelineProgress.setProgress((double) Math.max(0, step - 1) / 6.0);
        pipelineStep.setText("Paso actual: [" + step + "/6] " + message);
        pipelineStatus.setText(message);
        appendPipelineLog("[" + step + "/6] " + message);
        appendLog("Pipeline step " + step + "/6: " + message);
    }

    public void showPipelineStepFinished(int step, String message) {
        pipelineProgress.setProgress((double) step / 6.0);
        pipelineStatus.setText(message);
        appendPipelineLog(message);
    }

    public void showPipelineWarning(String message) {
        appendPipelineLog("Advertencia: " + message);
        appendLog("Pipeline warning: " + message);
    }

    public void showPipelineFinished() {
        pipelineProgress.setProgress(1.0);
        pipelineStatus.setText("Proceso distribuido finalizado correctamente.");
        pipelineStep.setText("Paso actual: finalizado");
        runPipelineButton.setDisable(false);
        appendPipelineLog("Proceso distribuido finalizado correctamente.");
        setState("Finalizado", "DONE", "#dcfce7", "#166534");
    }

    public void showPipelineFailed(String message) {
        pipelineProgress.setProgress(0);
        pipelineStatus.setText(message);
        pipelineStep.setText("Paso actual: detenido por error");
        runPipelineButton.setDisable(false);
        appendPipelineLog(message);
        appendLog("Pipeline failed: " + message);
        setState("Proceso detenido", "ERROR", "#fee2e2", "#991b1b");
    }

    public void showHealthScanStarted(List<WorkerEndpointConfig> endpoints) {
        workers.clear();
        for (WorkerEndpointConfig endpoint : endpoints) {
            workers.add(new WorkerRow(
                    endpoint.getLogicalName(),
                    "unknown",
                    endpoint.getProxyString(),
                    endpoint.getConfiguredHost() + ":" + endpoint.getConfiguredPort(),
                    "CHECKING",
                    "Health check in progress",
                    "now",
                    "0%"
            ));
        }
        workersDetected.setText(String.valueOf(endpoints.size()));
        activeWorkers.setText("0");
        downWorkers.setText("0");
        lastScanStatus.setText("checking");
        appendLog("Scanning configured workers...");
        setState("Checking " + endpoints.size() + " configured workers", "CHECKING", "#dbeafe", "#1d4ed8");
    }

    public void showWorkerScanResults(List<WorkerConnectionResult> results) {
        workers.clear();
        int up = 0;
        int down = 0;
        for (WorkerConnectionResult result : results) {
            if (result.isUp()) {
                up++;
                appendLog(result.getLogicalName() + " health check successful: " + result.getMessage());
            } else {
                down++;
                appendLog(result.getLogicalName() + " health check failed: " + result.getMessage());
            }
            workers.add(new WorkerRow(
                    result.getLogicalName(),
                    result.getWorkerId(),
                    result.getProxy(),
                    result.getHost() + ":" + result.getPort(),
                    result.getStatus(),
                    result.getMessage(),
                    result.getCheckedAt(),
                    "0%"
            ));
        }
        workersDetected.setText(String.valueOf(results.size()));
        activeWorkers.setText(String.valueOf(up));
        downWorkers.setText(String.valueOf(down));
        lastScanStatus.setText(up + " UP / " + down + " DOWN");
        if (down == 0 && up > 0) {
            setState("All configured workers are UP", "UP", "#dcfce7", "#166534");
        } else if (up > 0) {
            setState("Some configured workers are DOWN", "PARTIAL", "#fef3c7", "#92400e");
        } else {
            setState("No configured workers responded", "DOWN", "#fee2e2", "#991b1b");
        }
    }

    public void showTransferRequested() {
        transferResult.setText("checking workers");
        appendLog("Transfer test bucket requested.");
        setState("Preparing bucket transfer", "TRANSFER", "#dbeafe", "#1d4ed8");
    }

    public void showNoActiveWorkerForTransfer() {
        transferTarget.setText("none");
        transferResult.setText("no active worker");
        appendLog("No active worker available for transfer.");
        setState("No active worker available for transfer", "WAITING", "#fef3c7", "#92400e");
    }

    public void showTransferStarted(WorkerConnectionResult worker, String jobId, String bucketId, long totalBytes,
                                    int totalChunks) {
        transferTarget.setText(worker.getWorkerId());
        transferBucket.setText(bucketId);
        transferChunks.setText("0 / " + totalChunks);
        transferBytes.setText(String.valueOf(totalBytes));
        transferResult.setText("in progress");
        bucketsPrepared.setText("1");
        bucketsAssigned.setText("1");
        bucketsInProgress.setText("1");
        bucketsCompleted.setText("0");
        failedBuckets.setText("0");
        pendingBuckets.setText("0");
        globalProgress.setProgress(0);
        appendLog("Selected worker " + worker.getWorkerId() + " for transfer.");
        appendLog("Starting transfer job " + jobId + " bucket " + bucketId + ".");
        updateWorker(worker.getWorkerId(), "RECEIVING", "1", "0%");
        setState("Transferring test bucket", "TRANSFER", "#dbeafe", "#1d4ed8");
    }

    public void showTransferProgress(String message, int sentChunks, int totalChunks, long totalBytes) {
        transferChunks.setText(sentChunks + " / " + totalChunks);
        transferBytes.setText(String.valueOf(totalBytes));
        double progress = totalChunks == 0 ? 0 : (double) sentChunks / (double) totalChunks;
        globalProgress.setProgress(progress);
        appendLog(message);
    }

    public void showTransferFinished(BucketTransferResult result) {
        transferResult.setText(result.isSuccess() ? "success" : "failed");
        transferBucket.setText(result.getBucketId());
        transferChunks.setText(result.getTotalChunks() + " / " + result.getTotalChunks());
        transferBytes.setText(String.valueOf(result.getTotalBytes()));
        if (result.isSuccess()) {
            bucketsInProgress.setText("0");
            bucketsCompleted.setText("1");
            failedBuckets.setText("0");
            globalProgress.setProgress(1.0);
            updateWorker(result.getWorker().getWorkerId(), "RECEIVED", "1", "100%");
            appendLog("Transfer completed successfully.");
            appendLog("Worker stored bucket at " + result.getRemotePath() + ".");
            setState("Bucket transfer completed", "DONE", "#dcfce7", "#166534");
        } else {
            bucketsInProgress.setText("0");
            failedBuckets.setText("1");
            appendLog("Transfer failed: " + result.getMessage());
            setState("Bucket transfer failed", "ERROR", "#fee2e2", "#991b1b");
        }
    }

    public void showBucketizationStarted(BucketizationConfig config) {
        stopTimeline();
        results.clear();
        bucketizationJob.setText(config.getJobId());
        recordsRead.setText("0");
        validRecords.setText("0");
        invalidRecords.setText("0");
        bucketOutputDirectory.setText(config.getOutputDirectory().toString());
        bucketElapsed.setText("running");
        bucketsPrepared.setText(String.valueOf(config.getBucketCount()));
        pendingBuckets.setText(String.valueOf(config.getBucketCount()));
        bucketsCompleted.setText("0");
        failedBuckets.setText("0");
        globalProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        appendLog("Generate Buckets requested.");
        appendLog("Bucketization job " + config.getJobId() + " started.");
        appendLog("Input CSV: " + config.getDatagramsPath() + (config.isDemoDataset() ? " (demo)" : ""));
        appendLog("Partition by Key enabled with bucketCount=" + config.getBucketCount() + " using busId.");
        setState("Creating buckets", "BUCKETS", "#dbeafe", "#1d4ed8");
    }

    public void showBucketizationProgress(String message, long totalLinesRead, long validRecordsWritten) {
        recordsRead.setText(String.valueOf(totalLinesRead));
        validRecords.setText(String.valueOf(validRecordsWritten));
        appendLog(message);
    }

    public void showBucketizationFinished(BucketizationResult result) {
        bucketizationJob.setText(result.getJobId());
        recordsRead.setText(String.valueOf(result.getTotalLinesRead()));
        validRecords.setText(String.valueOf(result.getValidRecordsWritten()));
        invalidRecords.setText(String.valueOf(result.getInvalidLines()));
        bucketOutputDirectory.setText(String.valueOf(result.getOutputDirectory()));
        bucketElapsed.setText(result.getElapsedMillis() + " ms");
        updateMapSample(result);
        globalProgress.setProgress(result.isSuccess() ? 1.0 : 0);
        bucketsPrepared.setText(String.valueOf(result.getBucketCount()));
        bucketsCompleted.setText(result.isSuccess() ? String.valueOf(result.getBuckets().size()) : "0");
        failedBuckets.setText(result.isSuccess() ? "0" : "1");
        pendingBuckets.setText("0");
        results.clear();
        for (BucketInfo bucket : result.getBuckets()) {
            if (bucket.getRecordsWritten() > 0) {
                results.add(new ResultRow(bucket.getBucketId(), result.getJobId(), String.valueOf(bucket.getBucketIndex()),
                        String.valueOf(bucket.getRecordsWritten()), String.valueOf(bucket.getSizeBytes()),
                        bucket.getPath().getFileName().toString(), "Ready"));
            }
        }
        if (result.isSuccess()) {
            appendLog("Buckets ready in " + result.getOutputDirectory() + ".");
            appendLog("Records written: " + result.getValidRecordsWritten() + "; invalid lines: " + result.getInvalidLines() + ".");
            appendLog("Visual sample ready: kept=" + result.getVisualSampleTotalKept()
                    + ", seen=" + result.getVisualSampleTotalSeen()
                    + ", step=" + result.getVisualSampleStep()
                    + ", max=" + result.getVisualSampleMaxPoints() + ".");
            appendLog("Missing busId=" + result.getMissingBusId()
                    + ", missing routeId=" + result.getMissingRouteId()
                    + ", missing timestamp=" + result.getMissingTimestamp()
                    + ", missing coordinates=" + result.getMissingCoordinates()
                    + ", missing odometer=" + result.getMissingOdometer() + ".");
            setState("Buckets ready", "READY", "#dcfce7", "#166534");
        } else {
            appendLog(result.getMessage());
            setState("Bucketization failed", "ERROR", "#fee2e2", "#991b1b");
        }
    }

    public void showBucketizationFailed(String message) {
        failedBuckets.setText("1");
        bucketElapsed.setText("failed");
        globalProgress.setProgress(0);
        appendLog("Bucketization failed: " + message);
        setState("Bucketization failed", "ERROR", "#fee2e2", "#991b1b");
    }

    public void showDistributionStarted(String jobId, List<BucketDistributionItem> items,
                                        List<WorkerConnectionResult> activeWorkers) {
        stopTimeline();
        results.clear();
        distributionJob.setText(jobId);
        distributionWorkers.setText(String.valueOf(activeWorkers.size()));
        currentDistributionWorker.setText("none");
        currentDistributionBucket.setText("none");
        distributionResult.setText("running");
        bucketsPrepared.setText(String.valueOf(items.size()));
        pendingBuckets.setText(String.valueOf(items.size()));
        bucketsAssigned.setText("0");
        bucketsInProgress.setText("0");
        bucketsCompleted.setText("0");
        failedBuckets.setText("0");
        globalProgress.setProgress(items.isEmpty() ? 0 : 0.0);
        appendLog("Bucket distribution requested.");
        appendLog("Located " + items.size() + " generated bucket files.");
        appendLog("Detected " + activeWorkers.size() + " active workers.");
        for (BucketDistributionItem item : items) {
            appendLog("Queued " + item.getFileName() + ".");
            results.add(new ResultRow(
                    item.getBucketId(),
                    jobId,
                    item.getFileName(),
                    item.getAssignedWorker(),
                    String.valueOf(item.getSizeBytes()),
                    String.valueOf(item.getAttempts()),
                    item.getStatus().name()
            ));
        }
        setState("Distributing buckets", "DISTRIBUTING", "#dbeafe", "#1d4ed8");
    }

    public void showDistributionItemUpdated(BucketDistributionItem item) {
        currentDistributionBucket.setText(item.getBucketId());
        if (item.getAssignedWorker() != null && !item.getAssignedWorker().isEmpty()) {
            currentDistributionWorker.setText(item.getAssignedWorker());
        }
        upsertDistributionRow(item);
        updateDistributionCounters();
        appendDistributionLog(item);
    }

    public void showDistributionTransferProgress(BucketDistributionItem item, WorkerConnectionResult worker,
                                                 String message, int sentChunks, int totalChunks, long totalBytes) {
        currentDistributionWorker.setText(worker.getLogicalName());
        currentDistributionBucket.setText(item.getBucketId());
        transferBucket.setText(item.getBucketId());
        transferTarget.setText(worker.getLogicalName());
        transferChunks.setText(sentChunks + " / " + totalChunks);
        transferBytes.setText(String.valueOf(totalBytes));
        transferResult.setText("distribution");
        appendLog(message);
    }

    public void showDistributionFinished(DistributionResult result) {
        distributionJob.setText(result.getJobId());
        distributionWorkers.setText(String.valueOf(result.getWorkersUsed()));
        bucketsPrepared.setText(String.valueOf(result.getTotalBuckets()));
        pendingBuckets.setText("0");
        bucketsAssigned.setText("0");
        bucketsInProgress.setText("0");
        bucketsCompleted.setText(String.valueOf(result.getSentBuckets()));
        failedBuckets.setText(String.valueOf(result.getFailedBuckets()));
        distributionResult.setText(result.isSuccess() ? "success" : "failed");
        globalProgress.setProgress(result.getTotalBuckets() == 0 ? 0 : (double) result.getSentBuckets() / result.getTotalBuckets());
        for (BucketDistributionItem item : result.getItems()) {
            upsertDistributionRow(item);
        }
        appendLog("Distribution completed: sent=" + result.getSentBuckets()
                + " failed=" + result.getFailedBuckets()
                + " elapsed=" + result.getElapsedMillis() + " ms.");
        if (result.isSuccess()) {
            setState("Bucket distribution completed", "SENT", "#dcfce7", "#166534");
        } else {
            appendLog(result.getMessage());
            setState("Bucket distribution finished with failures", "FAILED", "#fee2e2", "#991b1b");
        }
    }

    public void showDistributionFailed(String message) {
        distributionResult.setText("failed");
        failedBuckets.setText("1");
        globalProgress.setProgress(0);
        appendLog("Bucket distribution failed: " + message);
        setState("Bucket distribution failed", "ERROR", "#fee2e2", "#991b1b");
    }

    public void showRemoteProcessingStarted(String jobId, List<WorkerConnectionResult> workers) {
        results.clear();
        remoteProcessingJob.setText(jobId);
        remoteWorkersRequested.setText(String.valueOf(workers.size()));
        remoteWorkersSucceeded.setText("0");
        remoteWorkersFailed.setText("0");
        remotePartialRows.setText("0");
        remoteProcessingElapsed.setText("running");
        globalProgress.setProgress(workers.isEmpty() ? 0 : 0.0);
        appendLog("Process Remote Buckets requested.");
        appendLog("Remote partial processing job " + jobId + " started.");
        appendLog("Requesting partial results from " + workers.size() + " worker(s).");
        setState("Requesting remote partial results", "REMOTE", "#dbeafe", "#1d4ed8");
    }

    public void showRemoteWorkerRequested(WorkerConnectionResult worker, String message) {
        appendLog(worker.getLogicalName() + ": " + message);
    }

    public void showRemoteWorkerResult(RemoteWorkerPartialResult result) {
        appendLog("Remote partial result from " + result.getWorkerLogicalName()
                + ": success=" + result.isSuccess()
                + ", buckets=" + result.getProcessedBuckets()
                + ", rows=" + result.getResultCount()
                + ", validIntervals=" + result.getValidIntervals() + ".");
        if (!result.isSuccess()) {
            appendLog(result.getWorkerLogicalName() + " returned error: " + result.getMessage());
        }
        results.add(new ResultRow(
                "worker " + result.getWorkerLogicalName(),
                result.getJobId(),
                result.getWorkerId(),
                String.valueOf(result.getProcessedBuckets()),
                String.valueOf(result.getValidIntervals()),
                String.valueOf(result.getResultCount()),
                result.isSuccess() ? "Partial OK" : "Partial Error"
        ));
        for (RemoteRouteMonthPartial partial : result.getRouteMonthResults()) {
            results.add(new ResultRow(
                    partial.getWorkerId() + " " + partial.getRouteId(),
                    result.getJobId(),
                    partial.getYear() + "-" + String.format("%02d", partial.getMonth()),
                    format(partial.getTotalDistanceMeters()) + " m",
                    format(partial.getTotalTimeSeconds()) + " s",
                    partial.getValidIntervals() + " intervals",
                    "Partial avg " + format(partial.getAverageKmh()) + " km/h"
            ));
        }
    }

    public void showRemoteProcessingFinished(RemoteProcessingSummary summary) {
        remoteProcessingJob.setText(summary.getJobId());
        remoteWorkersRequested.setText(String.valueOf(summary.getTotalWorkersRequested()));
        remoteWorkersSucceeded.setText(String.valueOf(summary.getSuccessfulWorkers()));
        remoteWorkersFailed.setText(String.valueOf(summary.getFailedWorkers()));
        remotePartialRows.setText(String.valueOf(summary.getTotalPartialResults()));
        remoteProcessingElapsed.setText(summary.getElapsedMillis() + " ms");
        globalProgress.setProgress(summary.getTotalWorkersRequested() == 0 ? 0 : 1.0);
        appendLog("Remote partial processing completed: workers=" + summary.getTotalWorkersRequested()
                + ", success=" + summary.getSuccessfulWorkers()
                + ", failed=" + summary.getFailedWorkers()
                + ", partialRows=" + summary.getTotalPartialResults()
                + ", elapsed=" + summary.getElapsedMillis() + " ms.");
        setState("Remote partial results received", "PARTIAL", "#dcfce7", "#166534");
    }

    public void showRemoteProcessingFailed(String message) {
        remoteWorkersFailed.setText("1");
        remoteProcessingElapsed.setText("failed");
        appendLog("Remote processing failed: " + message);
        setState("Remote processing failed", "ERROR", "#fee2e2", "#991b1b");
    }

    public void showGlobalMergeStarted(String jobId, int partialWorkers) {
        results.clear();
        globalMergeJob.setText(jobId);
        globalWorkersMerged.setText("0");
        globalWorkersFailed.setText("0");
        globalRows.setText("0");
        globalValidIntervals.setText("0");
        globalDistance.setText("0 m");
        globalTime.setText("0 s");
        globalMergeElapsed.setText("running");
        globalProgress.setProgress(0.0);
        appendLog("Merge Global Results requested.");
        appendLog("Merging remote partial results for job " + jobId + " from " + partialWorkers + " worker result(s).");
        setState("Merging global route-month results", "MERGE", "#dbeafe", "#1d4ed8");
    }

    public void showGlobalMergeFinished(GlobalMergeResult result) {
        globalMergeJob.setText(result.getJobId());
        globalWorkersMerged.setText(String.valueOf(result.getTotalWorkersMerged()));
        globalWorkersFailed.setText(String.valueOf(result.getFailedWorkers()));
        globalRows.setText(String.valueOf(result.getTotalGlobalRows()));
        globalValidIntervals.setText(String.valueOf(result.getCounters().getValidIntervals()));
        globalDistance.setText(format(totalDistance(result)) + " m");
        globalTime.setText(format(totalTime(result)) + " s");
        globalMergeElapsed.setText(result.getElapsedMillis() + " ms");
        globalProgress.setProgress(result.isSuccess() ? 1.0 : 0.0);
        results.clear();
        for (GlobalMergedResultRow row : result.getRows()) {
            results.add(new ResultRow(
                    row.getRouteId(),
                    result.getJobId(),
                    row.getYearMonth(),
                    format(row.getTotalDistanceMeters()) + " m",
                    format(row.getTotalTimeSeconds()) + " s",
                    row.getValidIntervals() + " intervals",
                    "Global avg " + format(row.getAverageKmh()) + " km/h from "
                            + row.getContributingWorkers() + " worker(s)"
            ));
        }
        appendLog("Global merge completed: success=" + result.isSuccess()
                + ", workersMerged=" + result.getTotalWorkersMerged()
                + ", failedWorkers=" + result.getFailedWorkers()
                + ", partialRows=" + result.getTotalPartialRows()
                + ", globalRows=" + result.getTotalGlobalRows()
                + ", elapsed=" + result.getElapsedMillis() + " ms.");
        appendLog(result.getMessage());
        if (result.isSuccess()) {
            setState("Global merge completed", "GLOBAL", "#dcfce7", "#166534");
        } else {
            setState("Global merge did not produce rows", "EMPTY", "#fef3c7", "#92400e");
        }
    }

    public void showGlobalMergeFailed(String message) {
        globalMergeElapsed.setText("failed");
        globalProgress.setProgress(0);
        appendLog("Global merge failed: " + message);
        setState("Global merge failed", "ERROR", "#fee2e2", "#991b1b");
    }

    private void simulateWorkers() {
        detectWorkers();
        bucketsPrepared.setText("24");
        pendingBuckets.setText("24");
        appendLog("Bucket queue simulation prepared.");
    }

    private void startProcessingSimulation() {
        if (workers.isEmpty()) {
            simulateWorkers();
        }
        stopTimeline();
        setState("Processing distributed visual simulation", "PROCESSING", "#dbeafe", "#1d4ed8");
        appendLog("Visual distributed processing simulation started.");
        results.clear();

        processingTimeline = new Timeline(
                step(0, 0.15, "18", "6", "6", "0", "worker-01", "PROCESSING", "25%", "Worker worker-01 changed to PROCESSING."),
                step(1, 0.35, "12", "12", "9", "3", "worker-02", "PROCESSING", "40%", "Worker worker-02 changed to PROCESSING."),
                step(2, 0.58, "7", "17", "7", "10", "worker-03", "PROCESSING", "55%", "Worker worker-03 changed to PROCESSING."),
                step(3, 0.78, "3", "21", "4", "18", "worker-01", "DONE", "100%", "Partial visual results received from worker-01."),
                step(4, 1.0, "0", "24", "0", "24", "worker-02", "DONE", "100%", "Visual distributed processing simulation completed.")
        );
        processingTimeline.setOnFinished(event -> {
            setState("Prepared for distributed processing", "DONE", "#dcfce7", "#166534");
            results.add(new ResultRow("Demo route", "2026", "06", "visual demo", "0", "worker-01, worker-02", "Prepared"));
        });
        processingTimeline.playFromStart();
    }

    private KeyFrame step(int seconds, double progress, String pending, String assigned, String processing,
                          String completed, String workerId, String workerStatus, String workerProgress, String log) {
        return new KeyFrame(Duration.seconds(seconds), event -> {
            globalProgress.setProgress(progress);
            pendingBuckets.setText(pending);
            bucketsAssigned.setText(assigned);
            bucketsInProgress.setText(processing);
            bucketsCompleted.setText(completed);
            bucketsPrepared.setText("24");
            updateWorker(workerId, workerStatus, assigned, workerProgress);
            appendLog(log);
        });
    }

    private void pauseSimulation() {
        if (processingTimeline != null) {
            processingTimeline.pause();
        }
        setState("Paused visual simulation", "PAUSED", "#fef3c7", "#92400e");
        appendLog("Visual distributed processing simulation paused.");
    }

    private void resetSimulation() {
        stopTimeline();
        workers.clear();
        results.clear();
        globalProgress.setProgress(0);
        workersDetected.setText("0");
        activeWorkers.setText("0");
        downWorkers.setText("0");
        lastScanStatus.setText("not scanned");
        bucketsPrepared.setText("0");
        bucketsAssigned.setText("0");
        bucketsInProgress.setText("0");
        bucketsCompleted.setText("0");
        failedBuckets.setText("0");
        pendingBuckets.setText("0");
        transferTarget.setText("none");
        transferBucket.setText("none");
        transferChunks.setText("0 / 0");
        transferBytes.setText("0");
        transferResult.setText("not started");
        bucketizationJob.setText("none");
        recordsRead.setText("0");
        validRecords.setText("0");
        invalidRecords.setText("0");
        bucketOutputDirectory.setText("none");
        bucketElapsed.setText("0 ms");
        distributionJob.setText("none");
        distributionWorkers.setText("0");
        currentDistributionWorker.setText("none");
        currentDistributionBucket.setText("none");
        distributionResult.setText("not started");
        remoteProcessingJob.setText("none");
        remoteWorkersRequested.setText("0");
        remoteWorkersSucceeded.setText("0");
        remoteWorkersFailed.setText("0");
        remotePartialRows.setText("0");
        remoteProcessingElapsed.setText("0 ms");
        globalMergeJob.setText("none");
        globalWorkersMerged.setText("0");
        globalWorkersFailed.setText("0");
        globalRows.setText("0");
        globalValidIntervals.setText("0");
        globalDistance.setText("0 m");
        globalTime.setText("0 s");
        globalMergeElapsed.setText("0 ms");
        mapStatus.setText("waiting for bucketization");
        mapSampleKept.setText("0");
        mapSampleSeen.setText("0");
        mapSampleStep.setText("1");
        mapSampleLimit.setText("0");
        resetRouteFilterOptions();
        mapView.resetPlayback();
        setState("Waiting for workers", "WAITING", "#dbeafe", "#1d4ed8");
        appendLog("Visual simulation reset.");
    }

    private void updateWorker(String workerId, String status, String assigned, String progress) {
        for (WorkerRow worker : workers) {
            if (worker.workerId.get().equals(workerId) || worker.logicalName.get().equals(workerId)) {
                worker.status.set(status);
                worker.message.set("Assigned " + assigned);
                worker.progress.set(progress);
            }
        }
    }

    private void stopTimeline() {
        if (processingTimeline != null) {
            processingTimeline.stop();
        }
    }

    private void setState(String text, String badge, String background, String textColor) {
        stateLabel.setText(text);
        stateBadge.setText(badge);
        stateBadge.setStyle(badgeStyle(background, textColor));
    }

    private void appendLog(String message) {
        activityLog.appendText(message + System.lineSeparator());
    }

    private void appendPipelineLog(String message) {
        pipelineLog.appendText(message + System.lineSeparator());
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private void updateMapSample(BucketizationResult result) {
        mapSampleKept.setText(String.valueOf(result.getVisualSampleTotalKept()));
        mapSampleSeen.setText(String.valueOf(result.getVisualSampleTotalSeen()));
        mapSampleStep.setText(String.valueOf(result.getVisualSampleStep()));
        mapSampleLimit.setText(String.valueOf(result.getVisualSampleMaxPoints()));
        updateRouteFilterOptions(result.getVisualSamplePoints());
        if (result.isSuccess() && !result.getVisualSamplePoints().isEmpty()) {
            mapView.loadPoints(result.getVisualSamplePoints());
            mapStatus.setText("sample loaded");
        } else if (result.isSuccess()) {
            mapStatus.setText("no visual sample points");
            mapView.resetPlayback();
        } else {
            mapStatus.setText("bucketization failed");
            mapView.resetPlayback();
        }
    }

    private void playMap() {
        mapView.play();
        mapStatus.setText(mapView.getVisiblePointCount() == 0 ? "no points loaded" : "playing");
        appendLog("Map playback play requested with " + mapView.getVisiblePointCount() + " visible sampled point(s).");
    }

    private void pauseMap() {
        mapView.pause();
        mapStatus.setText("paused");
        appendLog("Map playback paused.");
    }

    private void resetMap() {
        mapView.resetPlayback();
        mapStatus.setText("reset");
        appendLog("Map playback reset.");
    }

    public void playMapFromPipeline() {
        playMap();
        appendPipelineLog("Mapa cargado con " + mapView.getVisiblePointCount() + " punto(s) visibles.");
    }

    public int getVisibleMapPointCount() {
        return mapView.getVisiblePointCount();
    }

    private void updateRouteFilterOptions(List<master.map.MapPlaybackPoint> points) {
        Set<String> routeIds = new TreeSet<String>();
        if (points != null) {
            for (master.map.MapPlaybackPoint point : points) {
                String routeId = point.getRouteId();
                if (routeId != null && !routeId.trim().isEmpty()) {
                    routeIds.add(routeId.trim());
                }
            }
        }

        routeFilterComboBox.getItems().clear();
        routeFilterComboBox.getItems().add("Todas las rutas");
        routeFilterComboBox.getItems().addAll(new ArrayList<String>(routeIds));
        routeFilterComboBox.getSelectionModel().select("Todas las rutas");
        routeFilterComboBox.setDisable(routeIds.isEmpty());
        visualRouteLabel.setText(routeIds.isEmpty()
                ? "Ruta visual: Sin rutas visuales disponibles"
                : "Ruta visual: Todas las rutas");
        mapView.filterRoute(null);
    }

    private void resetRouteFilterOptions() {
        routeFilterComboBox.getItems().clear();
        routeFilterComboBox.getItems().add("Todas las rutas");
        routeFilterComboBox.getSelectionModel().select("Todas las rutas");
        routeFilterComboBox.setDisable(true);
        visualRouteLabel.setText("Ruta visual: Sin rutas visuales disponibles");
        mapView.filterRoute(null);
    }

    private void applyRouteFilter() {
        String selected = routeFilterComboBox.getSelectionModel().getSelectedItem();
        if (selected == null || "Todas las rutas".equals(selected)) {
            mapView.filterRoute(null);
            visualRouteLabel.setText("Ruta visual: Todas las rutas");
            mapStatus.setText(mapView.getLoadedPointCount() == 0 ? "no visual sample points" : "all routes");
            appendLog("Map route filter changed: all routes.");
        } else {
            mapView.filterRoute(selected);
            visualRouteLabel.setText("Ruta visual: " + selected);
            mapStatus.setText("route filter " + selected);
            appendLog("Map route filter changed: " + selected + ".");
        }
    }

    private double totalDistance(GlobalMergeResult result) {
        double total = 0;
        for (GlobalMergedResultRow row : result.getRows()) {
            total += row.getTotalDistanceMeters();
        }
        return total;
    }

    private double totalTime(GlobalMergeResult result) {
        double total = 0;
        for (GlobalMergedResultRow row : result.getRows()) {
            total += row.getTotalTimeSeconds();
        }
        return total;
    }

    private void upsertDistributionRow(BucketDistributionItem item) {
        for (ResultRow row : results) {
            if (row.route.get().equals(item.getBucketId())) {
                row.year.set(distributionJob.getText());
                row.month.set(item.getFileName());
                row.averageSpeed.set(item.getAssignedWorker());
                row.intervals.set(String.valueOf(item.getSizeBytes()));
                row.sourceWorkers.set(String.valueOf(item.getAttempts()));
                row.status.set(item.getStatus().name());
                return;
            }
        }
        results.add(new ResultRow(
                item.getBucketId(),
                distributionJob.getText(),
                item.getFileName(),
                item.getAssignedWorker(),
                String.valueOf(item.getSizeBytes()),
                String.valueOf(item.getAttempts()),
                item.getStatus().name()
        ));
    }

    private void updateDistributionCounters() {
        int pending = 0;
        int assigned = 0;
        int transferring = 0;
        int sent = 0;
        int failed = 0;
        for (ResultRow row : results) {
            String status = row.status.get();
            if (BucketDistributionStatus.PENDING.name().equals(status)) {
                pending++;
            } else if (BucketDistributionStatus.ASSIGNED.name().equals(status)) {
                assigned++;
            } else if (BucketDistributionStatus.TRANSFERRING.name().equals(status)) {
                transferring++;
            } else if (BucketDistributionStatus.SENT.name().equals(status)) {
                sent++;
            } else if (BucketDistributionStatus.FAILED.name().equals(status)) {
                failed++;
            }
        }
        pendingBuckets.setText(String.valueOf(pending));
        bucketsAssigned.setText(String.valueOf(assigned));
        bucketsInProgress.setText(String.valueOf(transferring));
        bucketsCompleted.setText(String.valueOf(sent));
        failedBuckets.setText(String.valueOf(failed));
        int total = Math.max(1, results.size());
        globalProgress.setProgress((double) (sent + failed) / total);
    }

    private void appendDistributionLog(BucketDistributionItem item) {
        if (item.getStatus() == BucketDistributionStatus.ASSIGNED) {
            appendLog("Assigning " + item.getFileName() + " to " + item.getAssignedWorker() + ".");
        } else if (item.getStatus() == BucketDistributionStatus.TRANSFERRING) {
            appendLog("Transferring " + item.getFileName() + " to " + item.getAssignedWorker() + ".");
        } else if (item.getStatus() == BucketDistributionStatus.SENT) {
            appendLog(item.getFileName() + " SENT.");
        } else if (item.getStatus() == BucketDistributionStatus.FAILED) {
            appendLog(item.getFileName() + " FAILED: " + item.getMessage());
        }
    }

    private Button actionButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: #1f4f82; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 5; -fx-padding: 9 12;");
        return button;
    }

    private GridPane metricGrid() {
        GridPane grid = new GridPane();
        grid.setVgap(9);
        grid.setHgap(12);
        ColumnConstraints left = new ColumnConstraints();
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setMinWidth(60);
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

    private TableColumn<WorkerRow, String> workerColumn(String title, String property) {
        TableColumn<WorkerRow, String> column = new TableColumn<WorkerRow, String>(title);
        column.setCellValueFactory(cell -> cell.getValue().value(property));
        return column;
    }

    private TableColumn<ResultRow, String> resultColumn(String title, String property) {
        TableColumn<ResultRow, String> column = new TableColumn<ResultRow, String>(title);
        column.setCellValueFactory(cell -> cell.getValue().value(property));
        return column;
    }

    public static final class WorkerRow {
        private final SimpleStringProperty logicalName;
        private final SimpleStringProperty workerId;
        private final SimpleStringProperty endpoint;
        private final SimpleStringProperty hostPort;
        private final SimpleStringProperty status;
        private final SimpleStringProperty message;
        private final SimpleStringProperty checkedAt;
        private final SimpleStringProperty progress;

        private WorkerRow(String logicalName, String workerId, String endpoint, String hostPort, String status,
                          String message, String checkedAt, String progress) {
            this.logicalName = new SimpleStringProperty(logicalName);
            this.workerId = new SimpleStringProperty(workerId);
            this.endpoint = new SimpleStringProperty(endpoint);
            this.hostPort = new SimpleStringProperty(hostPort);
            this.status = new SimpleStringProperty(status);
            this.message = new SimpleStringProperty(message);
            this.checkedAt = new SimpleStringProperty(checkedAt);
            this.progress = new SimpleStringProperty(progress);
        }

        private SimpleStringProperty value(String property) {
            if ("logicalName".equals(property)) {
                return logicalName;
            }
            if ("workerId".equals(property)) {
                return workerId;
            }
            if ("endpoint".equals(property)) {
                return endpoint;
            }
            if ("hostPort".equals(property)) {
                return hostPort;
            }
            if ("status".equals(property)) {
                return status;
            }
            if ("message".equals(property)) {
                return message;
            }
            if ("checkedAt".equals(property)) {
                return checkedAt;
            }
            return progress;
        }
    }

    public static final class ResultRow {
        private final SimpleStringProperty route;
        private final SimpleStringProperty year;
        private final SimpleStringProperty month;
        private final SimpleStringProperty averageSpeed;
        private final SimpleStringProperty intervals;
        private final SimpleStringProperty sourceWorkers;
        private final SimpleStringProperty status;

        private ResultRow(String route, String year, String month, String averageSpeed, String intervals,
                          String sourceWorkers, String status) {
            this.route = new SimpleStringProperty(route);
            this.year = new SimpleStringProperty(year);
            this.month = new SimpleStringProperty(month);
            this.averageSpeed = new SimpleStringProperty(averageSpeed);
            this.intervals = new SimpleStringProperty(intervals);
            this.sourceWorkers = new SimpleStringProperty(sourceWorkers);
            this.status = new SimpleStringProperty(status);
        }

        private SimpleStringProperty value(String property) {
            if ("route".equals(property)) {
                return route;
            }
            if ("year".equals(property)) {
                return year;
            }
            if ("month".equals(property)) {
                return month;
            }
            if ("averageSpeed".equals(property)) {
                return averageSpeed;
            }
            if ("intervals".equals(property)) {
                return intervals;
            }
            if ("sourceWorkers".equals(property)) {
                return sourceWorkers;
            }
            return status;
        }
    }
}
