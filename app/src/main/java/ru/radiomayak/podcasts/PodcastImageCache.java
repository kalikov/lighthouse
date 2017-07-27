package ru.radiomayak.podcasts;

import android.util.LongSparseArray;
import android.util.LruCache;

import ru.radiomayak.graphics.BitmapInfo;

public class PodcastImageCache {
    private static final int DEFAULT_IMAGES_CAPACITY = 100;

    private static final int SPLASH_CACHE_SIZE = 10 * 1024 * 1024;

    private static final PodcastImageCache instance = new PodcastImageCache();

    private final LongSparseArray<BitmapInfo> icons = new LongSparseArray<>(DEFAULT_IMAGES_CAPACITY);
    private final LruCache<Long, BitmapInfo> splashs = new LruCache<>(SPLASH_CACHE_SIZE);

    public static PodcastImageCache getInstance() {
        return instance;
    }

    public BitmapInfo getIcon(long id) {
        return icons.get(id);
    }

    public void putIcon(long id, BitmapInfo info) {
        icons.put(id, info);
    }

    public BitmapInfo getSplash(long id) {
        return splashs.get(id);
    }

    public void setSplash(long id, BitmapInfo info) {
        splashs.put(id, info);
    }
}
