package ru.radiomayak.podcasts;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import ru.radiomayak.StringUtils;
import ru.radiomayak.media.MediaProxyContext;

public class PodcastsMediaProxyContext implements MediaProxyContext {
    private static final String TAG = PodcastsMediaProxyContext.class.getSimpleName();

    private final Context context;
    private final File cacheDir;

    public PodcastsMediaProxyContext(Context application) {
        this.context = application;
        File dir = application.getExternalFilesDir(Environment.DIRECTORY_PODCASTS);
        if (dir == null) {
            dir = application.getFilesDir();
        }
        cacheDir = dir;
        if (!cacheDir.mkdirs()) {
            Log.e(TAG, "Storage directories not created");
        }
    }

    public File getCacheDir() {
        return cacheDir;
    }

    @Override
    public void notifyUpdate(String category, String id, int size, boolean partial) {
        long podcast = StringUtils.parseLong(category, 0);
        long record = StringUtils.parseLong(id, 0);
        if (podcast != 0 && record != 0) {
            Intent intent = new Intent(RecordsActivity.ACTION_UPDATE)
                    .setPackage(context.getPackageName())
                    .putExtra(RecordsActivity.EXTRA_PODCAST_ID, podcast)
                    .putExtra(RecordsActivity.EXTRA_RECORD_ID, record)
                    .putExtra(RecordsActivity.EXTRA_CACHE_SIZE, size)
                    .putExtra(RecordsActivity.EXTRA_CACHE_PARTIAL, partial);
            context.sendBroadcast(intent);
        }
    }
}
