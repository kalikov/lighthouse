package ru.radiomayak.podcasts;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.radiomayak.LighthouseApplication;
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

class PodcastsLoader extends AbstractHttpLoader<Podcasts> {
    private static final String LOG_TAG = PodcastsLoader.class.getSimpleName();

    private static final String PODCASTS_URL = "http://radiomayak.ru/podcasts/";

    private final PodcastsLayoutParser parser = new PodcastsLayoutParser();

    private final boolean loopback;
    private final AtomicBoolean requested = new AtomicBoolean();

    PodcastsLoader(Context context, boolean loopback) {
        super(context);
        this.loopback = loopback;
    }

    @Override
    protected Podcasts onExecute() {
        Podcasts podcasts = null;
        try {
            Podcasts loopbackPodcasts = PodcastsUtils.loadPodcasts(getContext());
            if (NetworkUtils.isConnected(getContext())) {
                try {
                    podcasts = requestPodcasts(PODCASTS_URL);
                    if (podcasts != null && !podcasts.list().isEmpty()) {
                        for (Podcast podcast : podcasts.list()) {
                            if (loopbackPodcasts.list().isEmpty()) {
                                podcast.setSeen(podcast.getLength());
                            } else {
                                Podcast loopbackPodcast = loopbackPodcasts.get(podcast.getId());
                                if (loopbackPodcast != null) {
                                    podcast.setSeen(loopbackPodcast.getSeen());
                                    setColors(podcast.getIcon(), loopbackPodcast.getIcon());
                                    setColors(podcast.getSplash(), loopbackPodcast.getSplash());
                                }
                            }
                        }
                        requested.set(true);
                        return podcasts;
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
            if (loopback) {
                return loopbackPodcasts;
            }
        } catch (Throwable e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            if (podcasts == null) {
                podcasts = new Podcasts();
            }
        }
        return podcasts;
    }

    @Override
    protected void onEndLoading(Podcasts podcasts) {
        if (!podcasts.list().isEmpty() && requested.get()) {
            new PodcastsStoreAsyncTask(getContext()).executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, podcasts);
        }
    }

    private static void setColors(Image target, Image source) {
        if (target != null && source != null && target.getUrl().equalsIgnoreCase(source.getUrl())) {
            target.setPrimaryColor(source.getPrimaryColor());
            target.setSecondaryColor(source.getSecondaryColor());
        }
    }

    private Podcasts requestPodcasts(String spec) throws IOException, HttpException {
        URL url = new URL(spec);
        HttpRequest request = new BasicHttpRequest("GET", url.getPath(), HttpVersion.HTTP_1_1);
        request.setHeader(HttpHeaders.ACCEPT, "text/html,*/*");
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(HttpHeaders.HOST, url.getAuthority());
        // If-Modified-Since: Thu, 24 Nov 2016 10:13:10 GMT
        try (HttpClientConnection connection = DefaultHttpClientConnectionFactory.INSTANCE.openConnection(url)) {
            HttpResponse response = getEntityResponse(connection, request);
            if (response == null) {
                return null;
            }
            try (InputStream input = HttpUtils.getContent(response.getEntity())) {
                return parser.parse(IOUtils.buffer(input), HttpUtils.getCharset(response), url.toString());
            }
        }
    }

    @Override
    public int hashCode() {
        return loopback ? 1 : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof PodcastsLoader)) {
            return false;
        }
        PodcastsLoader other = (PodcastsLoader) object;
        return loopback == other.loopback;
    }
}
