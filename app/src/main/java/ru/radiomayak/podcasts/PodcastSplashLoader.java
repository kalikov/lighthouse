package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;

class PodcastSplashLoader extends AbstractPodcastImageLoader {
    PodcastSplashLoader(Context context, Podcast podcast) {
        super(context, podcast);
    }

    @Override
    protected boolean shouldExtractColors() {
        Image splash = getPodcast().getSplash();
        return splash != null && splash.getPrimaryColor() == 0;
    }

    @Nullable
    @Override
    protected String getUrl() {
        Image splash = getPodcast().getSplash();
        return splash == null ? null : splash.getUrl();
    }

    @Override
    protected String getFilename() {
        return PodcastsUtils.getPodcastSplashFilename(getPodcast());
    }

    @Override
    protected void storeColors(int primaryColor, int secondaryColor) {
        PodcastsUtils.storePodcastSplashColors(getContext(), getPodcast().getId(), primaryColor, secondaryColor);
    }
}
