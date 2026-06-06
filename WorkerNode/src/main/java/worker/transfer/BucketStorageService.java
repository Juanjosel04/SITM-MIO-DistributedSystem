package worker.transfer;

import sitm.ChunkUploadResponse;
import sitm.TransferFinishResponse;
import sitm.TransferStartResponse;
import worker.runtime.WorkerRuntimePaths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BucketStorageService {
    private final Map<String, BucketTransferSession> sessions = new ConcurrentHashMap<String, BucketTransferSession>();
    private final WorkerRuntimePaths runtimePaths;
    private final BucketTransferListener listener;

    public BucketStorageService(WorkerRuntimePaths runtimePaths, BucketTransferListener listener) {
        this.runtimePaths = runtimePaths;
        this.listener = listener;
    }

    public TransferStartResponse start(String jobId, String bucketId, String fileName, long totalBytes,
                                       int chunkSize, int totalChunks) {
        try {
            if (totalBytes < 0) {
                throw new IllegalArgumentException("totalBytes must be non-negative");
            }
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("chunkSize must be positive");
            }
            if (totalChunks <= 0) {
                throw new IllegalArgumentException("totalChunks must be positive");
            }
            runtimePaths.ensureDirectories();
            String key = key(jobId, bucketId);
            BucketTransferSession previous = sessions.remove(key);
            if (previous != null) {
                previous.close();
            }
            BucketTransferSession session = new BucketTransferSession(
                    requireText(jobId, "jobId"),
                    requireText(bucketId, "bucketId"),
                    sanitizeFileName(fileName),
                    totalBytes,
                    chunkSize,
                    totalChunks,
                    resolveBucketPath(jobId, bucketId, fileName)
            );
            sessions.put(key, session);
            notifyStarted(session.info("Receiving bucket"));
            return new TransferStartResponse(true, "Bucket transfer session started", session.getLocalPath().toString());
        } catch (RuntimeException exception) {
            ReceivedBucketInfo info = new ReceivedBucketInfo(jobId, bucketId, null, totalBytes, 0, totalChunks,
                    0, "Transfer start failed: " + exception.getMessage());
            notifyFailed(info);
            return new TransferStartResponse(false, info.getMessage(), "");
        } catch (IOException exception) {
            ReceivedBucketInfo info = new ReceivedBucketInfo(jobId, bucketId, null, totalBytes, 0, totalChunks,
                    0, "Transfer start failed: " + exception.getMessage());
            notifyFailed(info);
            return new TransferStartResponse(false, info.getMessage(), "");
        }
    }

    public ChunkUploadResponse upload(String jobId, String bucketId, int chunkIndex, byte[] data) {
        BucketTransferSession session = sessions.get(key(jobId, bucketId));
        if (session == null) {
            return new ChunkUploadResponse(false, "Transfer session was not started", 0, 0);
        }
        try {
            session.writeChunk(chunkIndex, data);
            notifyChunk(session.info("Loading chunk " + session.getReceivedChunks() + "/" + session.getTotalChunks()));
            return new ChunkUploadResponse(true, "Chunk received", session.getReceivedChunks(), session.getReceivedBytes());
        } catch (RuntimeException exception) {
            notifyFailed(session.info("Chunk upload failed: " + exception.getMessage()));
            return new ChunkUploadResponse(false, "Chunk upload failed: " + exception.getMessage(),
                    session.getReceivedChunks(), session.getReceivedBytes());
        } catch (IOException exception) {
            notifyFailed(session.info("Chunk upload failed: " + exception.getMessage()));
            return new ChunkUploadResponse(false, "Chunk upload failed: " + exception.getMessage(),
                    session.getReceivedChunks(), session.getReceivedBytes());
        }
    }

    public TransferFinishResponse finish(String jobId, String bucketId) {
        BucketTransferSession session = sessions.remove(key(jobId, bucketId));
        if (session == null) {
            return new TransferFinishResponse(false, "Transfer session was not started", bucketId, "", 0, 0);
        }
        try {
            session.validateComplete();
            session.close();
            ReceivedBucketInfo info = session.info("Bucket received");
            notifyFinished(info);
            return new TransferFinishResponse(true, "Bucket received", bucketId, session.getLocalPath().toString(),
                    session.getReceivedBytes(), session.getReceivedChunks());
        } catch (RuntimeException exception) {
            closeQuietly(session);
            ReceivedBucketInfo info = session.info("Transfer finish failed: " + exception.getMessage());
            notifyFailed(info);
            return new TransferFinishResponse(false, info.getMessage(), bucketId, session.getLocalPath().toString(),
                    session.getReceivedBytes(), session.getReceivedChunks());
        } catch (IOException exception) {
            closeQuietly(session);
            ReceivedBucketInfo info = session.info("Transfer finish failed: " + exception.getMessage());
            notifyFailed(info);
            return new TransferFinishResponse(false, info.getMessage(), bucketId, session.getLocalPath().toString(),
                    session.getReceivedBytes(), session.getReceivedChunks());
        }
    }

    private Path resolveBucketPath(String jobId, String bucketId, String fileName) {
        return runtimePaths.receivedBucketsDirectory()
                .resolve(sanitizePathPart(jobId))
                .resolve(sanitizePathPart(bucketId))
                .resolve(sanitizeFileName(fileName));
    }

    private String key(String jobId, String bucketId) {
        return requireText(jobId, "jobId") + "::" + requireText(bucketId, "bucketId");
    }

    private String sanitizeFileName(String value) {
        String text = requireText(value, "fileName").replace('\\', '_').replace('/', '_');
        return text.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sanitizePathPart(String value) {
        return requireText(value, "pathPart").replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private void closeQuietly(BucketTransferSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
            // The response already reports the transfer failure.
        }
    }

    private void notifyStarted(ReceivedBucketInfo info) {
        if (listener != null) {
            listener.transferStarted(info);
        }
    }

    private void notifyChunk(ReceivedBucketInfo info) {
        if (listener != null) {
            listener.chunkReceived(info);
        }
    }

    private void notifyFinished(ReceivedBucketInfo info) {
        if (listener != null) {
            listener.transferFinished(info);
        }
    }

    private void notifyFailed(ReceivedBucketInfo info) {
        if (listener != null) {
            listener.transferFailed(info);
        }
    }
}
