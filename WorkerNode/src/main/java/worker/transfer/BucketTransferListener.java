package worker.transfer;

public interface BucketTransferListener {
    void transferStarted(ReceivedBucketInfo info);

    void chunkReceived(ReceivedBucketInfo info);

    void transferFinished(ReceivedBucketInfo info);

    void transferFailed(ReceivedBucketInfo info);
}
