package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import ru.radiomayak.R;

class PodcastIconLoader extends AbstractPodcastImageLoader {
    private final PodcastsActivity activity;

    PodcastIconLoader(PodcastsActivity activity, Podcast podcast) {
        super(podcast);
        this.activity = activity;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        activity.onIconLoadStarted(getPodcast().getId());
    }

    @Override
    protected Image loadImage(Context context) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            return database.loadPodcastIcon(getPodcast().getId());
        }
    }

    @Nullable
    @Override
    protected String getUrl(Context context) {
        Image icon = getPodcast().getIcon();
        if (icon == null) {
            return null;
        }
        String url = icon.getUrl();
        int size = context.getResources().getDimensionPixelSize(R.dimen.podcast_icon_size);
        if (size <= PictureUrlUtils.Size.XS_SQUARE.getWidth()) {
            return PictureUrlUtils.getPictureUrl(url, PictureUrlUtils.Size.XS_SQUARE);
        }
        return url;
    }

    @Override
    protected String getFilename() {
        return PodcastsUtils.getPodcastIconFilename(getPodcast());
    }

    @Override
    protected void storeImage(Context context, String url, int primaryColor, int secondaryColor) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
            database.storePodcastIcon(getPodcast().getId(), url, primaryColor, secondaryColor);
        }
    }

    @Override
    protected Bitmap postProcessBitmap(Context context, Bitmap bitmap) {
        int size = context.getResources().getDimensionPixelSize(R.dimen.podcast_icon_size);
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
