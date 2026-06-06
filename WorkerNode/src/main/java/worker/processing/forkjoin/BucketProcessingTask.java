package worker.processing.forkjoin;

import worker.processing.CompactBucketParser;
import worker.processing.CompactBucketRecord;
import worker.processing.PartialSpeedResult;
import worker.processing.ProcessingCounters;
import worker.processing.RouteMonthKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

public final class BucketProcessingTask extends RecursiveTask<PartialSpeedResult> {
    private static final int THRESHOLD = 2;
    private static final int MAX_DELTA_SECONDS = 600;
    private static final double MAX_SPEED_KMH = 120.0;

    private final List<Path> bucketFiles;

    public BucketProcessingTask(List<Path> bucketFiles) {
        this.bucketFiles = bucketFiles;
    }

    @Override
    protected PartialSpeedResult compute() {
        if (bucketFiles.size() <= THRESHOLD) {
            return processDirectly();
        }
        int middle = bucketFiles.size() / 2;
        BucketProcessingTask left = new BucketProcessingTask(bucketFiles.subList(0, middle));
        BucketProcessingTask right = new BucketProcessingTask(bucketFiles.subList(middle, bucketFiles.size()));
        left.fork();
        PartialSpeedResult rightResult = right.compute();
        PartialSpeedResult leftResult = left.join();
        leftResult.merge(rightResult);
        return leftResult;
    }

    private PartialSpeedResult processDirectly() {
        PartialSpeedResult result = new PartialSpeedResult();
        for (Path bucketFile : bucketFiles) {
            processBucket(bucketFile, result);
            result.addProcessedBucket(bucketFile);
        }
        return result;
    }

    private void processBucket(Path bucketFile, PartialSpeedResult result) {
        CompactBucketParser parser = new CompactBucketParser();
        Map<String, CompactBucketRecord> lastRecordByBusId = new HashMap<String, CompactBucketRecord>();
        try (BufferedReader reader = Files.newBufferedReader(bucketFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ProcessingCounters counters = result.getCounters();
                counters.countLine();
                CompactBucketParser.ParseResult parseResult = parser.parse(line);
                if (parseResult.isHeader()) {
                    continue;
                }
                if (!parseResult.isValid()) {
                    countParseDiscard(counters, parseResult.getReason());
                    continue;
                }
                processRecord(parseResult.getRecord(), lastRecordByBusId, result);
            }
        } catch (IOException exception) {
            PartialSpeedResult failure = PartialSpeedResult.failure("Could not process bucket "
                    + bucketFile + ": " + exception.getMessage());
            result.merge(failure);
        }
    }

    private void processRecord(CompactBucketRecord current, Map<String, CompactBucketRecord> lastRecordByBusId,
                               PartialSpeedResult result) {
        ProcessingCounters counters = result.getCounters();
        if ("-1".equals(current.getRouteId())) {
            counters.countRouteIdMinusOne();
            return;
        }
        if (current.getOdometer() < 0) {
            counters.countNegativeOdometer();
            return;
        }

        CompactBucketRecord previous = lastRecordByBusId.get(current.getBusId());
        if (previous == null) {
            counters.countFirstRecordByBus();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }
        if (previous.getOdometer() < 0) {
            counters.countNegativeOdometer();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }
        if (!previous.getRouteId().equals(current.getRouteId())) {
            counters.countRouteChanged();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }

        long deltaTimeSeconds = Duration.between(previous.getTimestamp(), current.getTimestamp()).getSeconds();
        if (deltaTimeSeconds <= 0) {
            counters.countNonPositiveDeltaTime();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }
        if (deltaTimeSeconds > MAX_DELTA_SECONDS) {
            counters.countExcessiveDeltaTime();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }

        double deltaDistanceMeters = current.getOdometer() - previous.getOdometer();
        if (deltaDistanceMeters <= 0) {
            counters.countNonPositiveDistance();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }

        double speedKmh = deltaDistanceMeters / deltaTimeSeconds * 3.6;
        if (speedKmh > MAX_SPEED_KMH) {
            counters.countSpeedTooHigh();
            lastRecordByBusId.put(current.getBusId(), current);
            return;
        }

        RouteMonthKey key = RouteMonthKey.from(current.getRouteId(), current.getTimestamp());
        result.addInterval(key, deltaDistanceMeters, deltaTimeSeconds);
        lastRecordByBusId.put(current.getBusId(), current);
    }

    private void countParseDiscard(ProcessingCounters counters, CompactBucketParser.Reason reason) {
        if (reason == CompactBucketParser.Reason.MISSING_BUS_ID) {
            counters.countMissingBusId();
        } else if (reason == CompactBucketParser.Reason.MISSING_ROUTE_ID) {
            counters.countMissingRouteId();
        } else if (reason == CompactBucketParser.Reason.INVALID_TIMESTAMP) {
            counters.countInvalidTimestamp();
        } else if (reason == CompactBucketParser.Reason.INVALID_ODOMETER) {
            counters.countInvalidOdometer();
        } else {
            counters.countInvalidLines();
        }
    }
}
