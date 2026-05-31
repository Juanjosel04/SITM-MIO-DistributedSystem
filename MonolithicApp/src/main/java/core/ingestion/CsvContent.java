package core.ingestion;

import java.util.Collections;
import java.util.List;

public class CsvContent {
    private final List<String> header;
    private final List<String> lines;

    public CsvContent(List<String> header, List<String> lines) {
        this.header = header == null ? Collections.<String>emptyList() : header;
        this.lines = lines == null ? Collections.<String>emptyList() : lines;
    }

    public List<String> getHeader() {
        return header;
    }

    public List<String> getLines() {
        return lines;
    }
}
