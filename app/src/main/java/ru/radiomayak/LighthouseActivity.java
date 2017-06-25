package ru.radiomayak;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

import ru.radiomayak.media.MediaPlaybackService;
import ru.radiomayak.media.MediaPlayerObserver;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.Record;

public class LighthouseActivity extends AppCompatActivity implements MediaPlayerObserver {
    private static final String ZERO_TIME_TEXT = "00:00";

    private MediaBrowserCompat mMediaBrowser;

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {

                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

                    // Create a MediaControllerCompat
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(LighthouseActivity.this, token);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Save the controller
                    MediaControllerCompat.setMediaController(LighthouseActivity.this, mediaController);

                    mediaController.registerCallback(controllerCallback);
                    // Finish building the UI
//                    buildTransportControls();
                    updatePlayerView();
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }

                void buildTransportControls()
                {
                    // Grab the view for the play/pause button
//                    mPlayPause = (ImageView) findViewById(R.id.play_pause);

                    // Attach a listener to the button
                    /*getPlayPauseButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Since this is a play/pause button, you'll need to test the current state
                            // and choose the action accordingly

                            int pbState = MediaControllerCompat.getMediaController(LighthouseActivity.this).getPlaybackState().getState();
                            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                                MediaControllerCompat.getMediaController(LighthouseActivity.this).getTransportControls().pause();
                            } else {
                                MediaControllerCompat.getMediaController(LighthouseActivity.this).getTransportControls().play();
                            }
                        }
                    });

                    MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(LighthouseActivity.this);

                    // Display the initial state
                    MediaMetadataCompat metadata = mediaController.getMetadata();
                    PlaybackStateCompat pbState = mediaController.getPlaybackState();*/

                    // Register a Callback to stay in sync
//                    mediaController.registerCallback(controllerCallback);
                }

            };

    MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {

                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    updatePlayerView();
                }
            };

    private boolean isTracking;

//    private final Runnable showProgressRunnable = new Runnable() {
//        @Override
//        public void run() {
//            long pos = setProgress();
//            if (!isTracking && isPlaying()) {
//                getPlayerView().postDelayed(showProgressRunnable, 1000 - (pos % 1000));
//            } else if (!isTracking) {
//                updatePlayPauseButton();
//            }
//        }
//    };

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                mConnectionCallbacks,
                null); // optional Bundle
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback);
        }
        mMediaBrowser.disconnect();

    }

    @Override
    protected void onDestroy() {
//        if (getLighthouseApplication().containsObserver(this)) {
//            getLighthouseApplication().unregisterObserver(this);
//        }
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
                MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(LighthouseActivity.this);
                long duration = mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                long position = progress * duration / 1000;
//                getMediaPlayer().seekTo((int) position);
                getSongPositionView().setText(formatTime((int) position));
                mediaController.getTransportControls().seekTo(position);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
//                setProgress();
//                getPlayerView().post(showProgressRunnable);
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
//
//        MediaPlayer player = getMediaPlayer();
//
        updatePlayPauseButton();
//
//        if ((isPreparing() || getMediaPlayer().isPlaying()) && !getLighthouseApplication().containsObserver(this)) {
//            getLighthouseApplication().registerObserver(this);
//        }
//
        getSongNameView().setText(record.getName());
        if (isPreparing()) {
            getSeekBar().setEnabled(false);
            getSeekBar().setProgress(0);
            getSeekBar().setSecondaryProgress(0);
            getSongPositionView().setText(ZERO_TIME_TEXT);
            getSongDurationView().setText(ZERO_TIME_TEXT);
        } else {
            getSeekBar().setEnabled(true);
            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
            getSongPositionView().setText(formatTime(mediaController.getPlaybackState().getPosition()));
            getSongDurationView().setText(formatTime(mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
            setProgress();

//            playerView.post(showProgressRunnable);
        }
        playerView.setVisibility(View.VISIBLE);
    }

    private void updatePlayPauseButton() {
        if (isPreparing()) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotation);
            animation.setInterpolator(new LinearInterpolator());
            getPlayPauseButton().setImageResource(R.drawable.progress);
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

//    public MediaPlayer getMediaPlayer() {
//        return getLighthouseApplication().getMediaPlayer();
//    }

    public LighthouseTrack getTrack() {
//        return getLighthouseApplication().getTrack();
        if (!mMediaBrowser.isConnected()) {
            return null;
        }
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController == null) {
            return null;
        }
        Bundle bundle = mediaController.getExtras();
        if (bundle == null) {
            return null;
        }
        Record record = bundle.getParcelable("record");
        Podcast podcast = bundle.getParcelable("podcast");
        if (record == null || podcast == null) {
            return null;
        }
        return new LighthouseTrack(podcast, record);
    }

    public void setTrack(LighthouseTrack track) throws IOException {
        Record record = track.getRecord();
        Bundle bundle = new Bundle();
        bundle.putParcelable("record", record);
        bundle.putParcelable("podcast", track.getPodcast());
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        mediaController.getTransportControls().playFromUri(Uri.parse(record.getUrl()), bundle);
//        getLighthouseApplication().resetTrack();
//        if (!getLighthouseApplication().containsObserver(this)) {
//            getLighthouseApplication().registerObserver(this);
//        }
//        getLighthouseApplication().setTrack(track);
    }

    public void resetTrack() {
//        getLighthouseApplication().resetTrack();
//        if (getLighthouseApplication().containsObserver(this)) {
//            getLighthouseApplication().unregisterObserver(this);
//        }
    }

//    public int getBufferPercentage() {
//        return getLighthouseApplication().getBufferPercentage();
//    }

    public boolean isPreparing() {
//        return getLighthouseApplication().isPreparing();
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        return mediaController != null && mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_CONNECTING;
    }

    public boolean isPlaying() {
//        return getLighthouseApplication().isPreparing();
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        return mediaController != null && mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
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

    private static String formatTime(long time) {
        if (time <= 0) {
            return ZERO_TIME_TEXT;
        }
        long totalSecs = time / 1000;
        long secs = totalSecs % 60;
        long mins = (totalSecs / 60) % 60;
        long hours = totalSecs / 3600;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, mins, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d", mins, secs);
    }

    private void togglePlayer() {
//        MediaPlayer player = getMediaPlayer();
//        if (player.isPlaying()) {
//            player.pause();
//        } else {
//            player.start();
//        }
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
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
//        MediaPlayer player = getMediaPlayer();
        long position = mediaController.getPlaybackState().getPosition();// player.getCurrentPosition();
        long duration = mediaController.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);//player.getDuration();
        if (duration > 0) {
            long progress = position * 1000 / duration;
            getSeekBar().setProgress((int) progress);
        } else {
            getSeekBar().setProgress(0);
        }
        int percent = (int)mediaController.getPlaybackState().getBufferedPosition();//getBufferPercentage();
//        Record record = getRecord();
//        if (record != null && getLighthouseApplication().getMediaProxyServer().getCacheSize(String.valueOf(record.getId())) == 100) {
//            getSeekBar().setSecondaryProgress(1000);
//        } else {
        getSeekBar().setSecondaryProgress(percent * 10);
//        }

        getSongDurationView().setText(formatTime(duration));
        getSongPositionView().setText(formatTime(position));

        return position;
    }

    @Override
    public void onPrepared() {
        /*getMediaPlayer().start();
        updatePlayerView();

        LighthouseTrack track = getTrack();
        if (track != null && !track.getRecord().isPlayed()) {
            track.getRecord().setPlayed(true);
            PodcastsUtils.storeRecordPlayedProperty(this, track.getPodcast().getId(), track.getRecord());
        }*/
    }

    @Override
    public void onFailed() {
        /*getLighthouseApplication().unregisterObserver(this);
        updatePlayerView();
        Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();*/
    }

    @Override
    public void onCompleted() {
       /* getLighthouseApplication().unregisterObserver(this);
        updatePlayerView();*/
    }
}
