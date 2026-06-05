package app;

import domain.Route;
import ingestion.RouteLoader;
import processing.AverageSpeedProcessingResult;
import processing.SpeedResultWriter;
import processing.benchmark.ProcessingMetrics;
import processing.streaming.BucketProcessingConfig;
import processing.streaming.StreamingBucketedAverageSpeedProcessor;

import java.util.List;

/**
 * V2 headless batch runner — no JavaFX, no GUI.
 *
 * Usage: ConcurrentBatchMain [datagrams] [lines] [output]
 *
 * Defaults:
 *   datagrams : datasets/mini/datagrams-MiniPilot.csv
 *   lines     : datasets/mini/lines-241-ActiveGT.csv
 *   output    : output/speed-results-v2.csv
 *
 * Gradle shortcut: ./gradlew :MonolithicApp:runV2
 *   with optional overrides: -Pdatagrams=... -Plines=... -Pout=...
 */
public final class ConcurrentBatchMain {

    private static final String DEFAULT_DATAGRAMS = "datasets/mini/datagrams-MiniPilot.csv";
    private static final String DEFAULT_LINES     = "datasets/mini/lines-241-ActiveGT.csv";
    private static final String DEFAULT_OUTPUT    = "output/speed-results-v2.csv";

    public static void main(String[] args) throws Exception {
        String datagramsPath = args.length > 0 ? args[0] : DEFAULT_DATAGRAMS;
        String linesPath     = args.length > 1 ? args[1] : DEFAULT_LINES;
        String outputPath    = args.length > 2 ? args[2] : DEFAULT_OUTPUT;

        int parallelism = StreamingBucketedAverageSpeedProcessor.DEFAULT_PARALLELISM;

        System.out.println("=== SITM-MIO Speed Batch V2 (Concurrent) ===");
        System.out.printf("Datagrams : %s%n", datagramsPath);
        System.out.printf("Lines     : %s%n", linesPath);
        System.out.printf("Output    : %s%n", outputPath);
        System.out.printf("Threads   : %d%n", parallelism);
        System.out.println();

        long t0 = System.currentTimeMillis();

        // 1. Load route catalog
        System.out.print("Loading routes... ");
        List<Route> routes = new RouteLoader().load(linesPath);
        long tRoutes = System.currentTimeMillis() - t0;
        System.out.printf("done  [%d routes, %d ms]%n", routes.size(), tRoutes);

        // 2. Run streaming + ForkJoin computation
        System.out.print("Processing (streaming bucketize + ForkJoin)... ");
        BucketProcessingConfig config = BucketProcessingConfig.defaults(parallelism);
        StreamingBucketedAverageSpeedProcessor processor =
                new StreamingBucketedAverageSpeedProcessor(datagramsPath, config);
        AverageSpeedProcessingResult result = processor.process(routes, null);
        long tProc = System.currentTimeMillis() - t0 - tRoutes;
        System.out.printf("done  [%d ms]%n", tProc);

        // 3. Write CSV
        System.out.print("Writing results... ");
        SpeedResultWriter.write(outputPath, result);
        long tWrite = System.currentTimeMillis() - t0 - tRoutes - tProc;
        System.out.printf("done  [%d ms]%n", tWrite);

        long totalMs = System.currentTimeMillis() - t0;

        ProcessingMetrics m = result.getMetrics();

        System.out.println();
        System.out.println("══════════════════════════════════════════════");
        System.out.println(" Timing");
        System.out.println("══════════════════════════════════════════════");
        System.out.printf("  Bucketización       : %,d ms%n", m.getBucketizationTimeMillis());
        System.out.printf("  Fork/Join           : %,d ms%n", m.getForkJoinProcessingTimeMillis());
        System.out.printf("  Total (wall clock)  : %,d ms%n", totalMs);
        System.out.printf("  Throughput          : %,.0f d/s%n", m.getThroughputDatagramsPerSecond());
        System.out.printf("  Hilos               : %d%n", m.getParallelism());
        System.out.println();
        System.out.println("══════════════════════════════════════════════");
        System.out.println(" Counters");
        System.out.println("══════════════════════════════════════════════");
        System.out.printf("  Datagramas leidos   : %,d%n", m.getReadDatagrams());
        System.out.printf("  Intervalos validos  : %,d%n", m.getValidIntervals());
        System.out.printf("  Rutas con resultado : %d / %d%n",
                m.getRoutesWithResult(), m.getLoadedRoutes());
        System.out.printf("  Rutas sin datos     : %,d%n", m.getRoutesWithoutData());
        System.out.printf("  Velocidad global    : %.2f km/h%n",
                m.getGlobalAverageSpeedKmh());
        System.out.println();
        System.out.printf("Output: %s%n", outputPath);
    }
}
