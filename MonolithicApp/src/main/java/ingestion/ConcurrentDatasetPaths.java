package ingestion;

import shared.constants.DatasetPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConcurrentDatasetPaths {
    public static final String ROUTES_FILE = DatasetPaths.ROUTES_FILE;
    public static final String DATAGRAMS_MINI_PILOT_FILE = DatasetPaths.MINI_DIRECTORY + "/datagrams-MiniPilot.csv";
    public static final String DATAGRAMS_FULL_PILOT_FILE = DatasetPaths.MINI_DIRECTORY + "/datagrams4Pilot.csv";
    public static final String DATAGRAMS_FALLBACK_FILE = DatasetPaths.DATAGRAMS_FILE;

    private ConcurrentDatasetPaths() {
    }

    public static String defaultDatagramsFile() {
        if (exists(DATAGRAMS_MINI_PILOT_FILE)) {
            return DATAGRAMS_MINI_PILOT_FILE;
        }
        if (exists(DATAGRAMS_FULL_PILOT_FILE)) {
            return DATAGRAMS_FULL_PILOT_FILE;
        }
        return DATAGRAMS_FALLBACK_FILE;
    }

    private static boolean exists(String path) {
        Path direct = Paths.get(path);
        Path parent = Paths.get("..").resolve(path).normalize();
        return Files.exists(direct) || Files.exists(parent);
    }
}
