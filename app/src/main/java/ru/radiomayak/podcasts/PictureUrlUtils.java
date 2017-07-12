package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PictureUrlUtils {
    private static final Pattern pattern = Pattern.compile("^(.+/vh/pictures/)([a-z]+)(/\\d+/\\d+(/\\d+)?.[a-z]+)$");

    enum Size {
        XS_SQUARE("r", 104, 104),
        M_SQUARE("bq", 400, 400),
        M("xw", 720, 409),
        L("prm", 1020, 420),
        XL_SQUARE("it", 1400, 1400),
        XXL("o", 2560, 499);

        private final String key;
        private final int width;
        private final int height;

        Size(String key, int width, int height) {
            this.key = key;
            this.width = width;
            this.height = height;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }
    }

    private PictureUrlUtils() {
    }

    static boolean isPictureUrl(String url) {
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }


    @Nullable
    static String getPictureUrl(String url, Size size) {
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            return null;
        }
        if (size.key.equals(matcher.group(2))) {
            return url;
        }
        return matcher.group(1) + size.key + matcher.group(3);
    }
}
