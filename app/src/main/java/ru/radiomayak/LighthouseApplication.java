package ru.radiomayak;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.media.MediaPlayerService;

public class LighthouseApplication extends Application {
    private static final int KEEP_ALIVE_SECONDS = 30;

    private static final ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, "NetworkPoolThread #" + counter.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<>(128);

    public static final Executor NETWORK_POOL_EXECUTOR;

    static {
        NETWORK_POOL_EXECUTOR = new ThreadPoolExecutor(1, 3, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, executorQueue, threadFactory);
    }

    public static final Executor NETWORK_SERIAL_EXECUTOR = Executors.newSingleThreadExecutor();

    private final LoaderManager<BitmapInfo> imageLoaderManager = new LoaderManager<>(true);

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            broadcastUpdate();
        }

        @Override
        public void onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
            broadcastUpdate();
        }

        @Override
        public void onConnectionFailed() {
            mediaBrowser.connect();
        }
    };

    private MediaBrowserCompat mediaBrowser;

    private Typeface fontBold;
    private Typeface fontNormal;
    private Typeface fontLight;

    @Override
    public void onCreate() {
        super.onCreate();

        fontBold = Typeface.createFromAsset(getAssets(), "fonts/russia-bold.ttf");
        fontNormal = Typeface.createFromAsset(getAssets(), "fonts/russia-normal.ttf");
        fontLight = Typeface.createFromAsset(getAssets(), "fonts/russia-light.ttf");

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaPlayerService.class), connectionCallbacks, null);
        mediaBrowser.connect();
    }

    @Override
    public void onTerminate() {
        mediaBrowser.disconnect();

        super.onTerminate();
    }

    public Typeface getFontBold() {
        return fontBold;
    }

    public Typeface getFontNormal() {
        return fontNormal;
    }

    public Typeface getFontLight() {
        return fontLight;
    }

    public LoaderManager<BitmapInfo> getImageLoaderManager() {
        return imageLoaderManager;
    }

    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowser;
    }

    private void broadcastUpdate() {
        Intent intent = new Intent(LighthouseActivity.ACTION_UPDATE).setPackage(getPackageName());
        sendBroadcast(intent);
    }
}
