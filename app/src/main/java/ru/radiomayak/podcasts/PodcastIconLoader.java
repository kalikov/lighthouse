package ru.radiomayak.podcasts;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import ru.radiomayak.R;

class PodcastIconLoader extends AbstractPodcastImageLoader {
    private final PodcastsActivity activity;

    PodcastIconLoader(PodcastsActivity activity, Podcast podcast) {
        super(activity, podcast);
        this.activity = activity;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        activity.onIconLoadStarted(getPodcast().getId());
    }

    @Override
    protected boolean shouldExtractColors() {
        Image splash = getPodcast().getSplash();
        return !PodcastsUtils.hasColors(getContext(), getPodcast(), getUrl(), splash == null ? null : splash.getUrl());
    }

    @Nullable
    @Override
    protected String getUrl() {
        Image icon = getPodcast().getIcon();
        return icon == null ? null : icon.getUrl();
    }

    @Override
    protected String getFilename() {
        return PodcastsUtils.getPodcastIconFilename(getPodcast());
    }

    @Override
    protected void storeColors(int primaryColor, int secondaryColor) {
        PodcastsUtils.storePodcastIconColors(getContext(), getPodcast().getId(), primaryColor, secondaryColor);
    }

    @Override
    protected Bitmap postProcessBitmap(Bitmap bitmap) {
        int size = getContext().getResources().getDimensionPixelSize(R.dimen.podcast_icon_size);
        if (bitmap == null || bitmap.getWidth() <= size || bitmap.getHeight() <= size) {
            return bitmap;
        }
        double widthAspect = (double) bitmap.getWidth() / size;
        double heightAspect = (double) bitmap.getHeight() / size;
        int width = size;
        int height = size;
        if (widthAspect > heightAspect) {
            width = (int) (bitmap.getWidth() * heightAspect);
        } else if (widthAspect < heightAspect) {
            height = (int) (bitmap.getHeight() * widthAspect);
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        bitmap.recycle();
        return scaledBitmap;
    }
}
