package dev.simpleye.worthify.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MoneyUtil {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([kKmMbBtT]?)\\s*$");

    private MoneyUtil() {
    }

    public static Double parseAmount(String input) {
        if (input == null) {
            return null;
        }

        Matcher m = AMOUNT_PATTERN.matcher(input);
        if (!m.matches()) {
            return null;
        }

        double value;
        try {
            value = Double.parseDouble(m.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }

        String suffix = m.group(2);
        double mult = 1.0D;
        if (suffix != null && !suffix.isEmpty()) {
            char c = Character.toLowerCase(suffix.charAt(0));
            if (c == 'k') {
                mult = 1_000.0D;
            } else if (c == 'm') {
                mult = 1_000_000.0D;
            } else if (c == 'b') {
                mult = 1_000_000_000.0D;
            } else if (c == 't') {
                mult = 1_000_000_000_000.0D;
            }
        }

        double out = value * mult;
        if (!(out > 0.0D) || Double.isNaN(out) || Double.isInfinite(out)) {
            return null;
        }
        return out;
    }

    public static String format(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return "0.00";
        }
        return String.format(Locale.US, "%,.2f", amount);
    }
}
