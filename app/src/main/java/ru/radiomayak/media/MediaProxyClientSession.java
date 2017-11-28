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
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.ProtocolVersion;
import ru.radiomayak.http.impl.DefaultBHttpClientConnectionFactory;
import ru.radiomayak.http.impl.io.DefaultHttpRequestParser;
import ru.radiomayak.http.impl.io.DefaultHttpResponseWriter;
import ru.radiomayak.http.io.HttpMessageParser;
import ru.radiomayak.http.io.HttpMessageWriter;
import ru.radiomayak.http.io.SessionInputBuffer;
import ru.radiomayak.http.io.SessionOutputBuffer;
import ru.radiomayak.http.message.BasicHttpRequest;
import ru.radiomayak.http.message.BasicHttpResponse;
import ru.radiomayak.http.message.BasicStatusLine;
import ru.radiomayak.http.protocol.HTTP;
import ru.radiomayak.io.RandomAccessFileOutputStream;

class MediaProxyClientSession {
    final SessionInputBuffer inputBuffer;
    final SessionOutputBuffer outputBuffer;

    final HttpRequest request;

    final String category;
    final String id;
    final URL url;
    final File file;

    volatile ByteMap byteMap;
    volatile int from;
    volatile int length;

    MediaProxyClientSession(SessionInputBuffer inputBuffer, SessionOutputBuffer outputBuffer, File dir) throws IOException, HttpException {
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;

        request = parseRequest(inputBuffer);
        URI uri = URI.create(request.getRequestLine().getUri());

        String query = StringUtils.requireNonEmpty(uri.getRawQuery());
        String charset = HttpUtils.getCharset(request);
        HttpRequestParams params = HttpUtils.parseQuery(query, StringUtils.nonEmpty(charset, HTTP.DEF_CONTENT_CHARSET.name()));

        category = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.CATEGORY_PARAMETER));
        id = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.ID_PARAMETER));
        String urlString = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.URL_PARAMETER));

        this.url = new URL(urlString);

        file = CacheUtils.getFile(dir, 0, id);
    }

    private static HttpRequest parseRequest(SessionInputBuffer buffer) throws IOException, HttpException {
        HttpMessageParser<HttpRequest> parser = new DefaultHttpRequestParser(buffer);
        return parser.parse();
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
        return createResponse(HttpVersion.HTTP_1_0, status);
    }

    private HttpResponse createResponse(ProtocolVersion protocol, HttpStatus status) {
        return createResponse(protocol, status.getCode(), status.getReason());
    }

    private HttpResponse createResponse(ProtocolVersion protocol, int code, String message) {
        return new BasicHttpResponse(new BasicStatusLine(protocol, code, message));
    }

    boolean streamFromFile() {
        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
            byteMap = ByteMapUtils.readHeader(source);
            if (byteMap == null) {
                return false;
            }
            HttpRange range = HttpRange.parseBounding(HttpUtils.getFirstHeader(request, HttpHeaders.RANGE));
            if (byteMap.isPartial() && (range == null || !byteMap.contains(range.getFrom(), range.getTo()))) {
                return false;
            }

            HttpResponse response = createResponse(request.getProtocolVersion(), HttpStatus.OK);
            if (range == null) {
                streamFile(response, source);
            } else {
                streamFileRange(response, source, range);
            }
            outputBuffer.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void streamFile(HttpResponse response, RandomAccessFile file) throws IOException {
        long offset = file.getFilePointer();
        long length = file.length();

        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length - offset));
        writeResponseHeader(response);

        transferFileBytes(file, offset, length - offset);
    }

    private void streamFileRange(HttpResponse response, RandomAccessFile file, HttpRange range) throws IOException {
        int capacity = byteMap.capacity();
        int to = range.getTo() > 0 ? range.getTo() : capacity;
        int length = to - range.getFrom() + 1;

        int offset = byteMap.toOffset(range.getFrom());
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
        final byte[] buffer = new byte[BUFFER_SIZE];
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

    void processRemoteRequest() throws HttpException {
        long requestLength = CONTENT_LENGTH_STRATEGY.determineLength(request);
        if (requestLength > 0) {
            writeResponseHeader(createResponse(request.getProtocolVersion(), HttpStatus.NOT_IMPLEMENTED));
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
            writeResponseHeader(createResponse(request.getProtocolVersion(), HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        try {
            processConnection(connection);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    private void processConnection(HttpClientConnection connection) {
        HttpResponse response;
        HttpResponse remoteResponse;
        try {
            remoteResponse = connection.receiveResponseHeader();
            int status = remoteResponse.getStatusLine().getStatusCode();
            String message = remoteResponse.getStatusLine().getReasonPhrase();

            response = createResponse(request.getProtocolVersion(), status, message);
            setResponseHeaders(response, remoteResponse.getAllHeaders());
        } catch (IOException | HttpException e) {
            writeResponseHeader(createResponse(request.getProtocolVersion(), HttpStatus.INTERNAL_SERVER_ERROR));
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
        if (byteMap == null) {
            byteMap = new ByteMap(capacity);
        }
        try {
            connection.receiveResponseEntity(remoteResponse);
            processResponse(capacity, from, remoteResponse.getEntity().getContent(), length);
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

    private void processResponse(int capacity, int from, InputStream content, int length) {
        int count = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream(length);
        try {
            from = from;
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (count < length && IOUtils.EOF != (n = content.read(buffer))) {
                output.write(buffer, 0, n);
                outputBuffer.write(buffer, 0, n);
                outputBuffer.flush();
                count += n;
                this.length += n;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            processBytesMapUpdate(capacity, from, from + count - 1, output);
            IOUtils.closeQuietly(output);
        }
    }

    private void processBytesMapUpdate(int capacity, int from, int to, ByteArrayOutputStream response) {
        File fileCopy = null;
        try (RandomAccessFile target = new RandomAccessFile(file, "rw")) {
            ByteMap byteMap = ByteMapUtils.readHeader(target);
            if (byteMap == null) {
                byteMap = new ByteMap(capacity, from, to);
                target.seek(0);
                ByteMapUtils.writeHeader(target, byteMap);
                response.writeTo(new RandomAccessFileOutputStream(target));
                target.getChannel().truncate(target.getFilePointer());
                notifyByteMapUpdate(byteMap);
                return;
            }
            int overlap = byteMap.merge(from, to);
            if (overlap < 0) {
                return;
            }
            if (capacity > 0) {
                byteMap.capacity(capacity);
            }
            int offset = byteMap.toOffset(from);

            fileCopy = new File(session.file.getAbsolutePath() + ".tmp");
            FileUtils.copyFile(session.file, fileCopy);

            long sourceBytesOrigin = target.getFilePointer();

            try (RandomAccessFile copy = new RandomAccessFile(fileCopy, "r")) {
                ByteMapUtils.writeHeader(target, byteMap);
                copy.getChannel().transferTo(target.getFilePointer(), offset, target.getChannel());
                response.writeTo(new RandomAccessFileOutputStream(target));
                copy.seek(sourceBytesOrigin + offset + overlap);
                copy.getChannel().transferTo(target.getFilePointer(), copy.length() - copy.getFilePointer(), target.getChannel());
                target.getChannel().truncate(target.getFilePointer());
            }
            notifyByteMapUpdate(byteMap);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (fileCopy != null) {
                FileUtils.deleteQuietly(fileCopy);
            }
        }
    }

    private void notifyByteMapUpdate(ByteMap byteMap) {
        context.notifyUpdate(category, id, byteMap.size(), byteMap.isPartial());
    }

//    public int getSize() {
//        if (byteMap.capacity() <= 0) {
//            return 0;
//        }
//        if (length <= 0) {
//            return (int)(byteMap.size() * 100L / byteMap.capacity());
//        }
//        return (int)(byteMap.size(from, from + length - 1) * 100L / byteMap.capacity());
//    }
}
