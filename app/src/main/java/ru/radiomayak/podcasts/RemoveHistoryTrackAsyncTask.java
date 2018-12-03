package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

class RemoveHistoryTrackAsyncTask extends AsyncTask<HistoryTrack, Void, Void> {
    private final WeakReference<Context> contextRef;

    RemoveHistoryTrackAsyncTask(Context context) {
        contextRef = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(HistoryTrack... tracks) {
        if (tracks.length > 0) {
            Context context = contextRef.get();
            if (context != null) {
                HistoryTrack track = tracks[0];
                PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
                try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
                    database.removeHistoryRecord(track.getPodcast().getId(), track.getRecord().getId());
                }
            }
        }
        return null;
    }
}
