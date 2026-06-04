package ingestion;

import domain.Datagram;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DatagramLoader {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double COORDINATE_SCALE = 10000000.0;

    private final CsvReader csvReader;

    public DatagramLoader() {
        this(new CsvReader());
    }

    DatagramLoader(CsvReader csvReader) {
        this.csvReader = csvReader;
    }

    public List<Datagram> loadDefault() throws IOException {
        return load(ConcurrentDatasetPaths.defaultDatagramsFile());
    }

    public List<Datagram> load(String path) throws IOException {
        CsvContent content = csvReader.read(path, false);
        List<List<String>> rows = content.getRows();
        if (rows.isEmpty()) {
            return new ArrayList<Datagram>();
        }

        Map<String, Integer> headerIndex = new HashMap<String, Integer>();
        int startIndex = 0;
        if (looksLikeHeader(rows.get(0))) {
            headerIndex = headerIndex(rows.get(0));
            startIndex = 1;
        }

        List<Datagram> datagrams = new ArrayList<Datagram>();
        for (int i = startIndex; i < rows.size(); i++) {
            Datagram datagram = parseDatagram(rows.get(i), headerIndex);
            if (datagram != null) {
                datagrams.add(datagram);
            }
        }
        return datagrams;
    }

    private Datagram parseDatagram(List<String> row, Map<String, Integer> headerIndex) {
        try {
            String busId = value(row, headerIndex, new String[]{"BUSID", "BUS_ID", "BUSCODE", "BUS_CODE"}, 2);
            int routeId = Integer.parseInt(value(row, headerIndex, new String[]{"ROUTEID", "ROUTE_ID", "LINEID", "LINE_ID"}, 7));
            LocalDateTime timestamp = parseTimestamp(value(row, headerIndex, new String[]{"TIMESTAMP", "DATE_TIME", "DATETIME", "EVENT_TIME"}, 10));
            Double speed = parseOptionalDouble(value(row, headerIndex, new String[]{"SPEED", "VELOCITY"}, 6));
            if (speed == null) {
                return null;
            }

            Double latitude = parseOptionalCoordinate(value(row, headerIndex, new String[]{"LATITUDE", "LAT"}, 4));
            Double longitude = parseOptionalCoordinate(value(row, headerIndex, new String[]{"LONGITUDE", "LON", "LNG"}, 5));
            String id = value(row, headerIndex, new String[]{"ID", "DATAGRAM_ID"}, 11);
            return new Datagram(id, busId, routeId, timestamp, speed.doubleValue(), latitude, longitude);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean looksLikeHeader(List<String> row) {
        for (String value : row) {
            String normalized = CsvReader.clean(value).toUpperCase(Locale.ROOT);
            if (normalized.contains("ROUTE") || normalized.contains("LINEID") || normalized.contains("TIMESTAMP")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (int i = 0; i < header.size(); i++) {
            String key = CsvReader.clean(header.get(i)).toUpperCase(Locale.ROOT);
            if (!index.containsKey(key)) {
                index.put(key, Integer.valueOf(i));
            }
        }
        return index;
    }

    private String value(List<String> row, Map<String, Integer> headerIndex, String[] columns, int fallbackIndex) {
        for (String column : columns) {
            Integer resolvedIndex = headerIndex.get(column);
            if (resolvedIndex != null) {
                return valueAt(row, resolvedIndex.intValue());
            }
        }
        return valueAt(row, fallbackIndex);
    }

    private String valueAt(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return "";
        }
        return CsvReader.clean(row.get(index));
    }

    private LocalDateTime parseTimestamp(String value) {
        try {
            return LocalDateTime.parse(value, TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid timestamp: " + value, exception);
        }
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.trim().isEmpty() || "-1".equals(value.trim())) {
            return null;
        }
        return Double.valueOf(value);
    }

    private Double parseOptionalCoordinate(String value) {
        Double coordinate = parseOptionalDouble(value);
        if (coordinate == null) {
            return null;
        }
        if (Math.abs(coordinate.doubleValue()) > 180.0) {
            return Double.valueOf(coordinate.doubleValue() / COORDINATE_SCALE);
        }
        return coordinate;
    }
}
