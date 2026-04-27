package com.pathdlc.digger.warden;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WardenTimerParser {
    private static final Pattern MM_SS = Pattern.compile("(\\d{1,2})\\s*[:：]\\s*(\\d{1,2})");
    private static final Pattern SECONDS = Pattern.compile("(\\d{1,3})\\s*(?:s|sec|сек|с)");

    public static double parseSeconds(String text) {
        if (text == null || text.isBlank()) {
            return -1.0;
        }

        String clean = text.toLowerCase();

        Matcher mmss = MM_SS.matcher(clean);

        if (mmss.find()) {
            int minutes = parseInt(mmss.group(1));
            int seconds = parseInt(mmss.group(2));
            return Math.max(0, minutes * 60 + seconds);
        }

        Matcher seconds = SECONDS.matcher(clean);

        if (seconds.find()) {
            return Math.max(0, parseInt(seconds.group(1)));
        }

        return -1.0;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private WardenTimerParser() {
    }
}
