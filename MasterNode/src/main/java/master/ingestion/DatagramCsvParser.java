package master.ingestion;

import java.util.ArrayList;
import java.util.List;

public final class DatagramCsvParser {
    private static final int VISUAL_BUS_KEY_INDEX = 2;
    private static final int ODOMETER_INDEX = 3;
    private static final int LATITUDE_INDEX = 4;
    private static final int LONGITUDE_INDEX = 5;
    private static final int ROUTE_ID_INDEX = 7;
    private static final int TIMESTAMP_INDEX = 10;
    private static final int BUS_ID_INDEX = 11;
    private static final int REQUIRED_COLUMNS = 12;

    public DatagramParseResult parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.EMPTY);
        }
        List<String> columns = splitCsvLine(line);
        if (looksLikeHeader(columns)) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.HEADER);
        }
        if (columns.size() < REQUIRED_COLUMNS) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.INVALID_COLUMNS);
        }

        String odometer = clean(columns.get(ODOMETER_INDEX));
        String latitude = clean(columns.get(LATITUDE_INDEX));
        String longitude = clean(columns.get(LONGITUDE_INDEX));
        String routeId = clean(columns.get(ROUTE_ID_INDEX));
        String timestamp = clean(columns.get(TIMESTAMP_INDEX));
        String visualBusKey = clean(columns.get(VISUAL_BUS_KEY_INDEX));
        String busId = clean(columns.get(BUS_ID_INDEX));

        if (busId.isEmpty()) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.MISSING_BUS_ID);
        }
        if (routeId.isEmpty()) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.MISSING_ROUTE_ID);
        }
        if (odometer.isEmpty()) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.MISSING_ODOMETER);
        }
        if (timestamp.isEmpty()) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.MISSING_TIMESTAMP);
        }
        if (latitude.isEmpty() || longitude.isEmpty()) {
            return DatagramParseResult.invalid(DatagramParseResult.Reason.MISSING_COORDINATES);
        }

        return DatagramParseResult.valid(new CompactDatagramRecord(busId, visualBusKey, routeId, odometer, timestamp, latitude, longitude));
    }

    private boolean looksLikeHeader(List<String> columns) {
        if (columns.size() < REQUIRED_COLUMNS) {
            return false;
        }
        return "odometer".equalsIgnoreCase(clean(columns.get(ODOMETER_INDEX)))
                || "latitude".equalsIgnoreCase(clean(columns.get(LATITUDE_INDEX)))
                || "longitude".equalsIgnoreCase(clean(columns.get(LONGITUDE_INDEX)))
                || "routeId".equalsIgnoreCase(clean(columns.get(ROUTE_ID_INDEX)))
                || "lineId".equalsIgnoreCase(clean(columns.get(ROUTE_ID_INDEX)))
                || "datagramDate".equalsIgnoreCase(clean(columns.get(TIMESTAMP_INDEX)))
                || "busCode".equalsIgnoreCase(clean(columns.get(VISUAL_BUS_KEY_INDEX)))
                || "busId".equalsIgnoreCase(clean(columns.get(BUS_ID_INDEX)));
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
}
