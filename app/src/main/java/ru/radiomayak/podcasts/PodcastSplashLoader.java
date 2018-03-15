package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;

class PodcastSplashLoader extends AbstractPodcastImageLoader {
    PodcastSplashLoader(Podcast podcast) {
        super(podcast);
    }

    @Override
    protected Image loadImage(Context context) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            return database.loadPodcastSplash(getPodcast().getId());
        }
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
    protected void storeImage(Context context, @Nullable String url, int primaryColor, int secondaryColor) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
            database.storePodcastSplash(getPodcast().getId(), url, primaryColor, secondaryColor);
        }
    }
}
