package core.speed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Single-pass streaming computation of average speed per route per month.
 *
 * Datagram CSV (no header, 12 cols):
 *   [3]  odometer      – metres since last stop; -1 = no data
 *   [7]  lineId        – route id; -1 = no route
 *   [10] datagramDate  – "yyyy-MM-dd HH:mm:ss"
 *   [11] busId
 */
public final class MonolithicSpeedJob {

    // ── Counters ──────────────────────────────────────────────────────────────

    public static final class Counters {
        public long totalRead          = 0;
        public long noRouteOrInactive  = 0; // lineId == -1 or not in catalog
        public long malformed          = 0; // unparseable line
        public long validIntervals     = 0;
        // per-discard-reason (only for lines that passed route check)
        public long discardNoPreview   = 0; // first datagram for this bus
        public long discardRouteChanged = 0;
        public long discardDtInvalid   = 0; // dt <= 0
        public long discardDtTooLong   = 0; // dt > 600 s
        public long discardBadOdometer = 0; // prev or cur odo < 0
        public long discardNoGain      = 0; // dDist <= 0
        public long discardSpeedTooHigh = 0; // > 120 km/h
    }

    // ── Last seen point per bus ───────────────────────────────────────────────

    private static final class LastPoint {
        final long odometer;    // metres
        final long epochSeconds;
        final int  routeId;

        LastPoint(long odometer, long epochSeconds, int routeId) {
            this.odometer    = odometer;
            this.epochSeconds = epochSeconds;
            this.routeId     = routeId;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final RouteCatalog catalog;
    // key: "<routeId>_<yyyy-MM>"  e.g. "131_2019-05"
    private final Map<String, RouteMonthAccumulator> accumulators = new HashMap<>();
    private final Map<Integer, LastPoint> lastPoints = new HashMap<>();
    private final Counters counters = new Counters();

    public MonolithicSpeedJob(RouteCatalog catalog) {
        this.catalog = catalog;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void run(String datagramsPath) throws IOException {
        // 1 MB read buffer – enough for ~12 000 rows per I/O call
        try (BufferedReader br = new BufferedReader(
                new FileReader(datagramsPath), 1 << 20)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                counters.totalRead++;
                processLine(line);
            }
        }
    }

    public Map<String, RouteMonthAccumulator> accumulators() {
        return Collections.unmodifiableMap(accumulators);
    }

    public Counters counters() { return counters; }

    // ── Hot path ──────────────────────────────────────────────────────────────

    private void processLine(String line) {
        // Fast split: scan once, capture field boundaries for the 12 columns.
        // We need indices 3, 7, 10, 11.
        int f3s = -1, f3e = -1;
        int f7s = -1, f7e = -1;
        int f10s = -1, f10e = -1;
        int f11s = -1;
        int field = 0, start = 0, len = line.length();

        for (int i = 0; i < len; i++) {
            if (line.charAt(i) == ',') {
                switch (field) {
                    case 3:  f3s  = start; f3e  = i; break;
                    case 7:  f7s  = start; f7e  = i; break;
                    case 10: f10s = start; f10e = i; break;
                }
                field++;
                start = i + 1;
                if (field == 11) { f11s = start; break; } // field 11 runs to EOL
            }
        }

        if (field < 11 || f3s < 0 || f7s < 0 || f10s < 0 || f11s < 0) {
            counters.malformed++;
            return;
        }

        // Parse lineId (col 7)
        int lineId;
        try {
            lineId = parseIntSubstr(line, f7s, f7e);
        } catch (NumberFormatException e) {
            counters.malformed++;
            return;
        }

        // Discard if no route or not in active catalog (do NOT update lastPoint)
        if (lineId == -1 || !catalog.contains(lineId)) {
            counters.noRouteOrInactive++;
            return;
        }

        // Parse remaining needed fields
        long odometer;
        int  busId;
        String ts;
        try {
            odometer = parseLongSubstr(line, f3s, f3e);
            // busId: field 11 goes to end of line; trim potential \r
            int busEnd = len;
            while (busEnd > f11s && line.charAt(busEnd - 1) <= ' ') busEnd--;
            busId = parseIntSubstr(line, f11s, busEnd);
            ts = line.substring(f10s, f10e);
        } catch (NumberFormatException e) {
            counters.malformed++;
            return;
        }

        long epochSecs  = parseEpochSeconds(ts);
        String yearMonth = ts.substring(0, 7); // "yyyy-MM" – fixed positions

        LastPoint prev = lastPoints.get(busId);

        if (prev != null) {
            long dt    = epochSecs    - prev.epochSeconds;
            long dDist = odometer     - prev.odometer;

            if (prev.routeId != lineId) {
                counters.discardRouteChanged++;
            } else if (dt <= 0) {
                counters.discardDtInvalid++;
            } else if (dt > 600) {
                counters.discardDtTooLong++;
            } else if (prev.odometer < 0 || odometer < 0) {
                counters.discardBadOdometer++;
            } else if (dDist <= 0) {
                counters.discardNoGain++;
            } else {
                double speedKmh = (dDist / (double) dt) * 3.6;
                if (speedKmh > 120.0) {
                    counters.discardSpeedTooHigh++;
                } else {
                    accumulators.computeIfAbsent(lineId + "_" + yearMonth,
                                    k -> new RouteMonthAccumulator())
                                .add(dDist, dt);
                    counters.validIntervals++;
                }
            }
        } else {
            counters.discardNoPreview++;
        }

        // Always update last point (route was valid/active)
        lastPoints.put(busId, new LastPoint(odometer, epochSecs, lineId));
    }

    // ── Parsing helpers ───────────────────────────────────────────────────────

    /** Parses epoch seconds from a fixed-format "yyyy-MM-dd HH:mm:ss" string. */
    private static long parseEpochSeconds(String ts) {
        int year  = d4(ts, 0);
        int month = d2(ts, 5);
        int day   = d2(ts, 8);
        int hour  = d2(ts, 11);
        int min   = d2(ts, 14);
        int sec   = d2(ts, 17);
        return LocalDateTime.of(year, month, day, hour, min, sec)
                            .toEpochSecond(ZoneOffset.UTC);
    }

    private static int d2(String s, int i) {
        return (s.charAt(i) - '0') * 10 + (s.charAt(i + 1) - '0');
    }

    private static int d4(String s, int i) {
        return (s.charAt(i)   - '0') * 1000
             + (s.charAt(i+1) - '0') * 100
             + (s.charAt(i+2) - '0') * 10
             + (s.charAt(i+3) - '0');
    }

    private static int parseIntSubstr(String s, int from, int to) {
        boolean neg = (s.charAt(from) == '-');
        int start = neg ? from + 1 : from;
        int val = 0;
        for (int i = start; i < to; i++) {
            val = val * 10 + (s.charAt(i) - '0');
        }
        return neg ? -val : val;
    }

    private static long parseLongSubstr(String s, int from, int to) {
        boolean neg = (s.charAt(from) == '-');
        int start = neg ? from + 1 : from;
        long val = 0;
        for (int i = start; i < to; i++) {
            val = val * 10 + (s.charAt(i) - '0');
        }
        return neg ? -val : val;
    }
}
