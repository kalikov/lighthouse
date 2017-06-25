package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.LongSparseArray;

import ru.radiomayak.R;

class PodcastIconAsyncTask extends AbstractPodcastImageAsyncTask {
    private final Listener listener;

    interface Listener {
        void onPodcastIconResponse(LongSparseArray<BitmapInfo> response);
    }

    PodcastIconAsyncTask(Context context, Listener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onPostExecute(LongSparseArray<BitmapInfo> response) {
        listener.onPodcastIconResponse(response);
    }

    @Override
    protected boolean shouldExtractColors(Podcast podcast) {
        Image splash = podcast.getSplash();
        Image icon = podcast.getIcon();
        return splash == null || splash.getPrimaryColor() == 0 || icon != null && icon.getPrimaryColor() == 0;
    }

    @Nullable
    @Override
    protected String getUrl(Podcast podcast) {
        return podcast.getIcon() == null ? null : podcast.getIcon().getUrl();
    }

    @Override
    protected String getFilename(Podcast podcast) {
        return PodcastsUtils.getPodcastIconFilename(podcast);
    }

    @Override
    protected void storeColors(Podcast podcast, int primaryColor, int secondaryColor) {
        PodcastsUtils.storePodcastIconColors(context, podcast.getId(), primaryColor, secondaryColor);
    }

    @Override
    protected Bitmap postProcessBitmap(Bitmap bitmap) {
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
