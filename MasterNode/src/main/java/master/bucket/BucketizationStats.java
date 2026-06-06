package master.bucket;

import master.ingestion.DatagramParseResult;

public final class BucketizationStats {
    private long totalLinesRead;
    private long headerSkipped;
    private long validRecordsWritten;
    private long invalidLines;
    private long missingBusId;
    private long missingRouteId;
    private long missingTimestamp;
    private long missingCoordinates;
    private long missingOdometer;

    public void countLine() {
        totalLinesRead++;
    }

    public void countValid() {
        validRecordsWritten++;
    }

    public void countInvalid(DatagramParseResult.Reason reason) {
        if (reason == DatagramParseResult.Reason.EMPTY) {
            return;
        }
        if (reason == DatagramParseResult.Reason.HEADER) {
            headerSkipped++;
            return;
        }
        invalidLines++;
        if (reason == DatagramParseResult.Reason.MISSING_BUS_ID) {
            missingBusId++;
        } else if (reason == DatagramParseResult.Reason.MISSING_ROUTE_ID) {
            missingRouteId++;
        } else if (reason == DatagramParseResult.Reason.MISSING_TIMESTAMP) {
            missingTimestamp++;
        } else if (reason == DatagramParseResult.Reason.MISSING_COORDINATES) {
            missingCoordinates++;
        } else if (reason == DatagramParseResult.Reason.MISSING_ODOMETER) {
            missingOdometer++;
        }
    }

    public long getTotalLinesRead() {
        return totalLinesRead;
    }

    public long getHeaderSkipped() {
        return headerSkipped;
    }

    public long getValidRecordsWritten() {
        return validRecordsWritten;
    }

    public long getInvalidLines() {
        return invalidLines;
    }

    public long getMissingBusId() {
        return missingBusId;
    }

    public long getMissingRouteId() {
        return missingRouteId;
    }

    public long getMissingTimestamp() {
        return missingTimestamp;
    }

    public long getMissingCoordinates() {
        return missingCoordinates;
    }

    public long getMissingOdometer() {
        return missingOdometer;
    }
}
