package ru.radiomayak;

import java.io.File;

public final class CacheUtils {
    static final String FILE_PREFIX = "radiomayak";
    static final String FILE_SUFFIX = ".cache";

    private CacheUtils() {

    }

    public static File getFile(File directory, long category, String id) {
        return new File(directory, FILE_PREFIX + id + FILE_SUFFIX);
    }
}
