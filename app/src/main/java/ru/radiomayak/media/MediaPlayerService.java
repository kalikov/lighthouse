package ru.radiomayak.media;

import android.content.Context;
import android.media.AudioManager;
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

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.util.List;
import java.util.Objects;

import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.Record;

public class MediaPlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = MediaPlayerService.class.getSimpleName();

    private final Handler progressHandler = new Handler();

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            int state = mediaPlayer.getPlaybackState();
            if (state == Player.STATE_READY) {
                long pos = mediaPlayer.getCurrentPosition();
                float speed = mediaPlayer.getPlaybackParameters().speed;

                if (!notificationManager.startNotification()) {
                    notificationManager.updateNotification();
                }
                stateBuilder.setBufferedPosition(mediaPlayer.getBufferedPercentage());
                int sessionState = mediaPlayer.getPlayWhenReady() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
                mediaSession.setPlaybackState(stateBuilder.setState(sessionState, pos, speed).build());
                if (mediaPlayer.getPlayWhenReady() || mediaPlayer.isLoading()) {
                    progressHandler.postDelayed(progressRunnable, 1000 - (pos % 1000));
                }
            }
        }
    };

    private ExoPlayer mediaPlayer;

    private WifiManager.WifiLock wifiLock;

    private MediaNotificationManager notificationManager;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaMetadataCompat metadata;

    private LighthouseTrack track;

    private boolean restorePlay;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector());
        mediaPlayer.setPlayWhenReady(true);
        mediaPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                updateSessionActiveState();
                updateMetadata();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_NONE, 0, 0).build());
                        break;
                    case Player.STATE_BUFFERING:
                        updateSessionActiveState();
                        updateMetadata();

                        long pos = mediaPlayer.getCurrentPosition();
                        float speed = mediaPlayer.getPlaybackParameters().speed;
                        stateBuilder.setBufferedPosition(mediaPlayer.getBufferedPercentage());
                        mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_BUFFERING, pos, speed).build());
                        break;
                    case Player.STATE_READY:
                        updateSessionActiveState();
                        updateMetadata();
                        progressRunnable.run();
                        break;
                    case Player.STATE_ENDED:
                        stop(PlaybackStateCompat.STATE_STOPPED);
                        break;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                mediaSession.setActive(false);
                stop(PlaybackStateCompat.STATE_ERROR);
            }

            @Override
            public void onPositionDiscontinuity() {
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters parameters) {
            }
        });
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        Objects.requireNonNull(audioManager, "Failed to obtain AudioManager");
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Objects.requireNonNull(wifiManager, "Failed to obtain WifiManager");
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
                acquireWifiLockIfNotHeld();
                stateBuilder.setExtras(extras);
                DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("ru.radiomayak");

                setPlayWhenReady(true);
                mediaPlayer.prepare(new ExtractorMediaSource(uri, dataSourceFactory, new DefaultExtractorsFactory(), null, null));
                mediaPlayer.seekToDefaultPosition();
            }

            @Override
            public void onSeekTo(long pos) {
                mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_REWINDING, pos, 0).build());
                progressHandler.removeMessages(0);
                mediaPlayer.seekTo(pos);
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

    private void updateSessionActiveState() {
        boolean isActive = mediaPlayer.getPlaybackState() != Player.STATE_IDLE;
        if (mediaSession.isActive() != isActive) {
            mediaSession.setActive(isActive);
        }
    }

    private void updateMetadata() {
        if (metadata == null) {
            long duration = mediaPlayer.getDuration();
            if (duration > 0) {
                metadata = new MediaMetadataCompat.Builder().putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration).build();
                mediaSession.setMetadata(metadata);
            }
        }
    }

    private void setPlayWhenReady(boolean playWhenReady) {
        mediaPlayer.setPlayWhenReady(playWhenReady);
    }

    public long getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return mediaPlayer.getDuration();
    }

    public boolean isPlaying() {
        return mediaPlayer.getPlayWhenReady();
    }

    public void play() {
        setPlayWhenReady(true);
        if (mediaPlayer.getPlaybackState() == Player.STATE_ENDED) {
            mediaPlayer.seekToDefaultPosition();
        }
        acquireWifiLockIfNotHeld();
        if (!notificationManager.startNotification()) {
            notificationManager.updateNotification();
        }
    }

    public void pause() {
        setPlayWhenReady(false);
        notificationManager.updateNotification();
    }

    public void stop() {
        stop(PlaybackStateCompat.STATE_STOPPED);
    }

    private void stop(int state) {
        releaseWifiLockIfHeld();
        notificationManager.stopNotification();
        if (mediaPlayer.getPlaybackState() != Player.STATE_IDLE && mediaPlayer.getPlaybackState() != Player.STATE_ENDED) {
            mediaPlayer.stop();
        }
        stopSelf();
        restorePlay = false;
        stateBuilder.setBufferedPosition(0);
        mediaSession.setPlaybackState(stateBuilder.setState(state, 0, mediaPlayer.getPlaybackParameters().speed).build());
    }

    public LighthouseTrack getTrack() {
        return track;
    }

    public void resetTrack() {
        stateBuilder.setExtras(null);
        stateBuilder.setBufferedPosition(0);

        mediaSession.setActive(false);
        mediaSession.setMetadata(null);
        metadata = null;

        stop(PlaybackStateCompat.STATE_NONE);
        this.track = null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange <= 0 && mediaPlayer.getPlayWhenReady()) {
            restorePlay = true;
            pause();
        } else if (focusChange > 0 && restorePlay) {
            restorePlay = false;
            play();
        }
    }
}