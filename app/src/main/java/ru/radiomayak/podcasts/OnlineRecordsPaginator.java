package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.Parcel;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
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

class OnlineRecordsPaginator implements RecordsPaginator {
    public static final Creator<OnlineRecordsPaginator> CREATOR = new Creator<OnlineRecordsPaginator>() {
        @Override
        public OnlineRecordsPaginator createFromParcel(Parcel in) {
            return new OnlineRecordsPaginator(in);
        }

        @Override
        public OnlineRecordsPaginator[] newArray(int size) {
            return new OnlineRecordsPaginator[size];
        }
    };

    private static final String PAGE_URL = "http://radiomayak.ru/podcasts/loadepisodes/podcast/%s/page/%s/";

    private static final PodcastJsonParser parser = new PodcastJsonParser();

    private final long id;
    private final Records records;
    private long nextPage;

    OnlineRecordsPaginator(long id, Records records, long nextPage) {
        this.id = id;
        this.records = records;
        this.nextPage = nextPage;
    }

    protected OnlineRecordsPaginator(Parcel in) {
        id = in.readLong();
        nextPage = in.readLong();
        records = Records.CREATOR.createFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeLong(nextPage);
        records.writeToParcel(out, flags);
    }

    @Override
    public Iterable<Record> current() {
        return records.list();
    }

    @Override
    public boolean hasNext() {
        return nextPage > 0;
    }

    @Override
    public RecordsPaginator advance(Context context) {
        try {
            return requestPage(context, id, nextPage);
        } catch (IOException | HttpException e) {
            throw new RuntimeException(e);
        }
    }

    private RecordsPaginator requestPage(Context context, long id, long nextPage) throws IOException, HttpException {
        String spec = String.format(PAGE_URL, String.valueOf(id), String.valueOf(nextPage));
        URL url = new URL(spec);
        HttpRequest request = new BasicHttpRequest("GET", url.getPath(), HttpVersion.HTTP_1_1);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
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
                return handlePageResponse(context, id, stream, NetworkUtils.toOptURI(spec));
            }
        }
    }

    private static RecordsPaginator handlePageResponse(Context context, long podcast, InputStream input, URI uri) throws IOException {
        PodcastLayoutContent content = parser.parse(IOUtils.buffer(new InputStreamReader(input)), uri);
        Records records = content.getRecords();
        if (records.list().isEmpty()) {
            throw new UnsupportedFormatException();
        }
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            database.loadRecordsPositionAndLength(podcast, records);
        }
//        PodcastsUtils.storeRecords(context, podcast, records.list());
        return new OnlineRecordsPaginator(podcast, records, content.getNextPage());
    }
}
