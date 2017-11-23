package ru.radiomayak.media;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;

import ru.radiomayak.Preferences;

class MediaLoadControl implements LoadControl {
    private final LoadControl defaultControl = new DefaultLoadControl();

    private final Context context;

    private boolean alwaysContinueLoading;

    MediaLoadControl(Context context) {
        this.context = context;
    }

    @Override
    public void onPrepared() {
        defaultControl.onPrepared();
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        defaultControl.onTracksSelected(renderers, trackGroups, trackSelections);
    }

    @Override
    public void onStopped() {
        defaultControl.onStopped();
    }

    @Override
    public void onReleased() {
        defaultControl.onReleased();
    }

    @Override
    public Allocator getAllocator() {
        return defaultControl.getAllocator();
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, boolean rebuffering) {
        return defaultControl.shouldStartPlayback(bufferedDurationUs, rebuffering);
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs) {
        boolean useCache = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Preferences.CACHE_WHILE_STREAMING, false);
        return useCache && alwaysContinueLoading || defaultControl.shouldContinueLoading(bufferedDurationUs);
    }

    void setAlwaysContinueLoading(boolean alwaysContinueLoading) {
        this.alwaysContinueLoading = alwaysContinueLoading;
    }
}
