package worker.ice;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import worker.processing.RemoteProcessingListener;
import worker.runtime.WorkerRuntimePaths;
import worker.transfer.BucketTransferListener;

public final class WorkerIceServer implements AutoCloseable {
    public static final String ADAPTER_NAME = "WorkerAdapter";
    public static final String SERVICE_IDENTITY = "WorkerService";

    private final WorkerIceConfig config;
    private final WorkerRuntimePaths runtimePaths;
    private final BucketTransferListener listener;
    private final RemoteProcessingListener processingListener;
    private Communicator communicator;
    private ObjectAdapter adapter;

    public WorkerIceServer(WorkerIceConfig config, BucketTransferListener listener,
                           RemoteProcessingListener processingListener, WorkerRuntimePaths runtimePaths) {
        this.config = config;
        this.runtimePaths = runtimePaths;
        this.listener = listener;
        this.processingListener = processingListener;
    }

    public synchronized void start() {
        if (communicator != null) {
            return;
        }
        runtimePaths.ensureDirectories();
        communicator = Util.initialize(new String[0]);
        adapter = communicator.createObjectAdapterWithEndpoints(ADAPTER_NAME, config.endpoints());
        adapter.add(new WorkerServiceImpl(config, listener, processingListener, runtimePaths),
                Util.stringToIdentity(SERVICE_IDENTITY));
        adapter.activate();
    }

    public WorkerIceConfig getConfig() {
        return config;
    }

    @Override
    public synchronized void close() {
        if (communicator != null) {
            try {
                communicator.destroy();
            } finally {
                communicator = null;
                adapter = null;
            }
        }
    }
}
