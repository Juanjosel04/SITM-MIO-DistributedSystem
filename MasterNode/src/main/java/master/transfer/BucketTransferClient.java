package master.transfer;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import master.ice.WorkerConnectionResult;
import sitm.ChunkUploadResponse;
import sitm.TransferFinishResponse;
import sitm.TransferStartResponse;
import sitm.WorkerServicePrx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

public final class BucketTransferClient {
    private final Communicator communicator;

    public BucketTransferClient(Communicator communicator) {
        this.communicator = communicator;
    }

    public BucketTransferResult transfer(WorkerConnectionResult worker, BucketTransferRequest request,
                                         BucketTransferProgress progress) {
        if (worker == null || !worker.isUp()) {
            return BucketTransferResult.failure(worker, request, "No active worker available for transfer");
        }
        try {
            WorkerServicePrx service = connect(worker);
            notify(progress, "Starting transfer job " + request.getJobId() + " bucket " + request.getBucketId() + ".", 0, request);
            TransferStartResponse start = service.startBucketTransfer(
                    request.getJobId(),
                    request.getBucketId(),
                    request.getFileName(),
                    request.getTotalBytes(),
                    request.getChunkSizeBytes(),
                    request.getTotalChunks()
            );
            if (!start.success) {
                return BucketTransferResult.failure(worker, request, start.message);
            }
            sendChunks(service, request, progress);
            TransferFinishResponse finish = service.finishBucketTransfer(request.getJobId(), request.getBucketId());
            if (!finish.success) {
                return BucketTransferResult.failure(worker, request, finish.message);
            }
            notify(progress, "Transfer finished for " + request.getBucketId() + ".", request.getTotalChunks(), request);
            return BucketTransferResult.success(worker, request, finish.localPath, finish.message);
        } catch (RuntimeException exception) {
            return BucketTransferResult.failure(worker, request, "Transfer failed: " + exception.getMessage());
        } catch (IOException exception) {
            return BucketTransferResult.failure(worker, request, "Transfer file error: " + exception.getMessage());
        }
    }

    private WorkerServicePrx connect(WorkerConnectionResult worker) {
        ObjectPrx base = communicator.stringToProxy(worker.getProxy()).ice_invocationTimeout(5000);
        WorkerServicePrx service = WorkerServicePrx.checkedCast(base);
        if (service == null) {
            throw new IllegalStateException("Worker proxy is not a WorkerService");
        }
        return service;
    }

    private void sendChunks(WorkerServicePrx service, BucketTransferRequest request,
                            BucketTransferProgress progress) throws IOException {
        byte[] buffer = new byte[request.getChunkSizeBytes()];
        int chunkIndex = 0;
        try (InputStream input = Files.newInputStream(request.getFile())) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                byte[] payload = read == buffer.length ? buffer : Arrays.copyOf(buffer, read);
                notify(progress, "Sending chunk " + (chunkIndex + 1) + "/" + request.getTotalChunks() + ".", chunkIndex, request);
                ChunkUploadResponse upload = service.uploadBucketChunk(
                        request.getJobId(),
                        request.getBucketId(),
                        chunkIndex,
                        payload
                );
                if (!upload.success) {
                    throw new IllegalStateException(upload.message);
                }
                chunkIndex++;
                notify(progress, "Chunk " + chunkIndex + "/" + request.getTotalChunks() + " sent.", chunkIndex, request);
            }
        }
    }

    private void notify(BucketTransferProgress progress, String message, int sentChunks, BucketTransferRequest request) {
        if (progress != null) {
            progress.onProgress(message, sentChunks, request.getTotalChunks(), request.getTotalBytes());
        }
    }

    public interface BucketTransferProgress {
        void onProgress(String message, int sentChunks, int totalChunks, long totalBytes);
    }
}
