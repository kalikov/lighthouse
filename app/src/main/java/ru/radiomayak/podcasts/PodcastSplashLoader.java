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
        return !PodcastsUtils.hasSplashColors(getContext(), getPodcast(), splash == null ? null : splash.getUrl());
    }

    @Nullable
    @Override
    protected String getUrl() {
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
    protected void storeColors(int primaryColor, int secondaryColor) {
        PodcastsUtils.storePodcastSplashColors(getContext(), getPodcast().getId(), primaryColor, secondaryColor);
    }
}
