package core.speed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RouteCatalog {

    private final Map<Integer, String> routes; // lineId -> shortName

    private RouteCatalog(Map<Integer, String> routes) {
        this.routes = routes;
    }

    /**
     * Loads active routes from lines-241-ActiveGT.csv.
     * Expects a header row; data rows: LINEID at col 0, SHORTNAME at col 2.
     * Uses indexOf to safely skip past col 1 even if DESCRIPTION (col 3+) contains commas.
     */
    public static RouteCatalog load(String path) throws IOException {
        Map<Integer, String> routes = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                int c1 = line.indexOf(',');
                if (c1 < 0) continue;
                int c2 = line.indexOf(',', c1 + 1);
                if (c2 < 0) continue;
                int c3 = line.indexOf(',', c2 + 1);
                String idStr    = line.substring(0, c1).trim();
                String shortName = (c3 < 0 ? line.substring(c2 + 1)
                                           : line.substring(c2 + 1, c3)).trim();
                try {
                    routes.put(Integer.parseInt(idStr), shortName);
                } catch (NumberFormatException ignored) {}
            }
        }
        return new RouteCatalog(routes);
    }

    public boolean contains(int lineId) {
        return routes.containsKey(lineId);
    }

    public String shortName(int lineId) {
        return routes.getOrDefault(lineId, "UNKNOWN");
    }

    /** Ordered map preserving insertion order for deterministic output. */
    public Map<Integer, String> allRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    public int size() {
        return routes.size();
    }
}
