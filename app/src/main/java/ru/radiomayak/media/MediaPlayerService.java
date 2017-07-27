package ru.radiomayak.media;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;
import java.util.List;

import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.Record;

public class MediaPlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = MediaPlayerService.class.getSimpleName();

    private final Handler progressHandler = new Handler();

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer.isPlaying()) {
                int pos = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                if (!seeking && duration > 0) {
                    notificationManager.updateNotification();
                    mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, pos, 1).build());
                }
                progressHandler.postDelayed(progressRunnable, 1000 - (pos % 1000));
            }
        }
    };

    private MediaPlayer mediaPlayer;
    private WifiManager.WifiLock wifiLock;

    private MediaNotificationManager notificationManager;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private LighthouseTrack track;

    private boolean restorePlay;
    private boolean seeking;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaSession.setActive(true);
                startService(new Intent(getApplicationContext(), MediaPlayerService.class));
                play();
                mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp.getDuration())
                        .build());
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mediaSession.setActive(false);
                stop(PlaybackStateCompat.STATE_ERROR);
                return true;
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stop(PlaybackStateCompat.STATE_STOPPED);
            }
        });
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent == 100) {
                    releaseWifiLockIfHeld();
                }
                if (!seeking) {
                    mediaSession.setPlaybackState(stateBuilder.setBufferedPosition(percent).build());
                }
            }
        });
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                seeking = false;
                if (mp.isPlaying()) {
                    progressRunnable.run();
                } else {
                    mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, mp.getCurrentPosition(), 0).build());
                }
            }
        });

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);

        notificationManager = new MediaNotificationManager(this);

        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        stateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlayFromUri(Uri uri, Bundle extras) {
                resetTrack();
                if (extras == null) {
                    return;
                }
                extras.setClassLoader(Record.class.getClassLoader());
                Record record = extras.getParcelable(Record.class.getName());
                Podcast podcast = extras.getParcelable(Podcast.class.getName());
                if (record == null || podcast == null) {
                    return;
                }
                track = new LighthouseTrack(podcast, record);
                stateBuilder.setExtras(extras);
                try {
                    mediaPlayer.setDataSource(MediaPlayerService.this, uri);
                    mediaPlayer.prepareAsync();
                    mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_CONNECTING, 0, 0).build());
                } catch (IOException e) {
                    e.printStackTrace();
                    mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_ERROR, 0, 0).build());
                }
            }

            @Override
            public void onSeekTo(long pos) {
                seeking = true;
                mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_REWINDING, pos, 0).build());
                progressHandler.removeMessages(0);
                mediaPlayer.seekTo((int) pos);
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onStop() {
                stop();
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                if ("reset".equals(action)) {
                    resetTrack();
                }
            }
        });
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        releaseWifiLockIfHeld();
        notificationManager.stopNotification();
        super.onDestroy();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentMediaId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    MediaPlayer getMediaPlayer() {
        return mediaPlayer;
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

    public void play() {
        if (!mediaSession.isActive()) {
            return;
        }
        getMediaPlayer().start();
        progressRunnable.run();
        acquireWifiLockIfNotHeld();
        if (!notificationManager.startNotification()) {
            notificationManager.updateNotification();
        }
        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, getMediaPlayer().getCurrentPosition(), 1).build());
    }

    public void pause() {
        getMediaPlayer().pause();
        notificationManager.updateNotification();
        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, getMediaPlayer().getCurrentPosition(), 0).build());
    }

    public void stop() {
        stop(PlaybackStateCompat.STATE_STOPPED);
    }

    private void stop(int state) {
        releaseWifiLockIfHeld();
        notificationManager.stopNotification();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        stopSelf();
        restorePlay = false;
        mediaSession.setPlaybackState(stateBuilder.setState(state, 0, 0).build());
    }

    public LighthouseTrack getTrack() {
        return track;
    }

    public void resetTrack() {
        stateBuilder.setExtras(null);
        mediaSession.setActive(false);
        mediaSession.setMetadata(null);
        stop(PlaybackStateCompat.STATE_NONE);

        mediaPlayer.reset();
        this.track = null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange <= 0) {
            if (getMediaPlayer().isPlaying()) {
                pause();
                restorePlay = true;
            }
        } else if (restorePlay) {
            restorePlay = false;
            play();
        }
    }
}