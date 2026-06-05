package ingestion;

import shared.constants.DatasetPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConcurrentDatasetPaths {
    public static final String ROUTES_FILE = DatasetPaths.ROUTES_FILE;

    // ==============================
    // RUTAS DE DATAGRAMAS DISPONIBLES
    // ==============================

    public static final String DATAGRAMS_MINI_PILOT_FILE =
            DatasetPaths.MINI_DIRECTORY + "/datagrams-MiniPilot.csv";


    public static final String DATAGRAMS_FALLBACK_FILE =
            DatasetPaths.DATAGRAMS_FILE;

    public static final String DATAGRAMS_3GB_FILE =
            "datasets/mini/bloque_3gb.csv";

    public static final String DATAGRAMS_7GB_FILE =
            "datasets/medium/bloque_7gb.csv";

    /*
     * ==========================================================
     * DATASET ACTIVO PARA V2 CONCURRENTE
     * ==========================================================
     *
     * Para cambiar el datagrama, cambia SOLO esta línea:
     */
    public static final String ACTIVE_DATAGRAMS_FILE = DATAGRAMS_3GB_FILE;

    private ConcurrentDatasetPaths() {
    }

    public static String defaultDatagramsFile() {
        if (exists(ACTIVE_DATAGRAMS_FILE)) {
            return ACTIVE_DATAGRAMS_FILE;
        }

        throw new IllegalStateException(
                "El dataset activo no existe: " + ACTIVE_DATAGRAMS_FILE
                        + ". Revisa la ruta en ConcurrentDatasetPaths.ACTIVE_DATAGRAMS_FILE."
        );
    }

    private static boolean exists(String path) {
        Path direct = Paths.get(path);
        Path parent = Paths.get("..").resolve(path).normalize();
        return Files.exists(direct) || Files.exists(parent);
    }
}