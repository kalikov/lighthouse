package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;
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

class PodcastLoader extends AbstractHttpLoader<PodcastResponse> {
    private static final String TAG = PodcastLoader.class.getSimpleName();

    private static final String PODCAST_URL = "https://radiomayak.ru/podcasts/podcast/id/%s/";

    private final PodcastLayoutParser parser = new PodcastLayoutParser();

    private final long id;

    PodcastLoader(long id) {
        this.id = id;
    }

    @Override
    protected PodcastResponse onExecute(Context context, LoaderState state) {
        try {
            if (NetworkUtils.isConnected(context)) {
                try {
                    PodcastLayoutContent response = requestContent(id);
                    if (response != null && !response.getRecords().isEmpty()) {
                        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
                        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
                            database.loadRecordsPositionAndLength(id, response.getRecords());
                        }
                        RecordsPaginator paginator = new OnlineRecordsPaginator(id, response.getRecords(), response.getNextPage());
                        return new PodcastResponse(response.getPodcast(), response.isArchived(), paginator);
                    }
                } catch (IOException | HttpException ignored) {
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return new PodcastResponse(null, null, null);
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

    @Override
    public int hashCode() {
        return (int)(id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof PodcastLoader && ((PodcastLoader) object).id == id;
    }
}

