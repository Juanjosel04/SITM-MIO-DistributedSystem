package ingestion;

import domain.Route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RouteLoader {
    private static final int FALLBACK_ID_INDEX = 0;
    private static final int FALLBACK_SHORT_NAME_INDEX = 2;
    private static final int FALLBACK_DESCRIPTION_INDEX = 3;

    private final CsvReader csvReader;

    public RouteLoader() {
        this(new CsvReader());
    }

    RouteLoader(CsvReader csvReader) {
        this.csvReader = csvReader;
    }

    public List<Route> loadDefault() throws IOException {
        return load(ConcurrentDatasetPaths.ROUTES_FILE);
    }

    public List<Route> load(String path) throws IOException {
        CsvContent content = csvReader.read(path, true);
        Map<String, Integer> headerIndex = headerIndex(content.getHeader());
        List<Route> routes = new ArrayList<Route>();

        for (List<String> row : content.getRows()) {
            Route route = parseRoute(row, headerIndex);
            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    private Route parseRoute(List<String> row, Map<String, Integer> headerIndex) {
        String idValue = value(row, headerIndex, "LINEID", FALLBACK_ID_INDEX);
        if (idValue.isEmpty()) {
            return null;
        }

        try {
            int id = Integer.parseInt(idValue);
            String shortName = value(row, headerIndex, "SHORTNAME", FALLBACK_SHORT_NAME_INDEX);
            String description = value(row, headerIndex, "DESCRIPTION", FALLBACK_DESCRIPTION_INDEX);
            return new Route(id, shortName, description);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (int i = 0; i < header.size(); i++) {
            String key = CsvReader.clean(header.get(i)).toUpperCase();
            if (!index.containsKey(key)) {
                index.put(key, Integer.valueOf(i));
            }
        }
        return index;
    }

    private String value(List<String> row, Map<String, Integer> headerIndex, String column, int fallbackIndex) {
        Integer resolvedIndex = headerIndex.get(column);
        int index = resolvedIndex == null ? fallbackIndex : resolvedIndex.intValue();
        if (index < 0 || index >= row.size()) {
            return "";
        }
        return CsvReader.clean(row.get(index));
    }
}
