package master.ice;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import master.transfer.BucketTransferClient;
import master.transfer.BucketTransferRequest;
import master.transfer.BucketTransferResult;
import sitm.PartialProcessingResult;
import sitm.WorkerServicePrx;

import java.util.List;

public final class MasterIceClient implements AutoCloseable {
    private final Communicator communicator;
    private final WorkerRegistry registry;
    private final WorkerHealthScanner scanner;
    private final BucketTransferClient transferClient;

    public MasterIceClient() {
        this.communicator = Util.initialize(new String[0]);
        this.registry = new WorkerRegistry();
        this.scanner = new WorkerHealthScanner(communicator);
        this.transferClient = new BucketTransferClient(communicator);
    }

    public WorkerConnectionResult checkWorkerHealth() {
        return scanWorkers().get(0);
    }

    public List<WorkerConnectionResult> scanWorkers() {
        return scanner.scan(getConfiguredWorkers());
    }

    public List<WorkerEndpointConfig> getConfiguredWorkers() {
        return registry.loadConfiguredWorkers();
    }

    public BucketTransferResult transferBucket(WorkerConnectionResult worker, BucketTransferRequest request,
                                               BucketTransferClient.BucketTransferProgress progress) {
        return transferClient.transfer(worker, request, progress);
    }

    public PartialProcessingResult processReceivedBuckets(WorkerConnectionResult worker, String jobId) {
        ObjectPrx base = communicator.stringToProxy(worker.getProxy()).ice_invocationTimeout(30000);
        WorkerServicePrx service = WorkerServicePrx.checkedCast(base);
        if (service == null) {
            throw new IllegalStateException("Worker proxy is not a WorkerService");
        }
        return service.processReceivedBuckets(jobId);
    }

    @Override
    public void close() {
        communicator.destroy();
    }
}
