package ru.radiomayak.podcasts;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.content.LoaderState;
import ru.radiomayak.http.DefaultHttpClientConnectionFactory;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.message.BasicHttpRequest;

class ArchiveLoader extends AbstractHttpLoader<Podcasts> {
    private static final String LOG_TAG = ArchiveLoader.class.getSimpleName();

    private static final String PODCASTS_URL = "https://radiomayak.ru/podcasts/archive/";

    private final PodcastsLayoutParser parser = new PodcastsLayoutParser();

    @Override
    protected Podcasts onExecute(Context context, LoaderState state) {
        try {
            if (NetworkUtils.isConnected(context)) {
                try {
                    return requestPodcasts(PODCASTS_URL);
                } catch (IOException | HttpException ignored) {
                }
            }
        } catch (Throwable e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return new Podcasts();
    }

    private Podcasts requestPodcasts(String spec) throws IOException, HttpException {
        URL url = new URL(spec);
        HttpRequest request = new BasicHttpRequest("GET", url.getPath(), HttpVersion.HTTP_1_1);
        request.setHeader(HttpHeaders.ACCEPT, "text/html,*/*");
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(HttpHeaders.HOST, url.getAuthority());
        try (HttpClientConnection connection = DefaultHttpClientConnectionFactory.INSTANCE.openConnection(url)) {
            HttpResponse response = getEntityResponse(connection, request);
            if (response == null) {
                return null;
            }
            try (InputStream input = HttpUtils.getContent(response.getEntity())) {
                Podcasts podcasts = parser.parse(IOUtils.buffer(input), HttpUtils.getCharset(response), url.toString());
                for (Podcast podcast : podcasts.list()) {
                    podcast.setArchived(true);
                }
                return podcasts;
            }
        }
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof ArchiveLoader;
    }
}
