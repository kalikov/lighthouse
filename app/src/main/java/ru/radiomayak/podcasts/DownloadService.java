package ru.radiomayak.podcasts;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLSocketFactory;

import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRange;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpStatus;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.entity.ContentLengthStrategy;
import ru.radiomayak.http.impl.DefaultBHttpClientConnectionFactory;
import ru.radiomayak.http.impl.entity.LaxContentLengthStrategy;
import ru.radiomayak.http.message.BasicHttpRequest;

public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();

    public static final String ACTION_DOWNLOAD = "ru.radiomayak.podcasts.action.DOWNLOAD";

    public static final String EXTRA_PODCAST = "ru.radiomayak.podcasts.extra.PODCAST";
    public static final String EXTRA_RECORD = "ru.radiomayak.podcasts.extra.RECORD";

    private static final int BUFFER_SIZE = 100 * 1024;

    private static final ContentLengthStrategy CONTENT_LENGTH_STRATEGY = new LaxContentLengthStrategy(0);

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private DownloadNotificationManager notificationManager;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("DownloadService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        notificationManager = new DownloadNotificationManager(this);
    }

    @Override
    public void onDestroy() {
        notificationManager.stopNotification();

        mServiceLooper.quit();

        super.onDestroy();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return START_NOT_STICKY;
    }

    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        Uri uri = intent.getData();
        long podcast = intent.getLongExtra(EXTRA_PODCAST, 0);
        Record record = intent.getParcelableExtra(EXTRA_RECORD);
        if (podcast == 0 || record == null || uri == null) {
            return;
        }

        File file;
        try {
            file = new File(new URI(uri.toString()));
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }
        File directory = file.getParentFile();
        if (!directory.mkdirs()) {
            Log.w(TAG, String.format("Failed to create directory \"%s\"", directory));
        }

        if (!notificationManager.startNotification(record.getName())) {
            notificationManager.updateNotification(record.getName());
        }
        try {
            download(podcast, record, file);
        } catch (RuntimeException | IOException | HttpException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void download(long podcast, Record record, File file) throws HttpException, IOException {
        Uri uri = Uri.parse(record.getUrl());
        HttpRequest remoteRequest = new BasicHttpRequest("GET", record.getUrl(), HttpVersion.HTTP_1_1);
        remoteRequest.addHeader(HttpHeaders.HOST, uri.getAuthority());
        if (record.getFile() != null && record.getFile().getSize() > 0) {
            remoteRequest.addHeader(HttpHeaders.RANGE, new HttpRange(record.getFile().getSize(), 0).toRangeString());
        }

        Socket socket = null;
        HttpClientConnection connection;
        try {
            socket = openConnection(uri);

            connection = DefaultBHttpClientConnectionFactory.INSTANCE.createConnection(socket);
            connection.sendRequestHeader(remoteRequest);
            connection.flush();
        } catch (IOException e) {
            if (socket != null) {
                IOUtils.closeQuietly(socket);
            }
            throw e;
        }
        try {
            processConnection(podcast, connection, record, file);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    @VisibleForTesting
    Socket openConnection(Uri url) throws IOException {
        return "https".equalsIgnoreCase(url.getScheme())
                ? SSLSocketFactory.getDefault().createSocket(url.getHost(), url.getPort() > 0 ? url.getPort() : 443)
                : new Socket(url.getHost(), url.getPort() > 0 ? url.getPort() : 80);
    }

    private void processConnection(long podcast, HttpClientConnection connection, Record record, File file) throws HttpException, IOException {
        HttpResponse remoteResponse = connection.receiveResponseHeader();
        int status = remoteResponse.getStatusLine().getStatusCode();
        if (status < 200 || status >= 400) {
            throw new IOException(remoteResponse.getStatusLine().toString());
        }
        int length = (int) CONTENT_LENGTH_STRATEGY.determineLength(remoteResponse);
        if (length <= 0) {
            return;
        }
        int capacity = 0;
        int from = 0;
        HttpRange responseRange = HttpRange.parseFirst(HttpUtils.getFirstHeader(remoteResponse, HttpHeaders.CONTENT_RANGE));
        if (responseRange != null) {
            from = responseRange.getFrom();
            capacity = responseRange.getLength();
        } else if (status == HttpStatus.PARTIAL_CONTENT.getCode()) {
            from = record.getFile().getSize();
        } else {
            capacity = length;
        }
        connection.receiveResponseEntity(remoteResponse);
        processResponse(podcast, remoteResponse.getEntity().getContent(), record, file, from, capacity, length);
    }

    private void processResponse(long podcast, InputStream content, Record record, File file, int from, int capacity, int length) throws IOException {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(this);
        boolean sync = false;

        byte[] buffer = new byte[BUFFER_SIZE];
        try (RandomAccessFile output = new RandomAccessFile(file, "rw"); PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
            if (from > 0) {
                if (output.length() < from) {
                    output.setLength(from);
                }
                output.seek(from);
            }
            int n;
            int count = 0;
            while (count < length && IOUtils.EOF != (n = content.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;

                if (sync) {
                    database.storeRecordFile(podcast, record.getId(), from + count, capacity);
                } else {
                    database.beginTransaction();
                    try {
                        database.storeRecord(podcast, record);
                        database.storeRecordFile(podcast, record.getId(), from + count, capacity);
                        database.commit();
                    } finally {
                        database.endTransaction();
                    }
                    sync = true;
                }

                notificationManager.updateNotification(count, length);
            }
            output.getChannel().truncate(output.getFilePointer());
        }
        notificationManager.updateDoneNotification();
    }

    public void cancel() {
    }
}
