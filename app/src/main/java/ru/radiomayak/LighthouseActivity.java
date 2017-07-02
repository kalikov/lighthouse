package ru.radiomayak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

import ru.radiomayak.media.MediaNotificationManager;
import ru.radiomayak.media.MediaPlayerObserver;
import ru.radiomayak.podcasts.PodcastsUtils;
import ru.radiomayak.podcasts.Record;

public class LighthouseActivity extends AppCompatActivity implements MediaPlayerObserver {
    private static final String ZERO_TIME_TEXT = "00:00";

    private boolean isTracking;

    private final Runnable showProgressRunnable = new Runnable() {
        @Override
        public void run() {
            long pos = setProgress();
            if (!isTracking && isPlaying()) {
                getPlayerView().postDelayed(showProgressRunnable, 1000 - (pos % 1000));
            } else if (!isTracking) {
                updatePlayPauseButton();
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LighthouseActivity.this.onReceive(context, intent);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaNotificationManager.ACTION_PAUSE);
        filter.addAction(MediaNotificationManager.ACTION_PLAY);
        filter.addAction(MediaNotificationManager.ACTION_STOP);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (getLighthouseApplication().containsObserver(this)) {
            getLighthouseApplication().unregisterObserver(this);
        }
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    protected void initializePlayerView() {
        getPlayPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayer();
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
                int duration = getMediaPlayer().getDuration();
                int position = (int) ((long) progress * duration / 1000);
                getSongPositionView().setText(formatTime(position));
                getMediaPlayer().seekTo(position);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
                setProgress();
                getPlayerView().post(showProgressRunnable);
            }
        });

        getPlayerView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return true;
            }
        });

        getSongNameView().setTypeface(getLighthouseApplication().getFontBold());
        getSongPositionView().setTypeface(getLighthouseApplication().getFontLight());
        getSongDurationView().setTypeface(getLighthouseApplication().getFontLight());

        updatePlayerView();
    }

    protected void updatePlayerView() {
        LighthouseTrack track = getTrack();

        View playerView = getPlayerView();
        if (track == null) {
            playerView.setVisibility(View.GONE);
            return;
        }
        Record record = track.getRecord();

        updatePlayPauseButton();

        if ((isPreparing() || isPlaying()) && !getLighthouseApplication().containsObserver(this)) {
            getLighthouseApplication().registerObserver(this);
        }

        getSongNameView().setText(record.getName());
        if (isPreparing() || isError()) {
            getSeekBar().setEnabled(false);
            getSeekBar().setProgress(0);
            getSeekBar().setSecondaryProgress(0);
            getSongPositionView().setText(ZERO_TIME_TEXT);
            getSongDurationView().setText(ZERO_TIME_TEXT);
        } else {
            getSeekBar().setEnabled(true);
            getSongPositionView().setText(formatTime(getMediaPlayer().getCurrentPosition()));
            getSongDurationView().setText(formatTime(getMediaPlayer().getDuration()));
            setProgress();

            playerView.post(showProgressRunnable);
        }
        playerView.setVisibility(View.VISIBLE);
    }

    private void updatePlayPauseButton() {
        if (isPreparing()) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotation);
            animation.setInterpolator(new LinearInterpolator());
            getPlayPauseButton().setImageResource(R.drawable.player_progress);
            getPlayPauseButton().setContentDescription(getString(R.string.loading));
            getPlayPauseButton().startAnimation(animation);
        } else {
            getPlayPauseButton().clearAnimation();
            getPlayPauseButton().setImageResource(isPlaying() ? R.drawable.pause_selector : R.drawable.play_selector);
            getPlayPauseButton().setContentDescription(getString(isPlaying() ? R.string.pause : R.string.play));
        }
    }

    protected final LighthouseApplication getLighthouseApplication() {
        return ((LighthouseApplication) getApplication());
    }

    public MediaPlayer getMediaPlayer() {
        return getLighthouseApplication().getMediaPlayer();
    }

    public LighthouseTrack getTrack() {
        return getLighthouseApplication().getTrack();
    }

    public void setTrack(LighthouseTrack track) throws IOException {
        resetTrack();
        if (!getLighthouseApplication().containsObserver(this)) {
            getLighthouseApplication().registerObserver(this);
        }
        getLighthouseApplication().setTrack(track);
    }

    public void resetTrack() {
        if (getLighthouseApplication().containsObserver(this)) {
            getLighthouseApplication().unregisterObserver(this);
        }
        getLighthouseApplication().resetTrack();
    }


    public int getBufferPercentage() {
        return getLighthouseApplication().getBufferPercentage();
    }

    public boolean isPreparing() {
        return getLighthouseApplication().isPreparing();
    }

    public boolean isPlaying() {
        return getMediaPlayer().isPlaying();
    }

    public boolean isError() {
        return getLighthouseApplication().isError();
    }

    public void pause() {
        getLighthouseApplication().pause();
    }

    public void play() {
        getLighthouseApplication().play();
    }

    public View getPlayerView() {
        return findViewById(R.id.player);
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

    private static String formatTime(int time) {
        if (time <= 0) {
            return ZERO_TIME_TEXT;
        }
        int totalSecs = time / 1000;
        int secs = totalSecs % 60;
        int mins = (totalSecs / 60) % 60;
        int hours = totalSecs / 3600;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, mins, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d", mins, secs);
    }

    private void togglePlayer() {
        if (isPlaying()) {
            pause();
        } else if (isError()) {
            try {
                setTrack(getTrack());
            } catch (IOException e) {
                Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            play();
        }
        updatePlayerView();
    }

    private void closePlayer() {
        resetTrack();

        updatePlayerView();
    }

    protected long setProgress() {
        if (isTracking || isPreparing()) {
            return 0;
        }
        MediaPlayer player = getMediaPlayer();
        int position = player.getCurrentPosition();
        int duration = player.getDuration();
        if (duration > 0) {
            long progress = (long) position * 1000 / duration;
            getSeekBar().setProgress((int) progress);
        } else {
            getSeekBar().setProgress(0);
        }
        int percent = getBufferPercentage();
        getSeekBar().setSecondaryProgress(percent * 10);

        getSongDurationView().setText(formatTime(duration));
        getSongPositionView().setText(formatTime(position));

        return position;
    }

    @Override
    public void onPrepared() {
        play();
        updatePlayerView();

        LighthouseTrack track = getTrack();
        if (track != null && !track.getRecord().isPlayed()) {
            track.getRecord().setPlayed(true);
            PodcastsUtils.storeRecordPlayedProperty(this, track.getPodcast().getId(), track.getRecord());
        }
    }

    @Override
    public void onFailed() {
        getLighthouseApplication().unregisterObserver(this);
        updatePlayerView();
        Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCompleted() {
        getLighthouseApplication().unregisterObserver(this);
        updatePlayerView();
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (MediaNotificationManager.ACTION_PAUSE.equals(action)) {
            getMediaPlayer().pause();
        } else if (MediaNotificationManager.ACTION_PLAY.equals(action)) {
            getMediaPlayer().start();
        } else if (MediaNotificationManager.ACTION_STOP.equals(action)) {
            resetTrack();
        }
        updatePlayerView();
    }
}
