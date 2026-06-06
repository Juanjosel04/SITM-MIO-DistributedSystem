package master.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import master.bucket.BucketizationConfig;
import master.bucket.BucketizationResult;
import master.bucket.DatagramBucketizer;
import master.distribution.BucketDistributionService;
import master.distribution.DistributionResult;
import master.distribution.GeneratedBucketLocator;
import master.ice.MasterIceClient;
import master.ice.WorkerConnectionResult;
import master.merge.GlobalMergeResult;
import master.merge.GlobalMergeService;
import master.results.RemoteProcessingService;
import master.results.RemoteProcessingSummary;
import master.results.RemoteWorkerPartialResult;
import master.transfer.BucketTransferRequest;
import master.transfer.BucketTransferResult;
import master.transfer.TestBucketFileFactory;
import master.ui.MasterNodeView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MasterNodeApplication extends Application {
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 800;
    private MasterIceClient iceClient;
    private final DatagramBucketizer bucketizer = new DatagramBucketizer();
    private final GeneratedBucketLocator bucketLocator = new GeneratedBucketLocator();
    private List<WorkerConnectionResult> lastScanResults = Collections.emptyList();
    private BucketizationResult lastBucketizationResult;
    private DistributionResult lastDistributionResult;
    private RemoteProcessingSummary lastRemoteProcessingSummary;
    private List<RemoteWorkerPartialResult> lastPartialResults = Collections.emptyList();
    private BucketizationResult activePipelineBucketizationResult;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        MasterNodeView view = new MasterNodeView();
        iceClient = new MasterIceClient();
        view.setRunFullPipelineHandler(() -> runFullDistributedPipeline(view));
        view.setDetectWorkersHandler(() -> detectWorkers(view));
        view.setTransferBucketHandler(() -> transferTestBucket(view));
        view.setGenerateBucketsHandler(() -> generateBuckets(view));
        view.setDistributeBucketsHandler(() -> distributeBuckets(view));
        view.setProcessRemoteBucketsHandler(() -> processRemoteBuckets(view));
        view.setMergeGlobalResultsHandler(() -> mergeGlobalResults(view));
        Scene scene = new Scene(view.createContent(), INITIAL_WIDTH, INITIAL_HEIGHT);

        primaryStage.setTitle("Master Node - Reconstruccion Distribuida SITM-MIO");
        primaryStage.setWidth(INITIAL_WIDTH);
        primaryStage.setHeight(INITIAL_HEIGHT);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> closeIceClient());
        primaryStage.show();
    }

    @Override
    public void stop() {
        closeIceClient();
    }

    private void detectWorkers(MasterNodeView view) {
        view.showHealthScanStarted(iceClient.getConfiguredWorkers());
        CompletableFuture
                .supplyAsync(() -> iceClient.scanWorkers())
                .thenAccept(results -> Platform.runLater(() -> showHealthResults(view, results)));
    }

    private void runFullDistributedPipeline(MasterNodeView view) {
        view.showPipelineStarted();
        CompletableFuture
                .runAsync(() -> runFullDistributedPipelineInBackground(view))
                .thenRun(() -> Platform.runLater(view::showPipelineFinished))
                .exceptionally(exception -> {
                    cleanupFailedPipelineJobIfConfigured(view);
                    Platform.runLater(() -> view.showPipelineFailed(cleanMessage(exception)));
                    return null;
                });
    }

    private void runFullDistributedPipelineInBackground(MasterNodeView view) {
        activePipelineBucketizationResult = null;
        Platform.runLater(() -> view.showPipelineStep(1, "Detectando workers disponibles..."));
        List<WorkerConnectionResult> scannedWorkers = iceClient.scanWorkers();
        lastScanResults = scannedWorkers;
        Platform.runLater(() -> showHealthResults(view, scannedWorkers));
        List<WorkerConnectionResult> active = activeWorkers(scannedWorkers);
        if (active.isEmpty()) {
            throw new IllegalStateException("No hay workers activos disponibles. Proceso detenido.");
        }
        Platform.runLater(() -> view.showPipelineStepFinished(1, active.size() + " worker(s) activos disponibles."));

        Platform.runLater(() -> view.showPipelineStep(2, "Generando buckets desde datagramas..."));
        BucketizationResult bucketizationResult = runBucketization(view);
        activePipelineBucketizationResult = bucketizationResult;
        lastBucketizationResult = bucketizationResult;
        Platform.runLater(() -> view.showBucketizationFinished(bucketizationResult));
        if (!bucketizationResult.isSuccess() || bucketizationResult.getBuckets().isEmpty()) {
            throw new IllegalStateException("No se generaron buckets. Proceso detenido.");
        }
        Platform.runLater(() -> view.showPipelineStepFinished(2,
                bucketizationResult.getBuckets().size() + " buckets generados; "
                        + bucketizationResult.getValidRecordsWritten() + " registros validos; "
                        + bucketizationResult.getInvalidLines() + " registros invalidos; "
                        + bucketizationResult.getVisualSampleTotalKept() + " puntos visuales capturados."));

        Platform.runLater(() -> view.showPipelineStep(3, "Distribuyendo buckets hacia workers activos..."));
        DistributionResult distributionResult = runDistribution(view);
        lastDistributionResult = distributionResult;
        Platform.runLater(() -> view.showDistributionFinished(distributionResult));
        if (distributionResult.getSentBuckets() == 0) {
            throw new IllegalStateException("No se pudieron distribuir buckets. Proceso detenido.");
        }
        Platform.runLater(() -> view.showPipelineStepFinished(3,
                distributionResult.getSentBuckets() + " buckets enviados; "
                        + distributionResult.getFailedBuckets() + " buckets fallidos."));

        Platform.runLater(() -> view.showPipelineStep(4, "Solicitando procesamiento remoto en workers..."));
        RemoteProcessingSummary remoteSummary = runRemoteProcessing(view);
        lastRemoteProcessingSummary = remoteSummary;
        lastPartialResults = remoteSummary.getWorkerResults();
        Platform.runLater(() -> view.showRemoteProcessingFinished(remoteSummary));
        if (remoteSummary.getSuccessfulWorkers() == 0) {
            throw new IllegalStateException("No se recibio procesamiento remoto exitoso. Proceso detenido.");
        }
        if (remoteSummary.getTotalPartialResults() == 0) {
            throw new IllegalStateException("No se recibieron resultados parciales validos. Proceso detenido.");
        }
        Platform.runLater(() -> view.showPipelineStepFinished(4,
                remoteSummary.getSuccessfulWorkers() + " workers procesados; "
                        + remoteSummary.getFailedWorkers() + " workers fallidos; "
                        + remoteSummary.getTotalPartialResults() + " resultados parciales."));

        Platform.runLater(() -> view.showPipelineStep(5, "Fusionando resultados globales..."));
        GlobalMergeResult globalMergeResult = runGlobalMerge(view);
        Platform.runLater(() -> view.showGlobalMergeFinished(globalMergeResult));
        Platform.runLater(() -> view.showPipelineStepFinished(5,
                globalMergeResult.getTotalGlobalRows() + " filas globales; "
                        + globalMergeResult.getTotalWorkersMerged() + " workers fusionados; "
                        + globalMergeResult.getCounters().getValidIntervals() + " intervalos validos globales."));

        Platform.runLater(() -> view.showPipelineStep(6, "Cargando mapa visual de rutas..."));
        if (bucketizationResult.getVisualSamplePoints().isEmpty()) {
            Platform.runLater(() -> view.showPipelineWarning(
                    "Resultados procesados, pero no hay muestra visual disponible para mapa."));
        } else {
            Platform.runLater(view::playMapFromPipeline);
        }
        Platform.runLater(() -> view.showPipelineStepFinished(6,
                "Mapa listo con " + bucketizationResult.getVisualSampleTotalKept() + " puntos visuales cargados."));
        cleanupMasterJobAfterSuccessfulPipeline(bucketizationResult, view);
        activePipelineBucketizationResult = null;
    }

    private void showHealthResults(MasterNodeView view, List<WorkerConnectionResult> results) {
        lastScanResults = results;
        view.showWorkerScanResults(results);
    }

    private void transferTestBucket(MasterNodeView view) {
        view.showTransferRequested();
        CompletableFuture
                .supplyAsync(() -> runTransfer(view))
                .thenAccept(result -> Platform.runLater(() -> view.showTransferFinished(result)));
    }

    private void generateBuckets(MasterNodeView view) {
        CompletableFuture
                .supplyAsync(() -> runBucketization(view))
                .thenAccept(result -> Platform.runLater(() -> {
                    lastBucketizationResult = result;
                    view.showBucketizationFinished(result);
                }));
    }

    private BucketizationResult runBucketization(MasterNodeView view) {
        try {
            BucketizationConfig config = BucketizationConfig.fromSystemProperties();
            Platform.runLater(() -> view.showBucketizationStarted(config));
            return bucketizer.bucketize(config, (message, totalLinesRead, validRecordsWritten) ->
                    Platform.runLater(() -> view.showBucketizationProgress(message, totalLinesRead, validRecordsWritten)));
        } catch (IOException exception) {
            Platform.runLater(() -> view.showBucketizationFailed(exception.getMessage()));
            throw new IllegalStateException(exception);
        }
    }

    private BucketTransferResult runTransfer(MasterNodeView view) {
        List<WorkerConnectionResult> results = lastScanResults;
        if (results.isEmpty() || firstUp(results) == null) {
            results = iceClient.scanWorkers();
            List<WorkerConnectionResult> finalResults = results;
            Platform.runLater(() -> showHealthResults(view, finalResults));
        }

        WorkerConnectionResult worker = firstUp(results);
        if (worker == null) {
            Platform.runLater(view::showNoActiveWorkerForTransfer);
            return BucketTransferResult.failure(null, null, "No active worker available for transfer");
        }

        try {
            BucketTransferRequest request = new TestBucketFileFactory().createDemoBucket();
            Platform.runLater(() -> view.showTransferStarted(worker, request.getJobId(), request.getBucketId(),
                    request.getTotalBytes(), request.getTotalChunks()));
            return iceClient.transferBucket(worker, request, (message, sentChunks, totalChunks, totalBytes) ->
                    Platform.runLater(() -> view.showTransferProgress(message, sentChunks, totalChunks, totalBytes)));
        } catch (IOException exception) {
            return BucketTransferResult.failure(worker, null, "Could not create test bucket: " + exception.getMessage());
        }
    }

    private void distributeBuckets(MasterNodeView view) {
        CompletableFuture
                .supplyAsync(() -> runDistribution(view))
                .thenAccept(result -> Platform.runLater(() -> {
                    lastDistributionResult = result;
                    view.showDistributionFinished(result);
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> view.showDistributionFailed(exception.getMessage()));
                    return null;
                });
    }

    private DistributionResult runDistribution(MasterNodeView view) {
        try {
            List<Path> buckets = bucketLocator.locate(lastBucketizationResult);
            List<WorkerConnectionResult> workers = activeWorkers();
            if (workers.isEmpty()) {
                List<WorkerConnectionResult> scanned = iceClient.scanWorkers();
                Platform.runLater(() -> showHealthResults(view, scanned));
                workers = activeWorkers(scanned);
            }
            BucketDistributionService service = new BucketDistributionService(iceClient);
            List<WorkerConnectionResult> finalWorkers = workers;
            return service.distribute(buckets, workers, new BucketDistributionService.DistributionProgress() {
                @Override
                public void onStarted(String jobId, List<master.distribution.BucketDistributionItem> items,
                                      List<WorkerConnectionResult> activeWorkers) {
                    Platform.runLater(() -> view.showDistributionStarted(jobId, items, finalWorkers));
                }

                @Override
                public void onItemUpdated(master.distribution.BucketDistributionItem item) {
                    Platform.runLater(() -> view.showDistributionItemUpdated(item));
                }

                @Override
                public void onTransferProgress(master.distribution.BucketDistributionItem item,
                                               WorkerConnectionResult worker, String message, int sentChunks,
                                               int totalChunks, long totalBytes) {
                    Platform.runLater(() -> view.showDistributionTransferProgress(item, worker, message,
                            sentChunks, totalChunks, totalBytes));
                }

                @Override
                public void onFinished(DistributionResult result) {
                    // Final UI update is handled by the CompletableFuture continuation.
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not locate generated buckets: " + exception.getMessage(), exception);
        }
    }

    private List<WorkerConnectionResult> activeWorkers() {
        return activeWorkers(lastScanResults);
    }

    private List<WorkerConnectionResult> activeWorkers(List<WorkerConnectionResult> results) {
        List<WorkerConnectionResult> active = new ArrayList<WorkerConnectionResult>();
        for (WorkerConnectionResult result : results) {
            if (result.isUp()) {
                active.add(result);
            }
        }
        return active;
    }

    private void processRemoteBuckets(MasterNodeView view) {
        CompletableFuture
                .supplyAsync(() -> runRemoteProcessing(view))
                .thenAccept(summary -> Platform.runLater(() -> {
                    lastRemoteProcessingSummary = summary;
                    lastPartialResults = summary.getWorkerResults();
                    view.showRemoteProcessingFinished(summary);
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> view.showRemoteProcessingFailed(exception.getMessage()));
                    return null;
                });
    }

    private void mergeGlobalResults(MasterNodeView view) {
        CompletableFuture
                .supplyAsync(() -> runGlobalMerge(view))
                .thenAccept(result -> Platform.runLater(() -> view.showGlobalMergeFinished(result)))
                .exceptionally(exception -> {
                    Platform.runLater(() -> view.showGlobalMergeFailed(exception.getMessage()));
                    return null;
                });
    }

    private GlobalMergeResult runGlobalMerge(MasterNodeView view) {
        if (lastPartialResults == null || lastPartialResults.isEmpty()) {
            throw new IllegalStateException("No partial results available. Run Process Remote Buckets first.");
        }
        String jobId = lastRemoteProcessingSummary == null ? "unknown" : lastRemoteProcessingSummary.getJobId();
        Platform.runLater(() -> view.showGlobalMergeStarted(jobId, lastPartialResults.size()));
        return new GlobalMergeService().merge(jobId, lastPartialResults);
    }

    private RemoteProcessingSummary runRemoteProcessing(MasterNodeView view) {
        if (lastDistributionResult == null) {
            throw new IllegalStateException("No distribution job available. Run Distribute Buckets first.");
        }
        List<WorkerConnectionResult> workers = activeWorkers();
        if (workers.isEmpty()) {
            List<WorkerConnectionResult> scanned = iceClient.scanWorkers();
            Platform.runLater(() -> showHealthResults(view, scanned));
            workers = activeWorkers(scanned);
        }
        if (workers.isEmpty()) {
            throw new IllegalStateException("No active workers available for remote processing.");
        }
        return new RemoteProcessingService(iceClient).requestPartialResults(lastDistributionResult, workers,
                new RemoteProcessingService.RemoteProcessingProgress() {
                    @Override
                    public void onStarted(String jobId, List<WorkerConnectionResult> workers) {
                        Platform.runLater(() -> view.showRemoteProcessingStarted(jobId, workers));
                    }

                    @Override
                    public void onWorkerRequested(WorkerConnectionResult worker, String message) {
                        Platform.runLater(() -> view.showRemoteWorkerRequested(worker, message));
                    }

                    @Override
                    public void onWorkerResult(RemoteWorkerPartialResult result) {
                        Platform.runLater(() -> view.showRemoteWorkerResult(result));
                    }
                });
    }

    private WorkerConnectionResult firstUp(List<WorkerConnectionResult> results) {
        for (WorkerConnectionResult result : results) {
            if (result.isUp()) {
                return result;
            }
        }
        return null;
    }

    private String cleanMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.trim().isEmpty()
                ? "El pipeline distribuido fallo. Proceso detenido."
                : message;
    }

    private void cleanupMasterJobAfterSuccessfulPipeline(BucketizationResult result, MasterNodeView view) {
        if (result == null || !result.isSuccess()) {
            return;
        }
        if (!cleanupJobAfterPipeline()) {
            Platform.runLater(() -> view.showStorageCleanup(
                    "Master bucketization job retained by sitm.master.cleanup.job.after.pipeline=false."));
            return;
        }
        CleanupStats stats = deleteRecursively(result.getOutputDirectory());
        Platform.runLater(() -> view.showStorageCleanup("Master bucketization job cleanup: deletedEntries="
                + stats.deletedEntries + ", deletedBytes=" + stats.deletedBytes
                + ", failedDeletes=" + stats.failedDeletes + "."));
    }

    private void cleanupFailedPipelineJobIfConfigured(MasterNodeView view) {
        BucketizationResult result = activePipelineBucketizationResult;
        if (result == null || keepFailedJob()) {
            return;
        }
        CleanupStats stats = deleteRecursively(result.getOutputDirectory());
        Platform.runLater(() -> view.showStorageCleanup("Failed Master bucketization job cleanup: deletedEntries="
                + stats.deletedEntries + ", deletedBytes=" + stats.deletedBytes
                + ", failedDeletes=" + stats.failedDeletes + "."));
        activePipelineBucketizationResult = null;
    }

    private boolean cleanupJobAfterPipeline() {
        return Boolean.parseBoolean(System.getProperty("sitm.master.cleanup.job.after.pipeline", "true"));
    }

    private boolean keepFailedJob() {
        return Boolean.parseBoolean(System.getProperty("sitm.master.keep.failed.job", "true"));
    }

    private CleanupStats deleteRecursively(Path directory) {
        CleanupStats stats = new CleanupStats();
        if (directory == null || !Files.exists(directory)) {
            return stats;
        }
        java.util.stream.Stream<Path> stream = null;
        try {
            stream = Files.walk(directory);
            List<Path> paths = new ArrayList<Path>();
            stream.forEach(paths::add);
            paths.sort(new Comparator<Path>() {
                @Override
                public int compare(Path left, Path right) {
                    return right.getNameCount() - left.getNameCount();
                }
            });
            for (Path path : paths) {
                try {
                    if (Files.isRegularFile(path)) {
                        stats.deletedBytes += Files.size(path);
                    }
                    if (Files.deleteIfExists(path)) {
                        stats.deletedEntries++;
                    }
                } catch (IOException exception) {
                    stats.failedDeletes++;
                }
            }
        } catch (IOException exception) {
            stats.failedDeletes++;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        return stats;
    }

    private static final class CleanupStats {
        private int deletedEntries;
        private long deletedBytes;
        private int failedDeletes;
    }

    private void closeIceClient() {
        if (iceClient != null) {
            iceClient.close();
            iceClient = null;
        }
    }
}
