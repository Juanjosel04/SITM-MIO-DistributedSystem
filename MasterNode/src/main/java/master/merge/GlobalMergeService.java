package master.merge;

import master.results.RemoteRouteMonthPartial;
import master.results.RemoteWorkerPartialResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GlobalMergeService {
    public GlobalMergeResult merge(String jobId, List<RemoteWorkerPartialResult> partialResults) {
        long started = System.currentTimeMillis();
        GlobalMergeCounters counters = new GlobalMergeCounters();
        if (partialResults == null || partialResults.isEmpty()) {
            return new GlobalMergeResult(false, safeJob(jobId),
                    "No partial results available. Run Process Remote Buckets first.",
                    0, 0, 0, 0, System.currentTimeMillis() - started, counters,
                    Collections.<GlobalMergedResultRow>emptyList());
        }

        Map<GlobalRouteMonthKey, GlobalSpeedAccumulator> merged = new LinkedHashMap<GlobalRouteMonthKey, GlobalSpeedAccumulator>();
        int requested = partialResults.size();
        int mergedWorkers = 0;
        int failedWorkers = 0;
        int partialRows = 0;
        for (RemoteWorkerPartialResult workerResult : partialResults) {
            if (!workerResult.isSuccess()) {
                failedWorkers++;
                continue;
            }
            if (workerResult.getRouteMonthResults().isEmpty() || workerResult.getResultCount() <= 0) {
                continue;
            }
            mergedWorkers++;
            partialRows += workerResult.getResultCount();
            counters.mergeFrom(workerResult);
            for (RemoteRouteMonthPartial partial : workerResult.getRouteMonthResults()) {
                if (partial.getRouteId() == null || partial.getRouteId().trim().isEmpty()) {
                    continue;
                }
                GlobalRouteMonthKey key = new GlobalRouteMonthKey(partial.getRouteId(), partial.getYear(), partial.getMonth());
                GlobalSpeedAccumulator accumulator = merged.get(key);
                if (accumulator == null) {
                    accumulator = new GlobalSpeedAccumulator();
                    merged.put(key, accumulator);
                }
                accumulator.mergeFrom(partial);
            }
        }

        List<GlobalMergedResultRow> rows = new ArrayList<GlobalMergedResultRow>();
        for (Map.Entry<GlobalRouteMonthKey, GlobalSpeedAccumulator> entry : merged.entrySet()) {
            rows.add(new GlobalMergedResultRow(entry.getKey(), entry.getValue()));
        }
        Collections.sort(rows, new Comparator<GlobalMergedResultRow>() {
            @Override
            public int compare(GlobalMergedResultRow left, GlobalMergedResultRow right) {
                int route = left.getRouteId().compareTo(right.getRouteId());
                if (route != 0) {
                    return route;
                }
                int year = Integer.compare(left.getYear(), right.getYear());
                if (year != 0) {
                    return year;
                }
                return Integer.compare(left.getMonth(), right.getMonth());
            }
        });

        boolean success = mergedWorkers > 0 && !rows.isEmpty();
        String message;
        if (success && failedWorkers > 0) {
            message = "Global merge completed with failed worker partials ignored.";
        } else if (success) {
            message = "Global merge completed.";
        } else {
            message = "No se recibieron resultados parciales validos.";
        }
        return new GlobalMergeResult(success, safeJob(jobId), message, requested, mergedWorkers, failedWorkers,
                partialRows, System.currentTimeMillis() - started, counters, rows);
    }

    private String safeJob(String jobId) {
        return jobId == null || jobId.trim().isEmpty() ? "unknown" : jobId.trim();
    }
}
