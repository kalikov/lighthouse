package ru.radiomayak.media;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import javax.net.ssl.SSLSocketFactory;

import ru.radiomayak.CacheUtils;
import ru.radiomayak.StringUtils;
import ru.radiomayak.http.Header;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRange;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpRequestParams;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpStatus;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.http.entity.ContentLengthStrategy;
import ru.radiomayak.http.impl.DefaultBHttpClientConnectionFactory;
import ru.radiomayak.http.impl.entity.LaxContentLengthStrategy;
import ru.radiomayak.http.impl.io.DefaultHttpResponseWriter;
import ru.radiomayak.http.io.HttpMessageWriter;
import ru.radiomayak.http.io.SessionOutputBuffer;
import ru.radiomayak.http.message.BasicHttpRequest;
import ru.radiomayak.http.message.BasicHttpResponse;
import ru.radiomayak.http.message.BasicStatusLine;
import ru.radiomayak.http.protocol.HTTP;
import ru.radiomayak.io.RandomAccessFileOutputStream;

class MediaProxyClientSession {
    private static final String TAG = MediaProxyClientSession.class.getSimpleName();

    private static final ContentLengthStrategy CONTENT_LENGTH_STRATEGY = new LaxContentLengthStrategy(0);

    private static final int FLUSH_BUFFER_SIZE = 256 * 1024;

    private final SessionOutputBuffer outputBuffer;
    private final int bufferSize;

    private final HttpRequest request;

    private final String category;
    private final String id;
    private final URL url;

    MediaProxyClientSession(HttpRequest request, SessionOutputBuffer outputBuffer, int bufferSize) throws IOException {
        this.request = request;
        this.outputBuffer = outputBuffer;
        this.bufferSize = bufferSize;

        URI uri = URI.create(request.getRequestLine().getUri());

        String query = StringUtils.requireNonEmpty(uri.getRawQuery());
        String charset = HttpUtils.getCharset(request);
        HttpRequestParams params = HttpUtils.parseQuery(query, StringUtils.nonEmpty(charset, HTTP.DEF_CONTENT_CHARSET.name()));

        category = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.CATEGORY_PARAMETER));
        id = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.ID_PARAMETER));
        String urlString = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.URL_PARAMETER));

        this.url = new URL(urlString);
    }

    void processRequest(MediaProxyContext context) throws HttpException {
        File file = CacheUtils.getFile(context.getCacheDir(), category, id);
        if (!streamFromFile(file)) {
            streamFromRemoteServer(context, file);
        }
    }

    private void writeResponseHeader(HttpResponse response) {
        HttpMessageWriter<HttpResponse> responseWriter = new DefaultHttpResponseWriter(outputBuffer);
        try {
            responseWriter.write(response);
            outputBuffer.flush();
        } catch (IOException | HttpException ignored) {
        }
    }

    private HttpResponse createResponse(HttpStatus status) {
        return createResponse(status.getCode(), status.getReason());
    }

    private HttpResponse createResponse(int code, String message) {
        return new BasicHttpResponse(new BasicStatusLine(request.getProtocolVersion(), code, message));
    }

    private boolean streamFromFile(File file) {
        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(source);
            if (byteMap == null) {
                return false;
            }
            HttpRange range = HttpRange.parseBounding(HttpUtils.getFirstHeader(request, HttpHeaders.RANGE));
            if (byteMap.isPartial() && (range == null || !byteMap.contains(range.getFrom(), range.getTo()))) {
                return false;
            }

            if (range == null) {
                streamFileFull(source);
            } else {
                streamFileRange(source, byteMap, range);
            }
            outputBuffer.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void streamFileFull(RandomAccessFile file) throws IOException {
        long offset = file.getFilePointer();
        long length = file.length();

        HttpResponse response = createResponse(HttpStatus.OK);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length - offset));
        writeResponseHeader(response);

        transferFileBytes(file, offset, length - offset);
    }

    private void streamFileRange(RandomAccessFile file, ByteMap byteMap, HttpRange range) throws IOException {
        int capacity = byteMap.capacity();
        int to = range.getTo() > 0 ? range.getTo() : capacity;
        int length = to - range.getFrom() + 1;

        int offset = byteMap.toOffset(range.getFrom());
        HttpResponse response = createResponse(HttpStatus.OK);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        if (capacity > 0) {
            response.setHeader(HttpHeaders.CONTENT_RANGE, range.toString() + "/" + capacity);
        } else {
            response.setHeader(HttpHeaders.CONTENT_RANGE, range.toString());
        }
        response.setStatusCode(HttpStatus.PARTIAL_CONTENT.getCode());
        writeResponseHeader(response);

        transferFileBytes(file, file.getFilePointer() + offset, length);
    }

    private void transferFileBytes(RandomAccessFile file, long offset, long count) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        file.getChannel().transferTo(offset, count, new WritableByteChannel() {
            @Override
            public int write(ByteBuffer src) throws IOException {
                int n = 0;
                while (src.remaining() > 0) {
                    int length = Math.min(src.remaining(), buffer.length);
                    src.get(buffer, 0, length);
                    outputBuffer.write(buffer, 0, length);
                    n += length;
                }
                return n;
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
            }
        });
    }

    @VisibleForTesting
    Socket openConnection(URL url) throws IOException {
        return "https".equalsIgnoreCase(url.getProtocol())
                ? SSLSocketFactory.getDefault().createSocket(url.getHost(), url.getPort() > 0 ? url.getPort() : 443)
                : new Socket(url.getHost(), url.getPort() > 0 ? url.getPort() : 80);
    }

    private void streamFromRemoteServer(MediaProxyContext context, File file) throws HttpException {
        long requestLength = CONTENT_LENGTH_STRATEGY.determineLength(request);
        if (requestLength > 0) {
            writeResponseHeader(createResponse(HttpStatus.NOT_IMPLEMENTED));
            return;
        }

        String uri = StringUtils.nonEmpty(url.getPath(), "/");
        if (!StringUtils.isEmpty(url.getQuery())) {
            uri += "?" + url.getQuery();
        }
        if (!StringUtils.isEmpty(url.getRef())) {
            uri += "#" + url.getRef();
        }
        HttpRequest remoteRequest = new BasicHttpRequest(request.getRequestLine().getMethod(), uri, request.getProtocolVersion());
        remoteRequest.addHeader(HTTP.TARGET_HOST, url.getAuthority());
        for (Header header : request.getAllHeaders()) {
            if (!HTTP.TARGET_HOST.equalsIgnoreCase(header.getName())) {
                remoteRequest.addHeader(header);
            }
        }

        Socket socket = null;
        HttpClientConnection connection;
        try {
            socket = openConnection(url);

            connection = DefaultBHttpClientConnectionFactory.INSTANCE.createConnection(socket);
            connection.sendRequestHeader(remoteRequest);
            connection.flush();
        } catch (IOException e) {
            if (socket != null) {
                IOUtils.closeQuietly(socket);
            }
            writeResponseHeader(createResponse(HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        try {
            processRemoteConnection(context, file, connection);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    private void processRemoteConnection(MediaProxyContext context, File file, HttpClientConnection connection) {
        HttpResponse response;
        HttpResponse remoteResponse;
        try {
            remoteResponse = connection.receiveResponseHeader();
            int status = remoteResponse.getStatusLine().getStatusCode();
            String message = remoteResponse.getStatusLine().getReasonPhrase();

            response = createResponse(status, message);
            setResponseHeaders(response, remoteResponse.getAllHeaders());
        } catch (IOException | HttpException e) {
            writeResponseHeader(createResponse(HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        writeResponseHeader(response);

        int status = response.getStatusLine().getStatusCode();
        if (status < 200 || status >= 400) {
            return;
        }
        int length = 0;
        try {
            length = (int) CONTENT_LENGTH_STRATEGY.determineLength(remoteResponse);
        } catch (HttpException ignored) {
        }
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
            HttpRange requestRange = HttpRange.parseBounding(HttpUtils.getFirstHeader(request, HttpHeaders.RANGE));
            Objects.requireNonNull(requestRange);
            from = requestRange.getFrom();
        } else {
            capacity = length;
        }
        try {
            connection.receiveResponseEntity(remoteResponse);
            processRemoteResponse(context, file, capacity, from, remoteResponse.getEntity().getContent(), length);
        } catch (IOException | HttpException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void setResponseHeaders(HttpResponse response, Header[] headers) {
        if (headers == null) {
            return;
        }
        for (Header header : headers) {
            String name = header.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            response.setHeader(name, header.getValue());
        }
    }

    private void processRemoteResponse(MediaProxyContext context, File file, int capacity, int from, InputStream content, int length) {
        int count = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(length, FLUSH_BUFFER_SIZE));
        try {
            byte[] buffer = new byte[bufferSize];
            int n;
            while (count < length && IOUtils.EOF != (n = content.read(buffer))) {
                if (count + n > FLUSH_BUFFER_SIZE) {
                    updateFile(context, file, capacity, from, from + count - 1, output);
                    from += count;
                    count = 0;
                    output.reset();
                }
                output.write(buffer, 0, n);
                outputBuffer.write(buffer, 0, n);
                outputBuffer.flush();
                count += n;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            updateFile(context, file, capacity, from, from + count - 1, output);
            IOUtils.closeQuietly(output);
        }
    }

    private void updateFile(MediaProxyContext context, File file, int capacity, int from, int to, ByteArrayOutputStream response) {
        ByteMap byteMap = ByteMapUtils.updateFile(file, capacity, from, to, response);
        if (byteMap != null) {
            notifyFileUpdate(context, byteMap);
        }
    }

    private void notifyFileUpdate(MediaProxyContext context, ByteMap byteMap) {
        context.notifyUpdate(category, id, byteMap.size(), byteMap.isPartial());
    }
}
