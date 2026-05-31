package core.processing.parser;

import core.model.Datagram;
import core.utils.CsvUtils;
import shared.constants.ProcessingConstants;
import shared.exceptions.CsvParsingException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DatagramParser {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double COORDINATE_SCALE = 10000000.0;

    public Datagram parse(String line) throws CsvParsingException {
        List<String> values = CsvUtils.split(line);
        if (values.size() < ProcessingConstants.EXPECTED_DATAGRAM_COLUMNS) {
            throw new CsvParsingException("Datagram line has fewer columns than expected");
        }

        try {
            String busCode = value(values, 2);
            double latitude = parseCoordinate(value(values, 4));
            double longitude = parseCoordinate(value(values, 5));
            Double speed = parseOptionalDouble(value(values, 6));
            int routeId = Integer.parseInt(value(values, 7));
            String eventCode = value(values, 8);
            LocalDateTime timestamp = parseTimestamp(value(values, 10));
            String id = value(values, 11);
            return new Datagram(id, busCode, routeId, latitude, longitude, speed, eventCode, timestamp, line);
        } catch (RuntimeException exception) {
            throw new CsvParsingException("Invalid datagram line", exception);
        }
    }

    private String value(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return CsvUtils.clean(values.get(index));
    }

    private double parseCoordinate(String value) {
        double coordinate = Double.parseDouble(value);
        if (Math.abs(coordinate) > 180.0) {
            return coordinate / COORDINATE_SCALE;
        }
        return coordinate;
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.trim().isEmpty() || "-1".equals(value.trim())) {
            return null;
        }
        return Double.valueOf(value);
    }

    private LocalDateTime parseTimestamp(String value) {
        try {
            return LocalDateTime.parse(value, TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException exception) {
            return LocalDateTime.now();
        }
    }
}
