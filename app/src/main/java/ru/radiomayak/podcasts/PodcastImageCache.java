package ru.radiomayak.podcasts;

import android.util.LruCache;

import ru.radiomayak.graphics.BitmapInfo;

public class PodcastImageCache {
    private static final int IMAGE_CACHE_SIZE = 100;

    private static final int SPLASH_CACHE_SIZE = 4;

    private static final PodcastImageCache instance = new PodcastImageCache();

    private final LruCache<Long, BitmapInfo> icons = new LruCache<>(IMAGE_CACHE_SIZE);
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
