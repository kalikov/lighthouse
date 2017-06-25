package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.LongSparseArray;

class PodcastSplashAsyncTask extends AbstractPodcastImageAsyncTask {
    private final Listener listener;

    interface Listener {
        void onPodcastSplashResponse(LongSparseArray<BitmapInfo> response, boolean isCancelled);
    }

    PodcastSplashAsyncTask(Context context, Listener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onPostExecute(LongSparseArray<BitmapInfo> response) {
        listener.onPodcastSplashResponse(response, false);
    }

    @Override
    protected void onCancelled(LongSparseArray<BitmapInfo> response) {
        listener.onPodcastSplashResponse(response, true);
    }

    @Override
    protected boolean shouldExtractColors(Podcast podcast) {
        return podcast.getSplash() != null && podcast.getSplash().getPrimaryColor() == 0;
    }

    @Nullable
    @Override
    protected String getUrl(Podcast podcast) {
        return podcast.getSplash() == null ? null : podcast.getSplash().getUrl();
    }

    @Override
    protected String getFilename(Podcast podcast) {
        return PodcastsUtils.getPodcastSplashFilename(podcast);
    }

    @Override
    protected void storeColors(Podcast podcast, int primaryColor, int secondaryColor) {
        PodcastsUtils.storePodcastSplashColors(context, podcast.getId(), primaryColor, secondaryColor);
    }
}
