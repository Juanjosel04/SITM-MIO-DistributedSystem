package app;

import core.speed.MonolithicSpeedJob;
import core.speed.RouteCatalog;
import core.speed.SpeedResultWriter;

/**
 * V1 – Monolithic headless batch: average speed per route per month.
 *
 * Usage: SpeedBatchMain [datagrams.csv] [lines.csv] [output.csv]
 *
 * Defaults (relative to working directory):
 *   datagrams : datasets/datagrams-MiniPilot.csv
 *   lines     : datasets/mini/lines-241-ActiveGT.csv
 *   output    : output/speed-results-v1.csv
 */
public final class SpeedBatchMain {

    private static final String DEFAULT_DATAGRAMS = "datasets/datagrams-MiniPilot.csv";
    private static final String DEFAULT_LINES     = "datasets/mini/lines-241-ActiveGT.csv";
    private static final String DEFAULT_OUTPUT    = "output/speed-results-v1.csv";

    public static void main(String[] args) throws Exception {
        String datagramsPath = args.length > 0 ? args[0] : DEFAULT_DATAGRAMS;
        String linesPath     = args.length > 1 ? args[1] : DEFAULT_LINES;
        String outputPath    = args.length > 2 ? args[2] : DEFAULT_OUTPUT;

        System.out.println("=== SITM-MIO Speed Batch V1 (Monolithic) ===");
        System.out.printf("Datagrams : %s%n", datagramsPath);
        System.out.printf("Lines     : %s%n", linesPath);
        System.out.printf("Output    : %s%n", outputPath);
        System.out.println();

        long t0 = System.currentTimeMillis();

        // ── 1. Load route catalog ──────────────────────────────────────────────
        System.out.print("Loading route catalog... ");
        long tCat0 = System.currentTimeMillis();
        RouteCatalog catalog = RouteCatalog.load(linesPath);
        long tCatMs = System.currentTimeMillis() - tCat0;
        System.out.printf("done  [%d active routes, %,d ms]%n", catalog.size(), tCatMs);

        // ── 2. Single-pass streaming computation ──────────────────────────────
        System.out.print("Processing datagrams (streaming)... ");
        long tComp0 = System.currentTimeMillis();
        MonolithicSpeedJob job = new MonolithicSpeedJob(catalog);
        job.run(datagramsPath);
        long tCompMs = System.currentTimeMillis() - tComp0;
        System.out.printf("done  [%,d ms]%n", tCompMs);

        // ── 3. Write CSV output ───────────────────────────────────────────────
        System.out.print("Writing results... ");
        long tWrite0 = System.currentTimeMillis();
        SpeedResultWriter.write(outputPath, catalog, job.accumulators());
        long tWriteMs = System.currentTimeMillis() - tWrite0;
        System.out.printf("done  [%,d ms]%n", tWriteMs);

        long totalMs = System.currentTimeMillis() - t0;

        // ── 4. Instrumentation summary ────────────────────────────────────────
        MonolithicSpeedJob.Counters c = job.counters();
        double throughput = tCompMs > 0 ? (c.totalRead * 1000.0 / tCompMs) : 0;

        System.out.println();
        System.out.println("══════════════════════════════════════════════");
        System.out.println(" Timing");
        System.out.println("══════════════════════════════════════════════");
        System.out.printf("  Catalog load    : %,8d ms%n", tCatMs);
        System.out.printf("  Compute         : %,8d ms  (read + calc)%n", tCompMs);
        System.out.printf("  Write output    : %,8d ms%n", tWriteMs);
        System.out.printf("  Total           : %,8d ms%n", totalMs);
        System.out.printf("  Throughput      : %,12.0f datagrams/s%n", throughput);
        System.out.println();
        System.out.println("══════════════════════════════════════════════");
        System.out.println(" Counters");
        System.out.println("══════════════════════════════════════════════");
        System.out.printf("  Total read         : %,d%n", c.totalRead);
        System.out.printf("  No route/inactive  : %,d%n", c.noRouteOrInactive);
        System.out.printf("  Malformed lines    : %,d%n", c.malformed);
        System.out.printf("  Valid intervals    : %,d%n", c.validIntervals);
        System.out.println("  Discarded intervals:");
        System.out.printf("    No previous point  : %,d%n", c.discardNoPreview);
        System.out.printf("    Route changed      : %,d%n", c.discardRouteChanged);
        System.out.printf("    dt <= 0            : %,d%n", c.discardDtInvalid);
        System.out.printf("    dt > 600 s         : %,d%n", c.discardDtTooLong);
        System.out.printf("    Odometer < 0       : %,d%n", c.discardBadOdometer);
        System.out.printf("    No distance gain   : %,d%n", c.discardNoGain);
        System.out.printf("    Speed > 120 km/h   : %,d%n", c.discardSpeedTooHigh);
        System.out.println();
        System.out.println("══════════════════════════════════════════════");
        System.out.printf("Output: %s%n", outputPath);
    }
}
