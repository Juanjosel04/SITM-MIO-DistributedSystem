package master.ice;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import sitm.WorkerHealth;
import sitm.WorkerServicePrx;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class WorkerHealthScanner {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Communicator communicator;

    public WorkerHealthScanner(Communicator communicator) {
        this.communicator = communicator;
    }

    public List<WorkerConnectionResult> scan(List<WorkerEndpointConfig> endpoints) {
        List<WorkerConnectionResult> results = new ArrayList<WorkerConnectionResult>();
        for (WorkerEndpointConfig endpoint : endpoints) {
            results.add(check(endpoint));
        }
        return results;
    }

    private WorkerConnectionResult check(WorkerEndpointConfig endpoint) {
        String checkedAt = LocalTime.now().format(TIME_FORMAT);
        try {
            ObjectPrx base = communicator.stringToProxy(endpoint.getProxyString()).ice_invocationTimeout(2500);
            WorkerServicePrx worker = WorkerServicePrx.checkedCast(base);
            if (worker == null) {
                return WorkerConnectionResult.down(endpoint, "Worker proxy is not a WorkerService", checkedAt);
            }
            WorkerHealth health = worker.health();
            return WorkerConnectionResult.up(
                    endpoint,
                    health.workerId,
                    health.status,
                    health.host,
                    health.port,
                    health.message,
                    checkedAt
            );
        } catch (RuntimeException exception) {
            return WorkerConnectionResult.down(endpoint, "Worker health check failed: " + exception.getMessage(), checkedAt);
        }
    }
}
