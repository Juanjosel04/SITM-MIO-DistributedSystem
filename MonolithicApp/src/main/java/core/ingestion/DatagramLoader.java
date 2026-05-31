package core.ingestion;

import core.processing.coordinator.ProcessingCoordinator;

import java.io.IOException;

public class DatagramLoader {
    private final CsvReader csvReader;
    private final ProcessingCoordinator processingCoordinator;
    private final int delayMillis;

    public DatagramLoader(CsvReader csvReader, ProcessingCoordinator processingCoordinator, int delayMillis) {
        this.csvReader = csvReader;
        this.processingCoordinator = processingCoordinator;
        this.delayMillis = delayMillis;
    }

    public void stream(String path) throws IOException {
        CsvContent content = csvReader.read(path, false);
        for (String line : content.getLines()) {
            processingCoordinator.processLine(line);
            waitBeforeNextLine();
        }
        processingCoordinator.finish();
    }

    private void waitBeforeNextLine() {
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
