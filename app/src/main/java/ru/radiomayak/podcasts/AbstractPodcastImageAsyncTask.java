package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.LongSparseArray;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.http.DefaultHttpClientConnectionFactory;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.message.BasicHttpRequest;

abstract class AbstractPodcastImageAsyncTask extends AsyncTask<Podcast, Void, LongSparseArray<BitmapInfo>> {
    protected final String TAG = getClass().getSimpleName();

    protected final Context context;

    protected AbstractPodcastImageAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected LongSparseArray<BitmapInfo> doInBackground(Podcast... podcasts) {
        LongSparseArray<BitmapInfo> array = new LongSparseArray<>(podcasts.length);
        try {
            for (Podcast podcast : podcasts) {
                if (isCancelled()) {
                    return array;
                }
                BitmapInfo image = getImage(podcast);
                if (image != null) {
                    array.put(podcast.getId(), image);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return array;
    }

    @Nullable
    private BitmapInfo getImage(Podcast podcast) {
        boolean extractColors = true;
        Bitmap bitmap = getStoredImage(podcast);
        if (bitmap != null) {
            extractColors = shouldExtractColors(podcast);
        } else {
            bitmap = getRemoteImage(podcast);
            if (bitmap == null) {
                return null;
            }
        }
        if (isCancelled()) {
            return null;
        }
        bitmap = postProcessBitmap(bitmap);
        if (!extractColors || isCancelled()) {
            return new BitmapInfo(bitmap, 0, 0);
        }
        BitmapInfo info = new BitmapInfo(bitmap);
        storeColors(podcast, info.getPrimaryColor(), info.getSecondaryColor());
        return info;
    }


    @Nullable
    private Bitmap getStoredImage(Podcast podcast) {
        String filename = getFilename(podcast);
        byte[] bytes = loadByteArray(context, filename);
        return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Nullable
    private Bitmap getRemoteImage(Podcast podcast) {
        if (NetworkUtils.isConnected(context) && !isCancelled()) {
            try {
                byte[] bytes = requestImage(getUrl(podcast));
                if (bytes != null) {
                    String filename = getFilename(podcast);
                    storeByteArray(context, filename, bytes);
                }
                return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (IOException | HttpException ignored) {
            }
        }
        return null;
    }

    protected abstract boolean shouldExtractColors(Podcast podcast);

    @Nullable
    protected abstract String getUrl(Podcast podcast);

    protected abstract String getFilename(Podcast podcast);

    protected abstract void storeColors(Podcast podcast, int primaryColor, int secondaryColor);

    protected Bitmap postProcessBitmap(Bitmap bitmap) {
        return bitmap;
    }

    @Nullable
    private byte[] requestImage(@Nullable String source) throws IOException, HttpException {
        if (source == null) {
            return null;
        }
        URL url = new URL(source);
        HttpRequest request = new BasicHttpRequest("GET", url.getPath(), HttpVersion.HTTP_1_1);
        request.setHeader(HttpHeaders.ACCEPT, "image/*");
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(HttpHeaders.HOST, url.getAuthority());
        // If-Modified-Since: Thu, 24 Nov 2016 10:13:10 GMT
        try (HttpClientConnection connection = DefaultHttpClientConnectionFactory.INSTANCE.openConnection(url)) {
            connection.setSocketTimeout(NetworkUtils.getRequestTimeout());
            connection.sendRequestHeader(request);
            connection.flush();
            HttpResponse response = connection.receiveResponseHeader();
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status >= 400) {
                return null;
            }
            connection.receiveResponseEntity(response);
            if (response.getEntity() == null || response.getEntity().getContentLength() <= 0) {
                return null;
            }
            try (InputStream stream = HttpUtils.getContent(response.getEntity())) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                int n;
                byte[] buffer = new byte[10 * 1024];
                while (IOUtils.EOF != (n = stream.read(buffer)) && !isCancelled()) {
                    output.write(buffer, 0, n);
                }
                return isCancelled() ? null : output.toByteArray();
            }
        }
    }

    private static void storeByteArray(Context context, String filename, byte[] bytes) {
        try (OutputStream output = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            output.write(bytes);
        } catch (IOException ignored) {
        }
    }

    private static byte[] loadByteArray(Context context, String filename) {
        try (InputStream input = context.openFileInput(filename)) {
            return IOUtils.toByteArray(input);
        } catch (IOException e) {
            return null;
        }
    }
}
