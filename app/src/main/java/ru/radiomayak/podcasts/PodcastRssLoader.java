package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ru.radiomayak.http.DefaultHttpClientConnectionFactory;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.message.BasicHttpRequest;

public class PodcastRssLoader extends AbstractHttpLoader<Records> {
    private static final String TAG = PodcastRssLoader.class.getSimpleName();

    private static final String URL_FORMAT = "http://radiomayak.ru/podcasts/rss/podcast/%s/type/audio/";

    private final PodcastRssParser parser = new PodcastRssParser();

    private final Podcast podcast;

    public PodcastRssLoader(Context context, Podcast podcast) {
        super(context);
        this.podcast = podcast;
    }

    @Nullable
    public Records onExecute() {
        try {
            return getRecords();
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Nullable
    private Records getRecords() throws IOException, HttpException {
        URL url =  new URL(String.format(URL_FORMAT, String.valueOf(podcast.getId())));
        HttpRequest request = new BasicHttpRequest("GET", url.getPath(), HttpVersion.HTTP_1_1);
        request.setHeader(HttpHeaders.ACCEPT, "application/rss+xml,application/xml");
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(HttpHeaders.HOST, url.getAuthority());
        // If-Modified-Since: Thu, 24 Nov 2016 10:13:10 GMT
        try (HttpClientConnection connection = DefaultHttpClientConnectionFactory.INSTANCE.openConnection(url)) {
            HttpResponse response = getEntityResponse(connection, request);
            if (response == null) {
                return null;
            }
            try (InputStream input = HttpUtils.getContent(response.getEntity())) {
                return parser.parse(IOUtils.toBufferedInputStream(input));
            }
        }
    }
}
