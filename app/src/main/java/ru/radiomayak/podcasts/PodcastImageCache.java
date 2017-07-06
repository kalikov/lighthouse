package ru.radiomayak.podcasts;

import android.graphics.Bitmap;
import android.util.LongSparseArray;
import android.util.LruCache;

public class PodcastImageCache {
    private static final int DEFAULT_IMAGES_CAPACITY = 100;

    private static final int SPLASH_CACHE_SIZE = 10 * 1024 * 1024;

    private static final PodcastImageCache instance = new PodcastImageCache();

    private final LongSparseArray<Bitmap> icons = new LongSparseArray<>(DEFAULT_IMAGES_CAPACITY);
    private final LruCache<Long, Bitmap> splashs = new LruCache<>(SPLASH_CACHE_SIZE);

    public static PodcastImageCache getInstance() {
        return instance;
    }

    public Bitmap getIcon(long id) {
        return icons.get(id);
    }

    public void putIcon(long id, Bitmap bitmap) {
        icons.put(id, bitmap);
    }

    public Bitmap getSplash(long id) {
        return splashs.get(id);
    }

    public void setSplash(long id, Bitmap bitmap) {
        splashs.put(id, bitmap);
    }
}
