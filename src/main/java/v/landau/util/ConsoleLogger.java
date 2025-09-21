package v.landau.util;

// ConsoleLogger.java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple console logger with colored output support.
 * Singleton pattern for consistent logging across the application.
 */
public class ConsoleLogger {
    private static ConsoleLogger instance;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";

    private boolean useColors;

    private ConsoleLogger() {
        // Check if console supports colors (basic check)
        this.useColors = System.console() != null &&
                !System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static ConsoleLogger getInstance() {
        if (instance == null) {
            instance = new ConsoleLogger();
        }
        return instance;
    }

    public void info(String message) {
        log("INFO", message, CYAN);
    }

    public void success(String message) {
        log("SUCCESS", message, GREEN);
    }

    public void warn(String message) {
        log("WARN", message, YELLOW);
    }

    public void error(String message) {
        log("ERROR", message, RED);
    }

    public void debug(String message) {
        log("DEBUG", message, BLUE);
    }

    private void log(String level, String message, String color) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);

        if (useColors && color != null) {
            System.out.println(String.format("%s[%s] [%s]%s %s",
                    color, timestamp, level, RESET, message));
        } else {
            System.out.println(String.format("[%s] [%s] %s",
                    timestamp, level, message));
        }
    }

    public void setUseColors(boolean useColors) {
        this.useColors = useColors;
    }
}