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
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
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
import sitm.ProcessingCountersDto;

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
    private final Label masterBucketsDeleted = new Label("0");
    private final Label masterBucketsRetained = new Label("0");
    private final Label storageFreed = new Label("0 B");
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
        root.setStyle("-fx-background-color: #eef3f8;");

        root.setTop(createHeader());
        root.setCenter(createDashboardBody());

        appendLog("Master node initialized.");
        appendLog("Waiting for worker registration.");
        appendLog("Distributed dashboard ready.");
        appendLog("No distributed job running.");
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #eef3f8; -fx-background: #eef3f8;");
        return scrollPane;
    }

    private Parent createHeader() {
        Label title = new Label("SITM-MIO V3 Distribuido - Master");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #18212f;");

        Label subtitle = new Label("Procesamiento distribuido con ICE, buckets y Fork/Join en workers");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        stateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        stateBadge.setStyle(badgeStyle("#dbeafe", "#1d4ed8"));

        VBox copy = new VBox(4, title, subtitle, stateLabel);
        HBox header = new HBox(14, copy, spacer(), stateBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 12, 22));
        return header;
    }

    private Parent createDashboardBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(0, 22, 22, 22));

        HBox mainArea = new HBox(14);
        VBox mapArea = new VBox(10, createMapFilters(), mapView);
        mapView.setPrefHeight(390);
        mapView.setMinHeight(360);
        mapView.setMaxHeight(430);
        HBox.setHgrow(mapArea, Priority.ALWAYS);

        VBox summary = createSummaryCard();
        summary.setPrefWidth(360);
        summary.setMinWidth(320);
        summary.setMaxWidth(420);

        mainArea.getChildren().addAll(mapArea, summary);

        VBox lowerArea = new VBox(10, createResultsPanel(), createCompactLogPanel());
        VBox.setVgrow(lowerArea, Priority.ALWAYS);
        body.getChildren().addAll(mainArea, lowerArea);
        return body;
    }

    private Parent createMapFilters() {
        routeFilterComboBox.setDisable(true);
        routeFilterComboBox.setPrefWidth(240);
        routeFilterComboBox.setOnAction(event -> applyRouteFilter());
        visualRouteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        Label routeLabel = new Label("Ruta");
        routeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox routeFilter = new VBox(4, routeLabel, routeFilterComboBox);
        routeFilter.setAlignment(Pos.CENTER_LEFT);

        VBox status = new VBox(4, visualRouteLabel, mapStatus);
        status.setAlignment(Pos.CENTER_LEFT);
        mapStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #0f766e;");

        HBox filters = new HBox(10, routeFilter, status);
        filters.setPadding(new Insets(12));
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setStyle(panelStyle());
        return filters;
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

    private VBox createSummaryCard() {
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
        pipelineLog.setPrefRowCount(5);
        pipelineLog.setStyle("-fx-font-size: 12px;");

        Label title = new Label("Procesamiento distribuido");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #18212f;");
        Label subtitle = new Label("Master + workers ICE");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox mainMetrics = new VBox(7);
        mainMetrics.getChildren().addAll(
                metricRow("Estado", stateLabel),
                metricRow("Workers activos", activeWorkers),
                metricRow("Workers configurados", workersDetected),
                metricRow("Lineas leidas", recordsRead),
                metricRow("Datagramas validos", validRecords),
                metricRow("Datagramas invalidos", invalidRecords),
                metricRow("Buckets generados", bucketsPrepared),
                metricRow("Buckets enviados", bucketsCompleted),
                metricRow("Buckets borrados", masterBucketsDeleted),
                metricRow("Buckets retenidos", masterBucketsRetained),
                metricRow("Espacio liberado", storageFreed),
                metricRow("Workers fusionados", globalWorkersMerged),
                metricRow("Rutas con resultado", globalRows),
                metricRow("Intervalos validos", globalValidIntervals),
                metricRow("Distancia global", globalDistance),
                metricRow("Tiempo global", globalTime),
                metricRow("Tiempo total", globalMergeElapsed),
                metricRow("Puntos de mapa", mapSampleKept)
        );

        ScrollPane metricsScroll = new ScrollPane(mainMetrics);
        metricsScroll.setFitToWidth(true);
        metricsScroll.setMinHeight(190);
        metricsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(metricsScroll, Priority.ALWAYS);

        VBox panel = new VBox(10, title, subtitle, pipelineStatus, runPipelineButton, pipelineProgress,
                pipelineStep, new Separator(), metricsScroll, new Separator(), pipelineLog);
        panel.setPadding(new Insets(16));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createResultsPanel() {
        TableView<ResultRow> table = new TableView<ResultRow>(results);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(220);
        table.setPlaceholder(new Label("Sin resultados globales para mostrar"));
        table.getColumns().add(resultColumn("Ruta", "route"));
        table.getColumns().add(resultColumn("Anio", "year"));
        table.getColumns().add(resultColumn("Mes", "month"));
        table.getColumns().add(resultColumn("Velocidad promedio", "averageSpeed"));
        table.getColumns().add(resultColumn("Intervalos validos", "intervals"));
        table.getColumns().add(resultColumn("Estado", "status"));

        VBox panel = new VBox(10, sectionTitle("Promedios globales por ruta"), table);
        panel.setPadding(new Insets(12));
        panel.setStyle(panelStyle());
        return panel;
    }

    private Parent createCompactLogPanel() {
        activityLog.setEditable(false);
        activityLog.setWrapText(true);
        activityLog.setPrefRowCount(4);
        activityLog.setStyle("-fx-font-size: 12px;");

        VBox panel = new VBox(10, sectionTitle("Actividad reciente"), activityLog);
        panel.setPadding(new Insets(12));
        panel.setStyle(panelStyle());
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

    public void showStorageCleanup(String message) {
        appendPipelineLog(message);
        appendLog(message);
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
        masterBucketsDeleted.setText("0");
        masterBucketsRetained.setText("0");
        storageFreed.setText("0 B");
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
        masterBucketsDeleted.setText("0");
        masterBucketsRetained.setText("0");
        storageFreed.setText("0 B");
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
        masterBucketsDeleted.setText(String.valueOf(result.getMasterBucketsDeleted()));
        masterBucketsRetained.setText(String.valueOf(result.getMasterBucketsRetained()));
        storageFreed.setText(humanBytes(result.getMasterBytesDeleted()));
        distributionResult.setText(result.isSuccess() ? "success" : "failed");
        globalProgress.setProgress(result.getTotalBuckets() == 0 ? 0 : (double) result.getSentBuckets() / result.getTotalBuckets());
        for (BucketDistributionItem item : result.getItems()) {
            upsertDistributionRow(item);
        }
        appendLog("Distribution completed: sent=" + result.getSentBuckets()
                + " failed=" + result.getFailedBuckets()
                + " elapsed=" + result.getElapsedMillis() + " ms.");
        appendLog("Master storage cleanup: deletedBuckets=" + result.getMasterBucketsDeleted()
                + ", retainedBuckets=" + result.getMasterBucketsRetained()
                + ", deletedBytes=" + result.getMasterBytesDeleted()
                + ", failedDeletes=" + result.getFailedDeletes() + ".");
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
        appendLog("Worker response jobId=" + result.getJobId()
                + ", workerId=" + result.getWorkerId()
                + ", message=" + result.getMessage());
        appendLog("Worker counters: " + countersSummary(result.getCounters()));
        if (!result.isSuccess()) {
            appendLog(result.getWorkerLogicalName() + " returned error: " + result.getMessage());
            return;
        }
        if (result.getRouteMonthResults().isEmpty()) {
            appendLog(result.getWorkerLogicalName() + " processed buckets but produced no route-month partial rows.");
        }
        for (RemoteRouteMonthPartial partial : result.getRouteMonthResults()) {
            appendLog("Partial row from " + result.getWorkerLogicalName()
                    + ": route=" + partial.getRouteId()
                    + ", year=" + partial.getYear()
                    + ", month=" + partial.getMonth()
                    + ", validIntervals=" + partial.getValidIntervals()
                    + ", avgKmh=" + format(partial.getAverageKmh()) + ".");
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
        if (summary.getSuccessfulWorkers() > 0 && summary.getTotalPartialResults() > 0) {
            setState("Remote partial results received", "PARTIAL", "#dcfce7", "#166534");
        } else {
            appendLog("No se recibieron resultados parciales validos.");
            setState("Remote processing without valid partial rows", "ERROR", "#fee2e2", "#991b1b");
        }
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
                    String.valueOf(row.getYear()),
                    String.format("%02d", Integer.valueOf(row.getMonth())),
                    format(row.getAverageKmh()) + " km/h",
                    String.valueOf(row.getValidIntervals()),
                    String.valueOf(row.getContributingWorkers()),
                    result.isSuccess() ? "Con datos" : "Sin datos"
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
        masterBucketsDeleted.setText("0");
        masterBucketsRetained.setText("0");
        storageFreed.setText("0 B");
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

    private String humanBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kib = bytes / 1024.0;
        if (kib < 1024.0) {
            return String.format(java.util.Locale.US, "%.1f KiB", Double.valueOf(kib));
        }
        double mib = kib / 1024.0;
        if (mib < 1024.0) {
            return String.format(java.util.Locale.US, "%.1f MiB", Double.valueOf(mib));
        }
        double gib = mib / 1024.0;
        return String.format(java.util.Locale.US, "%.2f GiB", Double.valueOf(gib));
    }

    private String countersSummary(ProcessingCountersDto counters) {
        if (counters == null) {
            return "none";
        }
        return "lines=" + counters.totalLinesRead
                + ", validIntervals=" + counters.validIntervals
                + ", invalidLines=" + counters.invalidLines
                + ", firstRecordsByBus=" + counters.firstRecordsByBus
                + ", routeChanged=" + counters.routeChanged
                + ", nonPositiveDeltaTime=" + counters.nonPositiveDeltaTime
                + ", excessiveDeltaTime=" + counters.excessiveDeltaTime
                + ", nonPositiveDistance=" + counters.nonPositiveDistance
                + ", speedTooHigh=" + counters.speedTooHigh;
    }

    private void updateMapSample(BucketizationResult result) {
        mapSampleKept.setText(String.valueOf(result.getVisualSampleTotalKept()));
        mapSampleSeen.setText(String.valueOf(result.getVisualSampleTotalSeen()));
        mapSampleStep.setText(String.valueOf(result.getVisualSampleStep()));
        mapSampleLimit.setText(String.valueOf(result.getVisualSampleMaxPoints()));
        updateRouteFilterOptions(result.getVisualSamplePoints());
        appendMapSampleAudit(result.getVisualSamplePoints());
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

    private void appendMapSampleAudit(List<master.map.MapPlaybackPoint> points) {
        int total = points == null ? 0 : points.size();
        Set<String> routes = new TreeSet<String>();
        master.map.MapPlaybackPoint first = null;
        if (points != null) {
            for (master.map.MapPlaybackPoint point : points) {
                if (first == null) {
                    first = point;
                }
                String routeId = point.getRouteId();
                if (routeId != null && !routeId.trim().isEmpty()) {
                    routes.add(routeId.trim());
                }
            }
        }
        appendLog("Map sample total points=" + total + "; unique routes=" + routes.size()
                + "; first routes=" + firstRoutes(routes) + ".");
        if (first != null) {
            appendLog("First map point: visualBusKey=" + first.getVisualBusKey()
                    + ", processingBusId=" + first.getBusId()
                    + ", routeId=" + first.getRouteId()
                    + ", lat=" + first.getLatitude()
                    + ", lon=" + first.getLongitude()
                    + ", timestamp=" + first.getTimestamp() + ".");
        }
    }

    private String firstRoutes(Set<String> routes) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String route : routes) {
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(route);
            count++;
            if (count >= 10) {
                break;
            }
        }
        return builder.toString();
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

    private Parent metricRow(String name, Label valueSource) {
        HBox row = new HBox(8);
        Label label = new Label(name);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label metric = new Label();
        metric.textProperty().bind(valueSource.textProperty());
        metric.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #18212f;");
        HBox.setHgrow(label, Priority.ALWAYS);
        row.getChildren().addAll(label, metric);
        return row;
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
