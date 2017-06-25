package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
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

class PodcastsAsyncTask extends AsyncTask<Object, Void, Podcasts> {
    static final Object LOOPBACK = new Object();

    private static final String LOG_TAG = PodcastsAsyncTask.class.getSimpleName();

    private static final String PODCASTS_URL = "http://radiomayak.ru/podcasts/";

    private final PodcastsLayoutParser parser = new PodcastsLayoutParser();

    private final Context context;
    private final Listener listener;

    interface Listener {
        void onPodcastsLoaded(Podcasts response, boolean isCancelled);
    }

    PodcastsAsyncTask(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Podcasts doInBackground(Object... params) {
        Podcasts podcasts = null;
        try {
            if (NetworkUtils.isConnected(context)) {
                try {
                    podcasts = requestPodcasts(PODCASTS_URL);
                    if (podcasts != null && !podcasts.list().isEmpty()) {
                        PodcastsUtils.storePodcasts(context, podcasts);
                        return podcasts;
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
            if (params.length > 0 && params[0] == LOOPBACK) {
                return PodcastsUtils.loadPodcasts(context);
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
    protected void onPostExecute(Podcasts response) {
        listener.onPodcastsLoaded(response, false);
    }

    @Override
    protected void onCancelled(Podcasts response) {
        listener.onPodcastsLoaded(response, true);
    }

    private Podcasts requestPodcasts(String spec) throws IOException, HttpException {
        URL url = new URL(spec);
        HttpRequest request = new BasicHttpRequest("GET", url.getPath(), HttpVersion.HTTP_1_1);
        request.setHeader(HttpHeaders.ACCEPT, "text/html,*/*");
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
            if (response.getEntity() == null || response.getEntity().getContentLength() == 0) {
                return null;
            }
            try (InputStream stream = HttpUtils.getContent(response.getEntity())) {
                return parser.parse(stream, HttpUtils.getCharset(response), url.toString());
            }
        }
    }
}
