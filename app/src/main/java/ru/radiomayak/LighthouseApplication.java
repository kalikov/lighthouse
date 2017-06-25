package ru.radiomayak;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.media.MediaPlayer;
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

import ru.radiomayak.media.MediaPlayerObservable;
import ru.radiomayak.media.MediaPlayerObserver;

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

    private final MediaPlayerObservable mediaPlayerObservable = new MediaPlayerObservable();

    private Typeface fontBold;
    private Typeface fontNormal;
    private Typeface fontLight;

//    private MediaPlayer mediaPlayer;

    private LighthouseTrack track;

    private int bufferPercentage;

    private boolean isPreparing;

//    private MediaProxyServer proxyServer;

    @Override
    public void onCreate() {
        super.onCreate();

        fontBold = Typeface.createFromAsset(getAssets(), "fonts/russia-bold.ttf");
        fontNormal = Typeface.createFromAsset(getAssets(), "fonts/russia-normal.ttf");
        fontLight = Typeface.createFromAsset(getAssets(), "fonts/russia-light.ttf");

//        mediaPlayer = new MediaPlayer();
//        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                isPreparing = false;
//                mediaPlayerObservable.notifyPrepared();
//            }
//        });
//        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//            @Override
//            public boolean onError(MediaPlayer mp, int what, int extra) {
//                isPreparing = false;
//                mediaPlayerObservable.notifyFailed();
//                return true;
//            }
//        });
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                isPreparing = false;
//                mediaPlayerObservable.notifyCompleted();
//            }
//        });
//        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
//            @Override
//            public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                bufferPercentage = percent;
//            }
//        });
//        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
//            @Override
//            public void onSeekComplete(MediaPlayer mp) {
//            }
//        });

//        proxyServer = new MediaProxyServer(getExternalFilesDir(Environment.DIRECTORY_PODCASTS));
//        try {
//            proxyServer.start();
//        } catch (IOException ignored) {
//        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

//        if (proxyServer != null) {
//            try {
//                proxyServer.stop();
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            } finally {
//                proxyServer = null;
//            }
//        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

//    public MediaPlayer getMediaPlayer() {
//        return mediaPlayer;
//    }

    public LighthouseTrack getTrack() {
        return track;
    }

    /*public void setTrack(LighthouseTrack track) throws IOException {
        resetTrack();

        String url = track.getRecord().getUrl();

//        InetAddress address = proxyServer.getInetAddress();
//        int port = proxyServer.getPort();
//
//        String src = "http://" + address.getHostAddress() + ":" + port + "?url=" + URLEncoder.encode(url, "UTF-8") + "&name=" + record.getId();
//        mediaPlayer.setDataSource(src);

        mediaPlayer.setDataSource(url);
        this.track = track;

        isPreparing = true;
        mediaPlayer.prepareAsync();
    }

    public void resetTrack() {
        mediaPlayer.reset();
        this.track = null;
        bufferPercentage = 0;
    }*/
//
//    public int getBufferPercentage() {
//        return bufferPercentage;
//    }
//
//    public void registerObserver(MediaPlayerObserver observer) {
//        mediaPlayerObservable.registerObserver(observer);
//    }
//
//    public void unregisterObserver(MediaPlayerObserver observer) {
//        mediaPlayerObservable.unregisterObserver(observer);
//    }
//
//    public boolean containsObserver(MediaPlayerObserver observer) {
//        return mediaPlayerObservable.containsObserver(observer);
//    }

    public Typeface getFontBold() {
        return fontBold;
    }

    public Typeface getFontNormal() {
        return fontNormal;
    }

    public Typeface getFontLight() {
        return fontLight;
    }

//    public boolean isPreparing() {
//        return isPreparing;
//    }
}
