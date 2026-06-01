package analytics.model;

public class ThroughputStatistic {
    private final int datagramsRead;
    private final int datagramsValid;
    private final int datagramsInvalid;
    private final int datagramsProcessed;
    private final int errors;
    private final long processingTimeMs;
    private final double datagramsPerSecond;

    public ThroughputStatistic(int datagramsRead, int datagramsValid, int datagramsInvalid,
                               int datagramsProcessed, int errors, long processingTimeMs) {
        this.datagramsRead = datagramsRead;
        this.datagramsValid = datagramsValid;
        this.datagramsInvalid = datagramsInvalid;
        this.datagramsProcessed = datagramsProcessed;
        this.errors = errors;
        this.processingTimeMs = processingTimeMs;
        this.datagramsPerSecond = processingTimeMs <= 0L ? 0.0 :
                datagramsProcessed / (processingTimeMs / 1000.0);
    }

    public int getDatagramsRead() {
        return datagramsRead;
    }

    public int getDatagramsValid() {
        return datagramsValid;
    }

    public int getDatagramsInvalid() {
        return datagramsInvalid;
    }

    public int getDatagramsProcessed() {
        return datagramsProcessed;
    }

    public int getErrors() {
        return errors;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public double getDatagramsPerSecond() {
        return datagramsPerSecond;
    }
}
