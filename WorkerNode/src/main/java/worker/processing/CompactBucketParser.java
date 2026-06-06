package worker.processing;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class CompactBucketParser {
    private static final int REQUIRED_COLUMNS = 6;
    private static final DateTimeFormatter SPACE_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public ParseResult parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return ParseResult.invalid(Reason.INVALID_LINE);
        }
        List<String> columns = splitCsvLine(line);
        if (isHeader(columns)) {
            return ParseResult.header();
        }
        if (columns.size() < REQUIRED_COLUMNS) {
            return ParseResult.invalid(Reason.INVALID_LINE);
        }

        String busId = clean(columns.get(0));
        String routeId = clean(columns.get(1));
        String odometerText = clean(columns.get(2));
        String timestampText = clean(columns.get(3));
        String latitude = clean(columns.get(4));
        String longitude = clean(columns.get(5));

        if (busId.isEmpty()) {
            return ParseResult.invalid(Reason.MISSING_BUS_ID);
        }
        if (routeId.isEmpty()) {
            return ParseResult.invalid(Reason.MISSING_ROUTE_ID);
        }

        double odometer;
        try {
            odometer = Double.parseDouble(odometerText);
        } catch (NumberFormatException exception) {
            return ParseResult.invalid(Reason.INVALID_ODOMETER);
        }

        LocalDateTime timestamp;
        try {
            timestamp = parseTimestamp(timestampText);
        } catch (DateTimeParseException exception) {
            return ParseResult.invalid(Reason.INVALID_TIMESTAMP);
        }

        return ParseResult.valid(new CompactBucketRecord(busId, routeId, odometer, timestamp, latitude, longitude));
    }

    private LocalDateTime parseTimestamp(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new DateTimeParseException("Empty timestamp", text, 0);
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try common SITM timestamp formats below.
        }
        try {
            return LocalDateTime.parse(text, SPACE_SECONDS);
        } catch (DateTimeParseException ignored) {
            // Try milliseconds below.
        }
        try {
            return LocalDateTime.parse(text, SPACE_MILLIS);
        } catch (DateTimeParseException ignored) {
            // Try offset timestamps as a last lightweight option.
        }
        return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
    }

    private boolean isHeader(List<String> columns) {
        return columns.size() >= REQUIRED_COLUMNS
                && "busId".equalsIgnoreCase(clean(columns.get(0)))
                && "routeId".equalsIgnoreCase(clean(columns.get(1)))
                && "odometer".equalsIgnoreCase(clean(columns.get(2)));
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public enum Reason {
        VALID,
        HEADER,
        INVALID_LINE,
        MISSING_BUS_ID,
        MISSING_ROUTE_ID,
        INVALID_TIMESTAMP,
        INVALID_ODOMETER
    }

    public static final class ParseResult {
        private final Reason reason;
        private final CompactBucketRecord record;

        private ParseResult(Reason reason, CompactBucketRecord record) {
            this.reason = reason;
            this.record = record;
        }

        public static ParseResult valid(CompactBucketRecord record) {
            return new ParseResult(Reason.VALID, record);
        }

        public static ParseResult header() {
            return new ParseResult(Reason.HEADER, null);
        }

        public static ParseResult invalid(Reason reason) {
            return new ParseResult(reason, null);
        }

        public boolean isValid() {
            return reason == Reason.VALID;
        }

        public boolean isHeader() {
            return reason == Reason.HEADER;
        }

        public Reason getReason() {
            return reason;
        }

        public CompactBucketRecord getRecord() {
            return record;
        }
    }
}
