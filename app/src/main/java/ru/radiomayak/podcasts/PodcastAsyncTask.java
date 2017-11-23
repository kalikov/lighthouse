package ru.radiomayak.podcasts;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.List;

import ru.radiomayak.CacheUtils;
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
import ru.radiomayak.media.ByteMap;
import ru.radiomayak.media.ByteMapUtils;

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
                    if (response != null && !response.getRecords().list().isEmpty()) {
                        PodcastsUtils.storeRecords(application, id, response.getRecords().list());
                        File cacheDir = application.getCacheDir();
                        for (Record record : response.getRecords().list()) {
                            try (RandomAccessFile file = new RandomAccessFile(CacheUtils.getFile(cacheDir, 0, String.valueOf(record.getId())), "r")) {
                                ByteMap byteMap = ByteMapUtils.readHeader(file);
                                record.setCacheSize(byteMap.size());
                            } catch (IOException ignored) {
                            }
                        }
                        RecordsPaginator paginator = new OnlineRecordsPaginator(id, response.getRecords(), response.getNextPage());
                        return new PodcastResponse(response.getPodcast(), paginator);
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
            if (params.length > 1 && params[1] == LOOPBACK) {
                List<Record> records = PodcastsUtils.loadRecords(application, id, 0, OFFLINE_PAGE_SIZE + 1);
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
            try (InputStream input = HttpUtils.getContent(response.getEntity())) {
                return parser.parse(id, IOUtils.buffer(input), HttpUtils.getCharset(response), url.toString());
            }
        }
    }
}
