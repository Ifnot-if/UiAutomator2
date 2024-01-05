package com.noinvasion.uiautomator2;

public final class LogUtil {

    private static final String PREFIX = "[uiautomator] ";

    public enum Level {
        INFO, DEBUG, ERROR
    }

    private static Level threshold = Level.DEBUG;

    private LogUtil() {
        // not instantiable
    }

    public static void initLogLevel(Level level) {
        threshold = level;
    }

    public static boolean isEnabled(Level level) {
        return level.ordinal() >= threshold.ordinal();
    }

    public static void i(String message) {
        if (isEnabled(Level.INFO)) {
            System.out.println(PREFIX + "INFO: " + message);
        }
    }

    public static void d(String message) {
        if (isEnabled(Level.DEBUG)) {
            System.out.println(PREFIX + "DEBUG: " + message);
        }
    }

    public static void d(String message, Throwable throwable) {
        if (isEnabled(Level.DEBUG)) {
            System.out.println(PREFIX + "DEBUG: " + message);
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }

    public static void e(String message, Throwable throwable) {
        if (isEnabled(Level.ERROR)) {
            System.out.println(PREFIX + "ERROR: " + message);
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }

    public static void e(String message) {
        e(message, null);
    }
}
