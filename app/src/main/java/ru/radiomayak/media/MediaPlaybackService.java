package ru.radiomayak.media;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {
    private static final String TAG = MediaPlaybackService.class.getSimpleName();

    private static final String MY_MEDIA_ROOT_ID = "1";

    private final Handler handler = new Handler();

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
//            long pos = setProgress();
            if (player.isPlaying()) {
                int pos = player.getCurrentPosition();
                handler.postDelayed(progressRunnable, 1000 - (pos % 1000));
                mMediaSession.setPlaybackState(mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, pos, 1).build());
            }
        }
    };

    private MediaPlayer player;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();


        player = new MediaPlayer();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                handler.post(progressRunnable);
                mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp.getDuration())
                        .build());
                mMediaSession.setPlaybackState(mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mp.getCurrentPosition(), 1).build());
            }
        });
        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mMediaSession.setPlaybackState(mStateBuilder.setBufferedPosition(percent).build());
            }
        });

        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(this, TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlayFromUri(Uri uri, Bundle extras) {
                try {
                    player.reset();
                    player.setDataSource(MediaPlaybackService.this, uri);
                    player.prepareAsync();
                    mMediaSession.setExtras(extras);
                    mMediaSession.setActive(true);
                    mMediaSession.setPlaybackState(mStateBuilder.setState(PlaybackStateCompat.STATE_CONNECTING, 0, 0).build());
                } catch (IOException e) {
                    e.printStackTrace();
                    mMediaSession.setExtras(null);
                    mMediaSession.setActive(false);
                    mMediaSession.setPlaybackState(mStateBuilder.setState(PlaybackStateCompat.STATE_ERROR, 0, 0).build());
                }
            }

            @Override
            public void onSeekTo(long pos) {
                player.seekTo((int) pos);
            }
        });

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mMediaSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {

        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
        if (allowBrowsing(clientPackageName, clientUid)) {
            // Returns a root ID, so clients can use onLoadChildren() to retrieve the content hierarchy
            return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
        } else {
            // Clients can connect, but since the BrowserRoot is an empty string
            // onLoadChildren will return nothing. This disables the ability to browse for content.
            return new BrowserRoot("", null);
        }
    }

    private boolean allowBrowsing(String clientPackageName, int clientUid) {
        return true;
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaBrowserCompat.MediaItem>> result) {

        //  Browsing not allowed
        if (TextUtils.isEmpty(parentMediaId)) {
            result.sendResult(null);
            return;
        }

        // Assume for example that the music catalog is already loaded/cached.

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID.equals(parentMediaId)) {

            // build the MediaItem objects for the top level,
            // and put them in the mediaItems list
        } else {

            // examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list
        }
        result.sendResult(mediaItems);
    }
}