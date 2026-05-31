package core.model;

public class PipelineSummary {
    private int routesLoaded;
    private int datagramsRead;
    private int datagramsValid;
    private int datagramsInvalid;
    private int datagramsProcessed;
    private int positionsSaved;
    private int errors;
    private long startedAtMillis;
    private long finishedAtMillis;

    public void start() {
        startedAtMillis = System.currentTimeMillis();
    }

    public void finish() {
        finishedAtMillis = System.currentTimeMillis();
    }

    public long getElapsedMillis() {
        if (startedAtMillis == 0L) {
            return 0L;
        }
        long end = finishedAtMillis == 0L ? System.currentTimeMillis() : finishedAtMillis;
        return end - startedAtMillis;
    }

    public int getRoutesLoaded() {
        return routesLoaded;
    }

    public void setRoutesLoaded(int routesLoaded) {
        this.routesLoaded = routesLoaded;
    }

    public int getDatagramsRead() {
        return datagramsRead;
    }

    public void incrementDatagramsRead() {
        datagramsRead++;
    }

    public int getDatagramsValid() {
        return datagramsValid;
    }

    public void incrementDatagramsValid() {
        datagramsValid++;
    }

    public int getDatagramsInvalid() {
        return datagramsInvalid;
    }

    public void incrementDatagramsInvalid() {
        datagramsInvalid++;
    }

    public int getDatagramsProcessed() {
        return datagramsProcessed;
    }

    public void incrementDatagramsProcessed() {
        datagramsProcessed++;
    }

    public int getPositionsSaved() {
        return positionsSaved;
    }

    public void incrementPositionsSaved() {
        positionsSaved++;
    }

    public int getErrors() {
        return errors;
    }

    public void incrementErrors() {
        errors++;
    }
}
