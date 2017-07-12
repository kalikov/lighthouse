package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.AsyncTask;

class PodcastsStoreAsyncTask extends AsyncTask<Podcasts, Void, Void> {
    private final Context context;

    PodcastsStoreAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Podcasts... podcasts) {
        if (podcasts.length > 0) {
            PodcastsUtils.storePodcasts(context, podcasts[0]);
        }
        return null;
    }
}
