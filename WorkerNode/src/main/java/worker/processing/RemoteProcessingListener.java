package worker.processing;

public interface RemoteProcessingListener {
    void remoteProcessingStarted(String jobId);

    void remoteProcessingFinished(String jobId, PartialSpeedResult result);
}
