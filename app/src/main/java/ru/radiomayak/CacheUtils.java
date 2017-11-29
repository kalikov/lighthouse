package ru.radiomayak;

import java.io.File;

public final class CacheUtils {
    private static final String FILE_DIRECTORY = "radiomayak";
    private static final String FILE_PREFIX = "";
    private static final String FILE_SUFFIX = ".cache";

    private CacheUtils() {
    }

    public static File getCategoryDirectory(File directory, String category) {
        File appDir = new File(directory, FILE_DIRECTORY);
        return new File(appDir, category);
    }

    public static File getFile(File directory, String category, String id) {
        return new File(getCategoryDirectory(directory, category), FILE_PREFIX + id + FILE_SUFFIX);
    }
}
