module sitm {
    struct WorkerHealth {
        string workerId;
        string status;
        string host;
        int port;
        string message;
    };

    struct TransferStartResponse {
        bool success;
        string message;
        string localPath;
    };

    struct ChunkUploadResponse {
        bool success;
        string message;
        int receivedChunks;
        long receivedBytes;
    };

    struct TransferFinishResponse {
        bool success;
        string message;
        string bucketId;
        string localPath;
        long totalBytes;
        int totalChunks;
    };

    struct RouteMonthPartial {
        string routeId;
        int year;
        int month;
        double totalDistanceMeters;
        double totalTimeSeconds;
        int validIntervals;
        double averageKmh;
    };

    struct ProcessingCountersDto {
        long totalLinesRead;
        long validIntervals;
        long invalidLines;
        long missingBusId;
        long missingRouteId;
        long routeIdMinusOne;
        long invalidTimestamp;
        long invalidOdometer;
        long negativeOdometer;
        long nonPositiveDeltaTime;
        long excessiveDeltaTime;
        long nonPositiveDistance;
        long routeChanged;
        long speedTooHigh;
        long firstRecordsByBus;
    };

    sequence<RouteMonthPartial> RouteMonthPartialSeq;

    struct PartialProcessingResult {
        bool success;
        string workerId;
        string jobId;
        string message;
        long elapsedMillis;
        int processedBuckets;
        int resultCount;
        ProcessingCountersDto counters;
        RouteMonthPartialSeq routeMonthResults;
    };

    sequence<byte> ByteChunk;

    interface WorkerService {
        WorkerHealth health();
        TransferStartResponse startBucketTransfer(string jobId, string bucketId, string fileName, long totalBytes, int chunkSize, int totalChunks);
        ChunkUploadResponse uploadBucketChunk(string jobId, string bucketId, int chunkIndex, ByteChunk data);
        TransferFinishResponse finishBucketTransfer(string jobId, string bucketId);
        PartialProcessingResult processReceivedBuckets(string jobId);
    };
};
