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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import ru.radiomayak.animation.Interpolators;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.PodcastsUtils;
import ru.radiomayak.podcasts.Record;

public abstract class LighthouseFragment extends Fragment {
    private MediaControllerCompat.Callback controllerCallback;

    @Nullable
    public final LighthouseActivity getLighthouseActivity() {
        return (LighthouseActivity) getActivity();
    }

    @NonNull
    public final LighthouseActivity requireLighthouseActivity() {
        return (LighthouseActivity) requireActivity();
    }

    public abstract void updatePlayerState();
}
