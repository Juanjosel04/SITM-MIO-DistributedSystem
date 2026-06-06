package master.ingestion;

public final class DatagramParseResult {
    public enum Reason {
        VALID,
        EMPTY,
        HEADER,
        INVALID_COLUMNS,
        MISSING_BUS_ID,
        MISSING_ROUTE_ID,
        MISSING_ODOMETER,
        MISSING_TIMESTAMP,
        MISSING_COORDINATES
    }

    private final Reason reason;
    private final CompactDatagramRecord record;

    private DatagramParseResult(Reason reason, CompactDatagramRecord record) {
        this.reason = reason;
        this.record = record;
    }

    public static DatagramParseResult valid(CompactDatagramRecord record) {
        return new DatagramParseResult(Reason.VALID, record);
    }

    public static DatagramParseResult invalid(Reason reason) {
        return new DatagramParseResult(reason, null);
    }

    public Reason getReason() {
        return reason;
    }

    public CompactDatagramRecord getRecord() {
        return record;
    }

    public boolean isValid() {
        return reason == Reason.VALID;
    }
}
