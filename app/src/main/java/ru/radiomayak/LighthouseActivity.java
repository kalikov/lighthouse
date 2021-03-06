package ru.radiomayak;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import ru.radiomayak.animation.Interpolators;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.PodcastsUtils;
import ru.radiomayak.podcasts.Record;

public abstract class LighthouseActivity extends AppCompatActivity {
    static final String ACTION_UPDATE = LighthouseActivity.class.getPackage().getName() + ".MEDIA_UPDATE";

    public static final String ACTION_POSITION = "ru.radiomayak.podcasts.action.POSITION";

    public static final String EXTRA_PODCAST = "ru.radiomayak.podcasts.extra.PODCAST";
    public static final String EXTRA_RECORD = "ru.radiomayak.podcasts.extra.RECORD";
    public static final String EXTRA_STATE = "ru.radiomayak.podcasts.extra.STATE";
    public static final String EXTRA_POSITION = "ru.radiomayak.podcasts.extra.POSITION";
    public static final String EXTRA_DURATION = "ru.radiomayak.podcasts.extra.DURATION";

    private static final String ZERO_TIME_TEXT = "00:00";

    private static final long ANIMATION_DURATION = 300;
    private static final long SEEK_INTERVAL = 2000;

    protected final String TAG = getClass().getSimpleName();

    private final ValueAnimator.AnimatorUpdateListener openUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            playerHeight = (int) animation.getAnimatedValue();
            getPlayerContainerView().getLayoutParams().height = playerHeight;
            if (playerHeight > 0) {
                getPlayerView().setVisibility(View.VISIBLE);
                getPlayerContainerView().setVisibility(View.VISIBLE);
            }
            getPlayerContainerView().requestLayout();
        }
    };

    private final ValueAnimator.AnimatorUpdateListener closeUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            playerHeight = (int) animation.getAnimatedValue();
            getPlayerContainerView().getLayoutParams().height = playerHeight;
            if (playerHeight <= 0) {
                getPlayerView().setVisibility(View.GONE);
                getPlayerContainerView().setVisibility(View.GONE);
            }
            getPlayerContainerView().requestLayout();
        }
    };

    private final ValueAnimator.AnimatorUpdateListener seekUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            seekAlpha = (int) animation.getAnimatedValue();
            updateSeekAlphaView();
        }
    };

    private final Runnable seekTimeout = new Runnable() {
        @Override
        public void run() {
            if (seekable) {
                if (isTracking) {
                    delaySeekTimeout();
                }
                long now = System.currentTimeMillis();
                if (now >= seekTimeoutMillis) {
                    setPlayerSeekable(false);
                } else {
                    getSeekBar().postDelayed(seekTimeout, seekTimeoutMillis - now);
                }
            }
        }
    };

    private MediaControllerCompat.Callback controllerCallback;

    private AnimatedVectorDrawableCompat forwardingDrawable;
    private AnimatedVectorDrawableCompat rewindingDrawable;

    private boolean isTracking;
    private boolean isSeeking;

    private int playerMaxHeight;
    private int playerHeight;
    private ValueAnimator valueAnimator;

    private ValueAnimator seekAnimator;
    private int seekAlpha = 0;
    private boolean seekable = false;
    private long seekTimeoutMillis;

    private PlaybackStateCompat playbackState;
    private Bundle extras;
    private LighthouseTrack track;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LighthouseActivity.this.onReceive(context, intent);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        forwardingDrawable = AnimatedVectorDrawableCompat.create(getApplicationContext(), R.drawable.forwarding_animated);
        rewindingDrawable = AnimatedVectorDrawableCompat.create(getApplicationContext(), R.drawable.rewinding_animated);

        playerMaxHeight = getResources().getDimensionPixelSize(R.dimen.player_height);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE);
        filter.addAction(ACTION_POSITION);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initializePlayerView();
    }

    @Override
    protected void onResume() {
        updateMediaController();

        updatePlayerView(false);

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);

        unsetMediaController();

        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void initializePlayerView() {
        getPlayPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delaySeekTimeout();
                togglePlay();
            }
        });
        getCloseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePlayer();
            }
        });
        getSeekBar().setMax(1000);
        getSeekBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                delaySeekTimeout();
                int duration = getDuration();
                int position = (int) ((long) progress * duration / 1000);
                getSongPositionView().setText(PodcastsUtils.formatTime(position));
                seekTo(position);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
                updateThumb();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
                updateThumb();
            }
        });
        getSeekBar().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!seekable) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            setPlayerSeekable(true);
                            break;
                    }
                    return true;
                }
                return false;
            }
        });

        final GestureDetector gestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                setPlayerSeekable(true);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int width = getPlayerView().getWidth();
                delaySeekTimeout();
                if (e.getX() > 2 * width / 3) {
                    fastForward();
                } else if (e.getX() < width / 3) {
                    rewindBack();
                }
                return true;
            }
        });

        getPlayerView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        getForwardingView().setImageDrawable(forwardingDrawable);
        getRewindingView().setImageDrawable(rewindingDrawable);

        getSongNameView().setTypeface(getLighthouseApplication().getFontBold());
        getSongPositionView().setTypeface(getLighthouseApplication().getFontLight());
        getSongDurationView().setTypeface(getLighthouseApplication().getFontLight());

        updatePlayerView(false);
    }

    private void delaySeekTimeout() {
        seekTimeoutMillis = Math.max(seekTimeoutMillis, System.currentTimeMillis() + SEEK_INTERVAL);
    }

    private void resetPlayerSeekable() {
        if (seekAnimator != null) {
            seekAnimator.cancel();
            seekAnimator = null;
        }
        isTracking = false;
        seekAlpha = 0;
        seekable = false;
        seekTimeoutMillis = 0;
        updateSeekAlphaView();
    }

    private void setPlayerSeekable(boolean seekable) {
        if (seekable) {
            delaySeekTimeout();
        }
        if (this.seekable == seekable) {
            return;
        }
        this.seekable = seekable;
        if (seekAnimator != null) {
            seekAnimator.cancel();
            seekAnimator = null;
        }
        if (!seekable) {
            if (seekAlpha == 0) {
                return;
            }
            seekAnimator = ValueAnimator.ofInt(seekAlpha, 0);
        } else {
            getSeekBar().postDelayed(seekTimeout, SEEK_INTERVAL + ANIMATION_DURATION);
            if (seekAlpha == 100) {
                return;
            }
            seekAnimator = ValueAnimator.ofInt(seekAlpha, 100);
        }
        seekAnimator.setInterpolator(Interpolators.ACCELERATE);
        seekAnimator.addUpdateListener(seekUpdateListener);
        seekAnimator.setDuration(ANIMATION_DURATION);
        seekAnimator.start();
    }

    private void updateSeekAlphaView() {
        View overlayView = getOverlayView();
//        overlayView.setAlpha(seekAlpha / 100f);
        overlayView.setVisibility(seekAlpha > 0 ? View.VISIBLE : View.INVISIBLE);

//        int component = 255 * seekAlpha / 100;
//        int color = 0xFF000000 | component | component << 8 | component << 16;
//        ColorStateList colorStateList = ColorStateList.valueOf(color);
//        ImageViewCompat.setImageTintList(getPlayPauseButton(), colorStateList);
//        ImageViewCompat.setImageTintList(getCloseButton(), colorStateList);

        updateThumb();
    }

    private void updateThumb() {
        getSeekBar().getThumb().setLevel(isTracking ? 10000 : Math.max(1, seekAlpha * 70));
    }

    protected void updatePlayerView(boolean animate) {
        LighthouseTrack track = getTrack();

        final View containerView = getPlayerContainerView();
        final View playerView = getPlayerView();
        if (track == null) {
            if (valueAnimator != null) {
                valueAnimator.cancel();
                valueAnimator = null;
            }
            if (!animate || playerHeight <= 0) {
                playerHeight = 0;
                playerView.setVisibility(View.GONE);
                containerView.setVisibility(View.GONE);
                containerView.getLayoutParams().height = 0;
                containerView.requestLayout();
                return;
            }
            valueAnimator = ValueAnimator.ofInt(playerHeight, 0);
            valueAnimator.setInterpolator(Interpolators.ACCELERATE);
            valueAnimator.addUpdateListener(closeUpdateListener);
            valueAnimator.setDuration(ANIMATION_DURATION);
            valueAnimator.start();
            return;
        }
        Record record = track.getRecord();

        updatePlayPauseButton();

        getSongNameView().setText(record.getName());
        if (isError()) {
            isTracking = false;
            getSeekBar().setEnabled(false);
            getSeekBar().setProgress(0);
            getSeekBar().setSecondaryProgress(0);
            getSongPositionView().setText(ZERO_TIME_TEXT);
            getSongDurationView().setText(ZERO_TIME_TEXT);
            updateThumb();
        } else {
            int position = getCurrentPosition();
            int duration = getDuration();
            isTracking = isTracking && duration > 0;
            getSeekBar().setEnabled(duration > 0);
            getSongPositionView().setText(position <= duration ? PodcastsUtils.formatTime(position) : ZERO_TIME_TEXT);
            getSongDurationView().setText(PodcastsUtils.formatTime(duration));
            updateProgress();
            updateThumb();
        }
        if (valueAnimator != null) {
            valueAnimator.cancel();
            valueAnimator = null;
        }
        if (!animate || playerHeight > playerMaxHeight) {
            playerHeight = playerMaxHeight;
            playerView.setVisibility(View.VISIBLE);
            containerView.setVisibility(View.VISIBLE);
            containerView.getLayoutParams().height = playerMaxHeight;
            containerView.requestLayout();
            return;
        }
        valueAnimator = ValueAnimator.ofInt(playerHeight, playerMaxHeight);
        valueAnimator.setInterpolator(Interpolators.DECELERATE);
        valueAnimator.addUpdateListener(openUpdateListener);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.start();
    }

    private void updatePlayPauseButton() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController == null || isPreparing()) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotation);
            animation.setInterpolator(Interpolators.LINEAR);
            getPlayPauseButton().setImageResource(R.drawable.player_progress);
            getPlayPauseButton().setContentDescription(getString(R.string.loading));
            getPlayPauseButton().startAnimation(animation);
        } else {
            getPlayPauseButton().clearAnimation();
            getPlayPauseButton().setImageResource(isPlaying() ? R.drawable.pause_selector : R.drawable.play_selector);
            getPlayPauseButton().setContentDescription(getString(isPlaying() ? R.string.pause : R.string.play));
        }
    }

    public final LighthouseApplication getLighthouseApplication() {
        return ((LighthouseApplication) getApplication());
    }

    private void updateTrackFromExtras() {
        if (extras != null) {
            extras.setClassLoader(Record.class.getClassLoader());
            Record record = extras.getParcelable(Record.class.getName());
            Podcast podcast = extras.getParcelable(Podcast.class.getName());
            if (record != null && podcast != null) {
                track = new LighthouseTrack(podcast, record);
                return;
            }
        }
        track = null;
        resetPlayerSeekable();
    }

    public LighthouseTrack getTrack() {
        return track;
    }

    public void setTrack(LighthouseTrack track) {
        Record record = track.getRecord();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Record.class.getName(), record);
        bundle.putParcelable(Podcast.class.getName(), track.getPodcast());
        extras = bundle;
        this.track = track;

        Uri uri = Uri.parse(record.getUrl());

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        mediaController.getTransportControls().playFromUri(uri, bundle);
    }

    public int getCurrentPosition() {
        return playbackState != null ? (int) playbackState.getPosition() : 0;
    }

    public int getDuration() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController == null) {
            return 0;
        }
        return mediaController.getMetadata() != null ? (int) mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION) : 0;
    }

    public int getBufferPercentage() {
        return playbackState != null ? (int) playbackState.getBufferedPosition() : 0;
    }

    public boolean isPreparing() {
        return playbackState != null && (playbackState.getState() == PlaybackStateCompat.STATE_CONNECTING
                || playbackState.getState() == PlaybackStateCompat.STATE_BUFFERING);
    }

    public boolean isRewinding() {
        return isSeeking || playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_REWINDING;
    }

    public boolean isPlaying() {
        return playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    public boolean isError() {
        return playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_ERROR;
    }

    public void pause() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController != null) {
            mediaController.getTransportControls().pause();
        }
    }

    public void play() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController != null) {
            mediaController.getTransportControls().play();
        }
    }

    public void seekTo(int position) {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController != null) {
            isSeeking = true;
            mediaController.getTransportControls().seekTo(position);
        }
    }

    public ImageView getForwardingView() {
        return getPlayerView().findViewById(R.id.forwarding);
    }

    public ImageView getRewindingView() {
        return getPlayerView().findViewById(R.id.rewinding);
    }

    public View getPlayerView() {
        return findViewById(R.id.player);
    }

    public View getPlayerContainerView() {
        return findViewById(R.id.player_container);
    }

    public TextView getSongPositionView() {
        return (TextView) getPlayerView().findViewById(R.id.offset);
    }

    public TextView getSongDurationView() {
        return (TextView) getPlayerView().findViewById(R.id.duration);
    }

    public TextView getSongNameView() {
        return (TextView) getPlayerView().findViewById(R.id.name);
    }

    public SeekBar getSeekBar() {
        return (SeekBar) getPlayerView().findViewById(R.id.seekbar);
    }

    public ImageView getPlayPauseButton() {
        return (ImageView) getPlayerView().findViewById(android.R.id.toggle);
    }

    public ImageView getCloseButton() {
        return (ImageView) getPlayerView().findViewById(android.R.id.closeButton);
    }

    public View getOverlayView() {
        return getPlayerView().findViewById(R.id.overlay);
    }

    private void fastForward() {
        int duration = getDuration();
        if (duration > 0) {
            seekTo(Math.min(duration, getCurrentPosition() + 10000));
            rewindingDrawable.stop();
            getRewindingView().setVisibility(View.GONE);
            forwardingDrawable.stop();
            forwardingDrawable.start();
            getForwardingView().setVisibility(View.VISIBLE);
        }
    }

    private void rewindBack() {
        if (getDuration() > 0) {
            seekTo(Math.max(0, getCurrentPosition() - 10000));
            forwardingDrawable.stop();
            getForwardingView().setVisibility(View.GONE);
            rewindingDrawable.stop();
            rewindingDrawable.start();
            getRewindingView().setVisibility(View.VISIBLE);
        }
    }

    private void togglePlay() {
        if (isPlaying()) {
            pause();
        } else if (isError()) {
            try {
                setTrack(getTrack());
            } catch (RuntimeException e) {
                Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            play();
        }
    }

    private void closePlayer() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController != null) {
            mediaController.getTransportControls().sendCustomAction("reset", null);
        }
    }

    protected int updateProgress() {
        if (isTracking || isRewinding()) {
            return 0;
        }
        int position = getCurrentPosition();
        int duration = getDuration();
        if (duration > 0) {
            long progress = (long) position * 1000 / duration;
            getSeekBar().setProgress((int) progress);
        } else {
            getSeekBar().setProgress(0);
        }
        int percent = getBufferPercentage();
        getSeekBar().setSecondaryProgress(percent * 10);

        return position;
    }

    protected void updateRecordPlaybackAttributes(int state, long podcast, long record, long position, long duration) {
        if (track != null && track.getPodcast().getId() == podcast && track.getRecord().getId() == record) {
            track.getRecord().setPosition((int) position);
            track.getRecord().setLength((int) duration);
        }
    }

    private void onFailed() {
        updatePlayerView(false);
        Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();
    }

    private void unsetMediaController() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController != null) {
            mediaController.unregisterCallback(controllerCallback);
            controllerCallback = null;
            MediaControllerCompat.setMediaController(this, null);
        }
    }

    private void updateMediaController() {
        MediaBrowserCompat mediaBrowser = getLighthouseApplication().getMediaBrowser();
        if (!mediaBrowser.isConnected()) {
            unsetMediaController();
            return;
        }
        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController == null) {
            try {
                mediaController = new MediaControllerCompat(this, token);
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage(), e);
                return;
            }
            MediaControllerCompat.setMediaController(this, mediaController);
            if (controllerCallback == null) {
                controllerCallback = new MediaControllerCallback();
            }
            mediaController.registerCallback(controllerCallback);
        }
        playbackState = mediaController.getPlaybackState();
        extras = playbackState.getExtras();
        updateTrackFromExtras();
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_UPDATE.equals(action)) {
            updateMediaController();

            updatePlayerView(true);
        } else if (ACTION_POSITION.equals(action)) {
            long podcast = intent.getLongExtra(EXTRA_PODCAST, 0);
            long record = intent.getLongExtra(EXTRA_RECORD, 0);
            int state = intent.getIntExtra(EXTRA_STATE, PlaybackStateCompat.STATE_NONE);
            long position = intent.getLongExtra(EXTRA_POSITION, 0);
            long duration = intent.getLongExtra(EXTRA_DURATION, 0);

            updateRecordPlaybackAttributes(state, podcast, record, position, duration);
        }
    }

    public abstract boolean isNavigateBackSupported();

    public abstract void openPodcast(Podcast podcast);

    public abstract void onPodcastSeen(long podcast, int seen);

    public abstract void onHistoryTrackRemoved(long podcast, long record);

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            PlaybackStateCompat previousState = playbackState;
            playbackState = state;
            if (extras != state.getExtras()) {
                extras = state.getExtras();
                updateTrackFromExtras();
            }
            if (state.getState() == PlaybackStateCompat.STATE_ERROR) {
                isSeeking = false;
                if (previousState == null || previousState.getState() != PlaybackStateCompat.STATE_ERROR) {
                    onFailed();
                }
            } else if (state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_PAUSED) {
                isSeeking = false;
                updatePlayerView(false);
            } else if (state.getState() != PlaybackStateCompat.STATE_REWINDING) {
                isSeeking = false;
                updatePlayerView(true);
            }
        }
    }
}
