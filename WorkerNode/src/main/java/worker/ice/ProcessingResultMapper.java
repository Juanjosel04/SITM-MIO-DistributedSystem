package worker.ice;

import sitm.PartialProcessingResult;
import sitm.ProcessingCountersDto;
import sitm.RouteMonthPartial;
import worker.processing.PartialSpeedResult;
import worker.processing.ProcessingCounters;
import worker.processing.RouteMonthKey;
import worker.processing.SpeedAccumulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProcessingResultMapper {
    public PartialProcessingResult toDto(String workerId, String jobId, PartialSpeedResult result) {
        ProcessingCountersDto counters = toCountersDto(result.getCounters());
        RouteMonthPartial[] partials = toPartials(result);
        return new PartialProcessingResult(
                result.isSuccess(),
                workerId,
                jobId,
                result.getMessage(),
                result.getElapsedMillis(),
                result.getProcessedBuckets().size(),
                partials.length,
                counters,
                partials
        );
    }

    private ProcessingCountersDto toCountersDto(ProcessingCounters counters) {
        return new ProcessingCountersDto(
                counters.getTotalLinesRead(),
                counters.getValidIntervals(),
                counters.getInvalidLines(),
                counters.getMissingBusId(),
                counters.getMissingRouteId(),
                counters.getRouteIdMinusOne(),
                counters.getInvalidTimestamp(),
                counters.getInvalidOdometer(),
                counters.getNegativeOdometer(),
                counters.getNonPositiveDeltaTime(),
                counters.getExcessiveDeltaTime(),
                counters.getNonPositiveDistance(),
                counters.getRouteChanged(),
                counters.getSpeedTooHigh(),
                counters.getFirstRecordsByBus()
        );
    }

    private RouteMonthPartial[] toPartials(PartialSpeedResult result) {
        List<RouteMonthPartial> partials = new ArrayList<RouteMonthPartial>();
        for (Map.Entry<RouteMonthKey, SpeedAccumulator> entry : result.getAccumulators().entrySet()) {
            RouteMonthKey key = entry.getKey();
            SpeedAccumulator accumulator = entry.getValue();
            partials.add(new RouteMonthPartial(
                    key.getRouteId(),
                    key.getYearMonth().getYear(),
                    key.getYearMonth().getMonthValue(),
                    accumulator.getTotalDistanceMeters(),
                    accumulator.getTotalTimeSeconds(),
                    (int) accumulator.getValidIntervals(),
                    accumulator.averageKmh()
            ));
        }
        return partials.toArray(new RouteMonthPartial[0]);
    }
}
