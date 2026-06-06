package master.results;

import sitm.PartialProcessingResult;
import sitm.ProcessingCountersDto;
import sitm.RouteMonthPartial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RemoteWorkerPartialResult {
    private final String workerLogicalName;
    private final String workerId;
    private final String jobId;
    private final boolean success;
    private final String message;
    private final int processedBuckets;
    private final long elapsedMillis;
    private final int resultCount;
    private final long validIntervals;
    private final ProcessingCountersDto counters;
    private final List<RemoteRouteMonthPartial> routeMonthResults;

    private RemoteWorkerPartialResult(String workerLogicalName, String workerId, String jobId, boolean success,
                                      String message, int processedBuckets, long elapsedMillis, int resultCount,
                                      long validIntervals, ProcessingCountersDto counters,
                                      List<RemoteRouteMonthPartial> routeMonthResults) {
        this.workerLogicalName = workerLogicalName;
        this.workerId = workerId;
        this.jobId = jobId;
        this.success = success;
        this.message = message;
        this.processedBuckets = processedBuckets;
        this.elapsedMillis = elapsedMillis;
        this.resultCount = resultCount;
        this.validIntervals = validIntervals;
        this.counters = counters;
        this.routeMonthResults = Collections.unmodifiableList(routeMonthResults);
    }

    public static RemoteWorkerPartialResult fromDto(String workerLogicalName, PartialProcessingResult dto) {
        List<RemoteRouteMonthPartial> rows = new ArrayList<RemoteRouteMonthPartial>();
        if (dto.routeMonthResults != null) {
            for (RouteMonthPartial partial : dto.routeMonthResults) {
                rows.add(new RemoteRouteMonthPartial(dto.workerId, partial.routeId, partial.year, partial.month,
                        partial.totalDistanceMeters, partial.totalTimeSeconds, partial.validIntervals,
                        partial.averageKmh));
            }
        }
        long validIntervals = dto.counters == null ? 0 : dto.counters.validIntervals;
        return new RemoteWorkerPartialResult(workerLogicalName, dto.workerId, dto.jobId, dto.success, dto.message,
                dto.processedBuckets, dto.elapsedMillis, dto.resultCount, validIntervals, dto.counters, rows);
    }

    public static RemoteWorkerPartialResult failure(String workerLogicalName, String workerId, String jobId,
                                                    String message) {
        return new RemoteWorkerPartialResult(workerLogicalName, workerId, jobId, false, message, 0, 0, 0,
                0, null, Collections.<RemoteRouteMonthPartial>emptyList());
    }

    public String getWorkerLogicalName() {
        return workerLogicalName;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getJobId() {
        return jobId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getProcessedBuckets() {
        return processedBuckets;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public int getResultCount() {
        return resultCount;
    }

    public long getValidIntervals() {
        return validIntervals;
    }

    public ProcessingCountersDto getCounters() {
        return counters;
    }

    public List<RemoteRouteMonthPartial> getRouteMonthResults() {
        return routeMonthResults;
    }
}
