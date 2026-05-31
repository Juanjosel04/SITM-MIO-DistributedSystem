package core.utils;

import java.util.ArrayList;
import java.util.List;

public final class CsvUtils {
    private CsvUtils() {
    }

    public static List<String> split(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                insideQuotes = !insideQuotes;
            } else if (character == ',' && !insideQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString().trim());
        return values;
    }

    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
