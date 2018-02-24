package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;

class PodcastSplashLoader extends AbstractPodcastImageLoader {
    PodcastSplashLoader(Podcast podcast) {
        super(podcast);
    }

    @Override
    protected boolean shouldExtractColors(Context context) {
        Image splash = getPodcast().getSplash();
        return !PodcastsUtils.hasSplashColors(context, getPodcast(), splash == null ? null : splash.getUrl());
    }

    @Nullable
    @Override
    protected String getUrl(Context context) {
        Image splash = getPodcast().getSplash();
        if (splash != null) {
            return splash.getUrl();
        }
        Image icon = getPodcast().getIcon();
        return icon == null ? null : PictureUrlUtils.getPictureUrl(icon.getUrl(), PictureUrlUtils.Size.L);
    }

    @Override
    protected String getFilename() {
        return PodcastsUtils.getPodcastSplashFilename(getPodcast());
    }

    @Override
    protected void storeColors(Context context, int primaryColor, int secondaryColor) {
        PodcastsUtils.storePodcastSplashColors(context, getPodcast().getId(), primaryColor, secondaryColor);
    }
}
