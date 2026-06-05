package processing;

import domain.Route;
import domain.RouteMonthKey;
import processing.aggregation.SpeedAccumulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes V2 results in the exact same CSV format as V1's SpeedResultWriter.
 *
 * Header: routeId,shortName,yearMonth,avgSpeedKmh,intervals,status
 *
 * Route order follows the loaded routes list (file insertion order, same as V1 catalog).
 * Each route gets:
 *   – One OK row per (route, month) that has valid intervals, sorted by year then month.
 *   – A single SIN_DATOS row (empty yearMonth, 0.0000, 0) when no intervals exist.
 */
public final class SpeedResultWriter {

    public static void write(String outputPath, AverageSpeedProcessingResult result) throws IOException {
        File out = new File(outputPath);
        if (out.getParentFile() != null) out.getParentFile().mkdirs();

        List<Route> routes = result.getRoutes();
        Map<RouteMonthKey, SpeedAccumulator> accs = result.getAccumulators();

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out)))) {
            pw.println("routeId,shortName,yearMonth,avgSpeedKmh,intervals,status");

            for (Route route : routes) {
                int routeId = route.getId();
                String shortName = route.getShortName();

                List<RouteMonthKey> monthKeys = new ArrayList<RouteMonthKey>();
                for (RouteMonthKey key : accs.keySet()) {
                    if (key.getRouteId() == routeId) {
                        SpeedAccumulator acc = accs.get(key);
                        if (acc != null && acc.hasData()) {
                            monthKeys.add(key);
                        }
                    }
                }

                if (monthKeys.isEmpty()) {
                    pw.printf(Locale.ROOT, "%d,%s,,0.0000,0,SIN_DATOS%n", routeId, shortName);
                } else {
                    Collections.sort(monthKeys); // RouteMonthKey.compareTo: routeId, year, month
                    for (RouteMonthKey key : monthKeys) {
                        SpeedAccumulator acc = accs.get(key);
                        String yearMonth = String.format("%04d-%02d", key.getYear(), key.getMonth());
                        pw.printf(Locale.ROOT, "%d,%s,%s,%.4f,%d,OK%n",
                                routeId, shortName, yearMonth,
                                acc.getAverageKmh(), acc.getIntervals());
                    }
                }
            }
        }
    }
}
