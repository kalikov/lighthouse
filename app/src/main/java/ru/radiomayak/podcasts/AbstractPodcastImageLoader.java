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

    protected AbstractPodcastImageLoader(Context context, Podcast podcast) {
        super(context);
        this.podcast = podcast;
    }

    public Podcast getPodcast() {
        return podcast;
    }

    @Override
    protected BitmapInfo onExecute() {
        try {
            if (isCancelled()) {
                return null;
            }
            return getImage();
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Nullable
    private BitmapInfo getImage() {
        boolean extractColors = true;
        Bitmap bitmap = getStoredImage();
        if (bitmap != null) {
            extractColors = shouldExtractColors();
        } else {
            bitmap = getRemoteImage();
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
        storeColors(info.getPrimaryColor(), info.getSecondaryColor());
        return info;
    }


    @Nullable
    private Bitmap getStoredImage() {
        String filename = getFilename();
        byte[] bytes = loadByteArray(getContext(), filename);
        return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Nullable
    private Bitmap getRemoteImage() {
        if (NetworkUtils.isConnected(getContext()) && !isCancelled()) {
            try {
                byte[] bytes = requestImage(getUrl());
                if (bytes != null) {
                    String filename = getFilename();
                    storeByteArray(getContext(), filename, bytes);
                }
                return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (IOException | HttpException ignored) {
            }
        }
        return null;
    }

    protected abstract boolean shouldExtractColors();

    @Nullable
    protected abstract String getUrl();

    protected abstract String getFilename();

    protected abstract void storeColors(int primaryColor, int secondaryColor);

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
