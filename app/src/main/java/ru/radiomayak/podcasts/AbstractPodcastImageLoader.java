package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.content.LoaderState;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.http.DefaultHttpClientConnectionFactory;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.message.BasicHttpRequest;

abstract class AbstractPodcastImageLoader extends AbstractHttpLoader<BitmapInfo> {
    protected final String TAG = getClass().getSimpleName();

    private final Podcast podcast;

    protected AbstractPodcastImageLoader(Podcast podcast) {
        this.podcast = podcast;
    }

    public Podcast getPodcast() {
        return podcast;
    }

    @Override
    protected BitmapInfo onExecute(Context context, LoaderState state) {
        try {
            if (state.isCancelled()) {
                return null;
            }
            return getImage(context, state);
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Nullable
    private BitmapInfo getImage(Context context, LoaderState state) {
        Bitmap bitmap = getStoredImage(context);
        if (bitmap == null) {
            return getRemoteImage(context, state);
        }
        bitmap = postProcessBitmap(context, bitmap);
        Image image = loadImage(context);
        if (image != null && image.hasColor()) {
            return new BitmapInfo(bitmap, image.getPrimaryColor(), image.getSecondaryColor());
        }
        return new BitmapInfo(bitmap);
    }


    @Nullable
    private Bitmap getStoredImage(Context context) {
        String filename = getFilename();
        byte[] bytes = loadByteArray(context, filename);
        return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Nullable
    private BitmapInfo getRemoteImage(Context context, LoaderState state) {
        if (NetworkUtils.isConnected(context) && !state.isCancelled()) {
            try {
                String url = getUrl(context);
                byte[] bytes = requestImage(url);
                Bitmap remoteBitmap = bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (remoteBitmap == null || state.isCancelled()) {
                    return null;
                }
                Bitmap bitmap = postProcessBitmap(context, remoteBitmap);
                if (state.isCancelled()) {
                    return null;
                }
                BitmapInfo info = new BitmapInfo(bitmap);
                storeImage(context, url, info.getPrimaryColor(), info.getSecondaryColor());
                storeByteArray(context, getFilename(), bytes);
                return info;
            } catch (IOException | HttpException ignored) {
            }
        }
        return null;
    }

    @Nullable
    protected abstract Image loadImage(Context context);

    @Nullable
    protected abstract String getUrl(Context context);

    protected abstract String getFilename();

    protected abstract void storeImage(Context context, String url, int primaryColor, int secondaryColor);

    protected Bitmap postProcessBitmap(Context context, Bitmap bitmap) {
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
            HttpResponse response = getEntityResponse(connection, request);
            return getResponseBytes(response);
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
