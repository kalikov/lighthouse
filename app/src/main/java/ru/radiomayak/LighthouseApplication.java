package ru.radiomayak;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.IOException;
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
import ru.radiomayak.media.MediaNotificationManager;
import ru.radiomayak.media.MediaPlayerObservable;
import ru.radiomayak.media.MediaPlayerObserver;

public class LighthouseApplication extends Application {
    private static final String TAG = LighthouseApplication.class.getSimpleName();

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

    private final MediaPlayerObservable mediaPlayerObservable = new MediaPlayerObservable();

    private final Handler progressHandler = new Handler();

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (getMediaPlayer().isPlaying()) {
                int pos = getMediaPlayer().getCurrentPosition();
                int newProgress = (int) ((long) pos * 100 / getMediaPlayer().getDuration());
                if (newProgress != notificationProgress) {
                    notificationManager.updateNotification();
                    notificationProgress = newProgress;
                }
                progressHandler.postDelayed(progressRunnable, 1000 - (pos % 1000));
            }
        }
    };

    private int notificationProgress = -1;

    private Typeface fontBold;
    private Typeface fontNormal;
    private Typeface fontLight;

    private MediaPlayer mediaPlayer;
    private WifiManager.WifiLock wifiLock;

    private MediaNotificationManager notificationManager;

    private LighthouseTrack track;

    private int bufferPercentage;

    private boolean isPreparing;
    private boolean isError;

    @Override
    public void onCreate() {
        super.onCreate();

        fontBold = Typeface.createFromAsset(getAssets(), "fonts/russia-bold.ttf");
        fontNormal = Typeface.createFromAsset(getAssets(), "fonts/russia-normal.ttf");
        fontLight = Typeface.createFromAsset(getAssets(), "fonts/russia-light.ttf");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPreparing = false;
                acquireWifiLockIfNotHeld();
                notificationProgress = -1;
                notificationManager.startNotification();
                mediaPlayerObservable.notifyPrepared();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                isPreparing = false;
                isError = true;
                releaseWifiLockIfHeld();
                notificationManager.stopNotification();
                mediaPlayerObservable.notifyFailed();
                return true;
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPreparing = false;
                releaseWifiLockIfHeld();
                notificationManager.stopNotification();
                mediaPlayerObservable.notifyCompleted();
            }
        });
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (bufferPercentage == percent) {
                    return;
                }
                bufferPercentage = percent;
                if (bufferPercentage == 100) {
                    releaseWifiLockIfHeld();
                }
            }
        });
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                notificationManager.updateNotification();
            }
        });

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);

        notificationManager = new MediaNotificationManager(this);
    }

    @Override
    public void onTerminate() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        releaseWifiLockIfHeld();
        notificationManager.stopNotification();
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public LighthouseTrack getTrack() {
        return track;
    }

    public void setTrack(LighthouseTrack track) throws IOException {
        resetTrack();

        String url = track.getRecord().getUrl();

        mediaPlayer.setDataSource(url);
        this.track = track;

        isPreparing = true;
        mediaPlayer.prepareAsync();
    }

    public void resetTrack() {
        releaseWifiLockIfHeld();
        notificationManager.stopNotification();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        this.track = null;
        bufferPercentage = 0;
        isPreparing = false;
        isError = false;
    }

    private void releaseWifiLockIfHeld() {
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void acquireWifiLockIfNotHeld() {
        if (!wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    public int getBufferPercentage() {
        return bufferPercentage;
    }

    public void registerObserver(MediaPlayerObserver observer) {
        mediaPlayerObservable.registerObserver(observer);
    }

    public void unregisterObserver(MediaPlayerObserver observer) {
        mediaPlayerObservable.unregisterObserver(observer);
    }

    public boolean containsObserver(MediaPlayerObserver observer) {
        return mediaPlayerObservable.containsObserver(observer);
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

    public boolean isPreparing() {
        return isPreparing;
    }

    public boolean isError() {
        return isError;
    }

    public void play() {
        if (isPreparing) {
            return;
        }
        getMediaPlayer().start();
        progressRunnable.run();
        acquireWifiLockIfNotHeld();
        if (!notificationManager.startNotification()) {
            notificationManager.updateNotification();
        }
    }

    public void pause() {
        getMediaPlayer().pause();
        notificationManager.updateNotification();
    }

    public LoaderManager<BitmapInfo> getImageLoaderManager() {
        return imageLoaderManager;
    }
}
