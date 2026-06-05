package processing.streaming;

public final class BucketProcessingConfig {
    public static final int DEFAULT_VISUAL_SAMPLE_LIMIT = 3000;

    private final int parallelism;
    private final int bucketCount;
    private final int bucketTaskThreshold;
    private final int visualSampleLimit;

    public BucketProcessingConfig(int parallelism, int bucketCount, int bucketTaskThreshold, int visualSampleLimit) {
        this.parallelism = Math.max(1, parallelism);
        this.bucketCount = Math.max(1, bucketCount);
        this.bucketTaskThreshold = Math.max(1, bucketTaskThreshold);
        this.visualSampleLimit = Math.max(0, visualSampleLimit);
    }

    public static BucketProcessingConfig defaults(int parallelism) {
        int safeParallelism = Math.max(1, parallelism);
        int bucketCount = integerProperty("sitm.concurrent.bucketCount", Math.max(safeParallelism * 4, 16));
        int threshold = integerProperty("sitm.concurrent.bucketTaskThreshold", 1);
        int sampleLimit = integerProperty("sitm.concurrent.visualSampleLimit", DEFAULT_VISUAL_SAMPLE_LIMIT);
        return new BucketProcessingConfig(safeParallelism, bucketCount, threshold, sampleLimit);
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public int getBucketTaskThreshold() {
        return bucketTaskThreshold;
    }

    public int getVisualSampleLimit() {
        return visualSampleLimit;
    }

    private static int integerProperty(String name, int fallback) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
