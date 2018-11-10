package ru.radiomayak.podcasts;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

class PodcastViewAsyncTask extends AsyncTask<Podcast, Void, Void> {
    private final WeakReference<Context> contextRef;

    PodcastViewAsyncTask(Context context) {
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
                Intent intent = new Intent(RecordsFragment.ACTION_VIEW)
                        .setPackage(context.getPackageName())
                        .putExtra(RecordsFragment.EXTRA_PODCAST_ID, podcast.getId())
                        .putExtra(RecordsFragment.EXTRA_SEEN, podcast.getLength());
                context.sendBroadcast(intent);
            }
        }
        return null;
    }
}
