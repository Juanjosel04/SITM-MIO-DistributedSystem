package core.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AppLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    private static void log(String level, String message) {
        System.out.println(FORMATTER.format(LocalDateTime.now()) + " [" + level + "] " + message);
    }
}
