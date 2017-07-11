package ru.radiomayak.podcasts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PictureUrlUtils {
    private static final Pattern pattern = Pattern.compile("^(.+/vh/pictures/)([a-z]+)(/\\d+/\\d+(/\\d+)?.[a-z]+)$");

    enum Quality {
        R("r");

        private final String key;

        Quality(String key) {
            this.key = key;
        }
    }

    private PictureUrlUtils() {
    }

    static boolean isPictureUrl(String url) {
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    static String getPictureUrl(String url, Quality quality) {
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(url);
        }
        if (quality.key.equals(matcher.group(2))) {
            return url;
        }
        return matcher.group(1) + quality.key + matcher.group(3);
    }
}
