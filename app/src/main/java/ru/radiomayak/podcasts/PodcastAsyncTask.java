package ru.radiomayak.podcasts;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.List;

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

class PodcastAsyncTask extends AbstractHttpAsyncTask<Object, Void, PodcastResponse> {
    static final Object LOOPBACK = new Object();

    private static final String LOG_TAG = PodcastAsyncTask.class.getSimpleName();

    private static final String PODCAST_URL = "http://radiomayak.ru/podcasts/podcast/id/%s/";

    private static final int OFFLINE_PAGE_SIZE = 20;

    private final PodcastLayoutJsoupParser parser = new PodcastLayoutJsoupParser();

    private final Context context;
    private final Listener listener;

    interface Listener {
        void onPodcastLoaded(PodcastResponse response, boolean isCancelled);
    }

    PodcastAsyncTask(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected PodcastResponse doInBackground(Object... params) {
        try {
            long id = ((Number) params[0]).longValue();
            if (NetworkUtils.isConnected(context)) {
                try {
                    PodcastLayoutContent response = requestContent(id);
                    if (response != null && !response.getRecords().list().isEmpty()) {
                        PodcastsUtils.storeRecords(context, id, response.getRecords().list());
                        RecordsPaginator paginator = new OnlineRecordsPaginator(id, response.getRecords(), response.getNextPage());
                        return new PodcastResponse(response.getPodcast(), paginator);
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
            if (params.length > 1 && params[1] == LOOPBACK) {
                List<Record> records = PodcastsUtils.loadRecords(context, id, 0, OFFLINE_PAGE_SIZE + 1);
                return new PodcastResponse(null, new OfflineRecordsPaginator(id, records, OFFLINE_PAGE_SIZE));
            }
        } catch (Throwable e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return new PodcastResponse(null, null);
    }

    @Override
    protected void onPostExecute(PodcastResponse response) {
        listener.onPodcastLoaded(response, false);
    }

    @Override
    protected void onCancelled(PodcastResponse response) {
        listener.onPodcastLoaded(response, true);
    }

    private PodcastLayoutContent requestContent(long id) throws IOException, HttpException {
        URL url = new URL(String.format(PODCAST_URL, String.valueOf(id)));
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
            byte[] bytes = getResponseBytes(response);
            return bytes == null ? null : parser.parse(bytes, HttpUtils.getCharset(response), url.toString());
        }
    }
}
