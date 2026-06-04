package ingestion;

import java.util.Collections;
import java.util.List;

public final class CsvContent {
    private final List<String> header;
    private final List<List<String>> rows;

    public CsvContent(List<String> header, List<List<String>> rows) {
        this.header = header == null ? Collections.<String>emptyList() : Collections.unmodifiableList(header);
        this.rows = rows == null ? Collections.<List<String>>emptyList() : Collections.unmodifiableList(rows);
    }

    public List<String> getHeader() {
        return header;
    }

    public List<List<String>> getRows() {
        return rows;
    }
}
