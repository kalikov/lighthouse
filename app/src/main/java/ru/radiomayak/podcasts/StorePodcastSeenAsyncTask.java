package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

class StorePodcastSeenAsyncTask extends AsyncTask<Podcast, Void, Void> {
    private final WeakReference<Context> contextRef;

    StorePodcastSeenAsyncTask(Context context) {
        contextRef = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        if (podcasts.length > 0) {
            Context context = contextRef.get();
            if (context != null) {
                Podcast podcast = podcasts[0];
                PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
                try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
                    database.storePodcastSeen(podcast.getId(), podcast.getLength());
                }
            }
        }
        return null;
    }
}
