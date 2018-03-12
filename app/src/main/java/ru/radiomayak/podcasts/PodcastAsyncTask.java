package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

class PodcastAsyncTask extends AbstractHttpAsyncTask<Object, Void, PodcastResponse> {
    static final Object LOOPBACK = new Object();

    private static final String LOG_TAG = PodcastAsyncTask.class.getSimpleName();

    private static final String PODCAST_URL = "http://radiomayak.ru/podcasts/podcast/id/%s/";

    private static final int OFFLINE_PAGE_SIZE = 20;

    private final PodcastLayoutParser parser = new PodcastLayoutParser();

    private final LighthouseApplication application;
    private final Listener listener;

    interface Listener {
        void onPodcastLoaded(PodcastResponse response, boolean isCancelled);
    }

    PodcastAsyncTask(LighthouseApplication application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected PodcastResponse doInBackground(Object... params) {
        try {
            long id = ((Number) params[0]).longValue();
            if (NetworkUtils.isConnected(application)) {
                try {
                    PodcastLayoutContent response = requestContent(id);
                    if (response != null && !response.getRecords().isEmpty()) {
                        PodcastsOpenHelper helper = new PodcastsOpenHelper(application);
                        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
                            database.loadRecordsFile(id, response.getRecords());
                            database.loadRecordsPosition(id, response.getRecords());
                        }
                        RecordsPaginator paginator = new OnlineRecordsPaginator(id, response.getRecords(), response.getNextPage());
                        return new PodcastResponse(response.getPodcast(), paginator);
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
            if (params.length > 1 && params[1] == LOOPBACK) {
//                List<Record> records = PodcastsUtils.loadRecords(application, id, 0, OFFLINE_PAGE_SIZE + 1);
//                File cacheDir = application.getCacheDir();
//                for (Record record : records) {
//                    Cache cache = CacheUtils.getCache(cacheDir, String.valueOf(id));
//                    record.setCacheSize((int) cache.getCacheSpace());
//                }
//                return new PodcastResponse(null, new OfflineRecordsPaginator(id, records, OFFLINE_PAGE_SIZE));
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

    @Nullable
    private PodcastLayoutContent requestContent(long podcast) throws IOException, HttpException {
        URL url = new URL(String.format(PODCAST_URL, String.valueOf(podcast)));
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
                return parser.parse(podcast, IOUtils.buffer(input), HttpUtils.getCharset(response), url.toString());
            }
        }
    }
}
