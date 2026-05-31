package core.ingestion;

import core.utils.CsvUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {
    private static final Charset DATASET_CHARSET = Charset.forName("UTF-8");

    public CsvContent read(String path, boolean hasHeader) throws IOException {
        List<String> header = new ArrayList<String>();
        List<String> lines = new ArrayList<String>();

        BufferedReader reader = Files.newBufferedReader(resolvePath(path), DATASET_CHARSET);
        try {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (firstLine && hasHeader) {
                    header = CsvUtils.split(line);
                    firstLine = false;
                    continue;
                }
                lines.add(line);
                firstLine = false;
            }
        } finally {
            reader.close();
        }

        return new CsvContent(header, lines);
    }

    private Path resolvePath(String path) throws IOException {
        Path directPath = Paths.get(path);
        if (Files.exists(directPath)) {
            return directPath;
        }

        Path parentPath = Paths.get("..").resolve(path).normalize();
        if (Files.exists(parentPath)) {
            return parentPath;
        }

        throw new IOException(path);
    }
}
