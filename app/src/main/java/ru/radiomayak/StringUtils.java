package ru.radiomayak;

import android.support.annotation.Nullable;

import java.util.regex.Pattern;

public final class StringUtils {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\u00A0*[\\s&&[^\\u00A0]][\\u00A0\\s]*");

    public static int parseInt(String string, int defaultValue) {
        if (string != null && !string.isEmpty()) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static long parseLong(String string, long defaultValue) {
        if (string != null && !string.isEmpty()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static boolean equals(String s1, String s2) {
        return s1 != null && s2 != null ? s1.equals(s2) : s1 == null && s2 == null;
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        return s1 != null && s2 != null ? s1.equalsIgnoreCase(s2) : s1 == null && s2 == null;
    }


    @Nullable
    public static String normalize(@Nullable String string) {
        if (string == null) {
            return null;
        }
        if (string.isEmpty()) {
            return string;
        }
        return WHITESPACE_PATTERN.matcher(string).replaceAll(" ").trim();
    }

    @Nullable
    public static String nonEmpty(@Nullable String string) {
        return string == null || string.isEmpty() ? null : string;
    }

    @Nullable
    public static String nonEmptyTrimmed(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        String trimmed = string.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    public static String nonEmptyNormalized(@Nullable String string) {
        String normalized = normalize(string);
        return nonEmpty(normalized);
    }

    @Nullable
    public static String nonEmpty(@Nullable String string, String defaultString) {
        return string == null || string.isEmpty() ? defaultString : string;
    }

    public static String requireNonEmpty(String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return string;
    }

    public static String join(String[] strings, String delimiter) {
        int length = 0;
        for (int i = 0; i < strings.length; i++) {
            length += strings[i].length();
            if (i > 0) {
                length += delimiter.length();
            }
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(strings[i]);
        }
        return builder.toString();
    }

    private StringUtils() {
    }
}
