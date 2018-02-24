package ru.radiomayak;

import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

public final class CacheUtils {
    private static final String FILE_DIRECTORY = "radiomayak";

    private CacheUtils() {
    }

    public static Cache getCache(File directory, String name) {
        File appDir = new File(directory, FILE_DIRECTORY);
        File cacheDir = new File(appDir, name);
        return new SimpleCache(cacheDir, new NoOpCacheEvictor());
    }
}
