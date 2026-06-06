package worker.ice;

import com.zeroc.Ice.Current;
import sitm.ChunkUploadResponse;
import sitm.PartialProcessingResult;
import sitm.TransferFinishResponse;
import sitm.TransferStartResponse;
import sitm.WorkerHealth;
import sitm.WorkerService;
import worker.processing.BucketProcessingService;
import worker.processing.PartialSpeedResult;
import worker.processing.RemoteProcessingListener;
import worker.runtime.WorkerRuntimePaths;
import worker.transfer.BucketStorageService;
import worker.transfer.BucketTransferListener;

public final class WorkerServiceImpl implements WorkerService {
    private final WorkerIceConfig config;
    private final WorkerRuntimePaths runtimePaths;
    private final BucketStorageService storageService;
    private final BucketProcessingService processingService;
    private final ProcessingResultMapper resultMapper = new ProcessingResultMapper();
    private final RemoteProcessingListener processingListener;

    public WorkerServiceImpl(WorkerIceConfig config, BucketTransferListener listener,
                             RemoteProcessingListener processingListener, WorkerRuntimePaths runtimePaths) {
        this.config = config;
        this.runtimePaths = runtimePaths;
        this.storageService = new BucketStorageService(runtimePaths, listener);
        this.processingService = new BucketProcessingService(runtimePaths);
        this.processingListener = processingListener;
    }

    @Override
    public WorkerHealth health(Current current) {
        return new WorkerHealth(
                config.getWorkerId(),
                "UP",
                config.getHost(),
                config.getPort(),
                "Worker service is available; runtime=" + runtimePaths.baseDirectory()
        );
    }

    @Override
    public TransferStartResponse startBucketTransfer(String jobId, String bucketId, String fileName, long totalBytes,
                                                     int chunkSize, int totalChunks, Current current) {
        return storageService.start(jobId, bucketId, fileName, totalBytes, chunkSize, totalChunks);
    }

    @Override
    public ChunkUploadResponse uploadBucketChunk(String jobId, String bucketId, int chunkIndex, byte[] data,
                                                 Current current) {
        return storageService.upload(jobId, bucketId, chunkIndex, data);
    }

    @Override
    public TransferFinishResponse finishBucketTransfer(String jobId, String bucketId, Current current) {
        return storageService.finish(jobId, bucketId);
    }

    @Override
    public PartialProcessingResult processReceivedBuckets(String jobId, Current current) {
        notifyRemoteStarted(jobId);
        PartialSpeedResult result = processingService.processReceivedBuckets(jobId);
        PartialProcessingResult dto = resultMapper.toDto(config.getWorkerId(), jobId, result);
        processingService.cleanupReceivedJobAfterProcessing(jobId, result);
        notifyRemoteFinished(jobId, result);
        return dto;
    }

    private void notifyRemoteStarted(String jobId) {
        if (processingListener != null) {
            processingListener.remoteProcessingStarted(jobId);
        }
    }

    private void notifyRemoteFinished(String jobId, PartialSpeedResult result) {
        if (processingListener != null) {
            processingListener.remoteProcessingFinished(jobId, result);
        }
    }
}
