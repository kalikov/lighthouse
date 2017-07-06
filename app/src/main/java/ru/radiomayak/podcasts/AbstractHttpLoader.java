package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.content.Loader;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpUtils;

abstract class AbstractHttpLoader<T> extends Loader<T> {
    private static final int BUFFER_SIZE = 10 * 1024;

    protected AbstractHttpLoader(Context context) {
        super(context);
    }

    protected final HttpResponse getResponse(HttpClientConnection connection, HttpRequest request) throws IOException, HttpException {
        connection.setSocketTimeout(NetworkUtils.getRequestTimeout());
        connection.sendRequestHeader(request);
        connection.flush();
        return connection.receiveResponseHeader();
    }

    @Nullable
    protected final HttpResponse getOkResponse(HttpClientConnection connection, HttpRequest request) throws IOException, HttpException {
        HttpResponse response = getResponse(connection, request);
        int status = response.getStatusLine().getStatusCode();
        if (status < 200 || status >= 400) {
            return null;
        }
        return response;
    }

    @Nullable
    protected final HttpResponse getEntityResponse(HttpClientConnection connection, HttpRequest request) throws IOException, HttpException {
        HttpResponse response = getOkResponse(connection, request);
        if (response == null) {
            return null;
        }
        connection.receiveResponseEntity(response);
        if (response.getEntity() == null || response.getEntity().getContentLength() == 0) {
            return null;
        }
        return response;
    }

    @Nullable
    protected final byte[] getResponseBytes(HttpResponse response) throws IOException, HttpException {
        try (InputStream stream = HttpUtils.getContent(response.getEntity())) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int n;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (IOUtils.EOF != (n = stream.read(buffer)) && !isInterrupted()) {
                output.write(buffer, 0, n);
            }
            return isInterrupted() ? null : output.toByteArray();
        }
    }

    private boolean isInterrupted() {
        return isCancelled() || Thread.currentThread().isInterrupted();
    }
}
