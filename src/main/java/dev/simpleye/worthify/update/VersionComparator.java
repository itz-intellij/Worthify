package dev.simpleye.worthify.update;

public final class VersionComparator {

    private VersionComparator() {
    }

    public static boolean isNewer(String candidate, String current) {
        if (candidate == null || candidate.isEmpty() || current == null || current.isEmpty()) {
            return false;
        }

        int cmp = compare(candidate, current);
        return cmp > 0;
    }

    public static int compare(String a, String b) {
        String aa = normalize(a);
        String bb = normalize(b);

        int[] ap = parseParts(aa);
        int[] bp = parseParts(bb);

        int max = Math.max(ap.length, bp.length);
        for (int i = 0; i < max; i++) {
            int av = i < ap.length ? ap[i] : 0;
            int bv = i < bp.length ? bp[i] : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }

        // Fallback to string compare if numeric parts are equal (handles qualifiers).
        return aa.compareToIgnoreCase(bb);
    }

    private static String normalize(String v) {
        String out = v.trim();
        if (out.startsWith("v") || out.startsWith("V")) {
            out = out.substring(1);
        }
        return out;
    }

    private static int[] parseParts(String v) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.') {
                digits.append(c);
            } else {
                break;
            }
        }

        String numeric = digits.toString();
        if (numeric.isEmpty()) {
            return new int[0];
        }

        String[] parts = numeric.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                out[i] = 0;
            }
        }
        return out;
    }
}
