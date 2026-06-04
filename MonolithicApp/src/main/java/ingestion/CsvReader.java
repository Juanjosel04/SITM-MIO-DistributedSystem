package ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class CsvReader {
    private static final Charset DATASET_CHARSET = Charset.forName("UTF-8");

    public CsvContent read(String path, boolean hasHeader) throws IOException {
        return read(resolvePath(path), hasHeader);
    }

    public CsvContent read(Path path, boolean hasHeader) throws IOException {
        List<String> header = new ArrayList<String>();
        List<List<String>> rows = new ArrayList<List<String>>();

        BufferedReader reader = Files.newBufferedReader(path, DATASET_CHARSET);
        try {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = split(line);
                if (firstLine && hasHeader) {
                    header = values;
                    firstLine = false;
                    continue;
                }
                rows.add(values);
                firstLine = false;
            }
        } finally {
            reader.close();
        }

        return new CsvContent(header, rows);
    }

    public Path resolvePath(String path) throws IOException {
        Path directPath = Paths.get(path);
        if (Files.exists(directPath)) {
            return directPath;
        }

        Path parentPath = Paths.get("..").resolve(path).normalize();
        if (Files.exists(parentPath)) {
            return parentPath;
        }

        throw new IOException("Dataset file not found: " + path);
    }

    static List<String> split(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                insideQuotes = !insideQuotes;
            } else if (character == ',' && !insideQuotes) {
                values.add(clean(current.toString()));
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(clean(current.toString()));
        return values;
    }

    static String clean(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
