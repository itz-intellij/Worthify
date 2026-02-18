package dev.simpleye.worthify.selltools;

public final class TimeUnitParser {

    private TimeUnitParser() {
    }

    public static long parseDurationMillis(int amount, String unit) {
        if (amount <= 0 || unit == null) {
            return -1L;
        }
        String u = unit.trim().toLowerCase(java.util.Locale.ROOT);
        long seconds;
        if (u.equals("second") || u.equals("seconds") || u.equals("sec") || u.equals("secs") || u.equals("s")) {
            seconds = amount;
        } else if (u.equals("minute") || u.equals("minutes") || u.equals("min") || u.equals("mins") || u.equals("m")) {
            seconds = amount * 60L;
        } else if (u.equals("hour") || u.equals("hours") || u.equals("h")) {
            seconds = amount * 3600L;
        } else if (u.equals("day") || u.equals("days") || u.equals("d")) {
            seconds = amount * 86400L;
        } else {
            return -1L;
        }
        return seconds * 1000L;
    }
}
