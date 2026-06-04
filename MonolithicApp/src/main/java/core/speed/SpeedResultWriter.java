package core.speed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpeedResultWriter {

    /**
     * Writes speed results to {@code outputPath}.
     *
     * Format: routeId,shortName,yearMonth,avgSpeedKmh,intervals,status
     *
     * Every active route in the catalog gets at least one row:
     *   – One row per (route, month) with valid intervals → status=OK
     *   – A single SIN_DATOS row for routes with no valid intervals at all.
     */
    public static void write(String outputPath,
                             RouteCatalog catalog,
                             Map<String, RouteMonthAccumulator> accumulators)
            throws IOException {

        File out = new File(outputPath);
        if (out.getParentFile() != null) out.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter(out)))) {

            pw.println("routeId,shortName,yearMonth,avgSpeedKmh,intervals,status");

            for (Map.Entry<Integer, String> entry : catalog.allRoutes().entrySet()) {
                int    routeId   = entry.getKey();
                String shortName = entry.getValue();
                String prefix    = routeId + "_";

                List<Map.Entry<String, RouteMonthAccumulator>> matching = new ArrayList<>();
                for (Map.Entry<String, RouteMonthAccumulator> e : accumulators.entrySet()) {
                    if (e.getKey().startsWith(prefix)) matching.add(e);
                }

                if (matching.isEmpty()) {
                    pw.printf(Locale.ROOT, "%d,%s,,0.0000,0,SIN_DATOS%n",
                            routeId, shortName);
                } else {
                    matching.sort(Map.Entry.comparingByKey()); // chronological order
                    for (Map.Entry<String, RouteMonthAccumulator> e : matching) {
                        String yearMonth = e.getKey().substring(prefix.length());
                        RouteMonthAccumulator acc = e.getValue();
                        pw.printf(Locale.ROOT, "%d,%s,%s,%.4f,%d,OK%n",
                                routeId, shortName, yearMonth,
                                acc.avgKmh(), acc.intervals());
                    }
                }
            }
        }
    }
}
