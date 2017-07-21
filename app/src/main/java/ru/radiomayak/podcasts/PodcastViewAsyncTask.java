package ru.radiomayak.podcasts;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

class PodcastViewAsyncTask extends AsyncTask<Podcast, Void, Void> {
    private final Context context;

    PodcastViewAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        if (podcasts.length > 0) {
            Podcast podcast = podcasts[0];
            PodcastsUtils.storePodcastSeen(context, podcast.getId(), podcast.getLength());
            Intent intent = new Intent(RecordsActivity.ACTION_VIEW)
                    .setPackage(context.getPackageName())
                    .putExtra(RecordsActivity.EXTRA_PODCAST_ID, podcast.getId())
                    .putExtra(RecordsActivity.EXTRA_SEEN, podcast.getLength());
            context.sendBroadcast(intent);
        }
        return null;
    }
}
