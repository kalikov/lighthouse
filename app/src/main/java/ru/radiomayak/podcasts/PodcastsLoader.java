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

class PodcastsLoader extends AbstractHttpLoader<Podcasts> {
    private static final String LOG_TAG = PodcastsLoader.class.getSimpleName();

    private static final String PODCASTS_URL = "http://radiomayak.ru/podcasts/";

    private final PodcastsLayoutParser parser = new PodcastsLayoutParser();
    private final Podcasts loopbackPodcasts;

    PodcastsLoader(Podcasts loopbackPodcasts) {
        this.loopbackPodcasts = loopbackPodcasts;
    }

    @Override
    protected Podcasts onExecute(Context context, LoaderState state) {
//        Podcasts podcasts = null;
        try {
            if (NetworkUtils.isConnected(context)) {
                try {
                    Podcasts podcasts = requestPodcasts(PODCASTS_URL);
                    if (podcasts != null && !podcasts.list().isEmpty()) {
                        for (Podcast podcast : podcasts.list()) {
                            if (loopbackPodcasts.list().isEmpty()) {
                                podcast.setSeen(podcast.getLength());
                            } else {
                                Podcast loopbackPodcast = loopbackPodcasts.get(podcast.getId());
                                if (loopbackPodcast != null) {
                                    podcast.setSeen(loopbackPodcast.getSeen());
                                    copyColors(podcast.getIcon(), loopbackPodcast.getIcon());
                                    copyColors(podcast.getSplash(), loopbackPodcast.getSplash());
                                }
                            }
                        }
                        return podcasts;
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
        } catch (Throwable e) {
            Log.e(LOG_TAG, e.getMessage(), e);
//            if (podcasts == null) {
//                podcasts = new Podcasts();
//            }
//            return new Podcasts();
        }
        return new Podcasts();
//        return podcasts;
    }

//    @Override
//    protected void onEndLoading(Podcasts podcasts) {
//        if (!podcasts.list().isEmpty() && requested.get()) {
//            new StorePodcastsRunnable(getContext()).executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, podcasts);
//        }
//    }

    private static void copyColors(Image target, Image source) {
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
        return 1;
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof PodcastsLoader;
    }
}
