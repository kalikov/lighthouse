package ru.radiomayak.podcasts;

import android.support.annotation.VisibleForTesting;
import android.util.LruCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.radiomayak.graphics.BitmapInfo;

public class PodcastImageCache {
    private static final int ICONS_INITIAL_CAPACITY = 100;

    private static final int SPLASH_CACHE_SIZE = 4;

    private static final PodcastImageCache instance = new PodcastImageCache();

    private final Map<Long, BitmapInfo> icons = new ConcurrentHashMap<>(ICONS_INITIAL_CAPACITY);

    @VisibleForTesting
    final LruCache<Long, BitmapInfo> splashs = new LruCache<>(SPLASH_CACHE_SIZE);

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
