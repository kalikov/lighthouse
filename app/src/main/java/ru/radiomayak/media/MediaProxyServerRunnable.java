package ru.radiomayak.media;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import ru.radiomayak.CacheUtils;
import ru.radiomayak.http.Header;
import ru.radiomayak.http.HttpClientConnection;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpHeaders;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpResponse;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.ProtocolVersion;
import ru.radiomayak.http.entity.ContentLengthStrategy;
import ru.radiomayak.http.impl.DefaultBHttpClientConnectionFactory;
import ru.radiomayak.http.impl.entity.LaxContentLengthStrategy;
import ru.radiomayak.http.impl.io.DefaultHttpRequestParser;
import ru.radiomayak.http.impl.io.DefaultHttpResponseWriter;
import ru.radiomayak.http.impl.io.HttpTransportMetricsImpl;
import ru.radiomayak.http.impl.io.SessionInputBufferImpl;
import ru.radiomayak.http.impl.io.SessionOutputBufferImpl;
import ru.radiomayak.http.io.HttpMessageParser;
import ru.radiomayak.http.io.HttpMessageWriter;
import ru.radiomayak.http.io.SessionInputBuffer;
import ru.radiomayak.http.io.SessionOutputBuffer;
import ru.radiomayak.http.message.BasicHttpRequest;
import ru.radiomayak.http.message.BasicHttpResponse;
import ru.radiomayak.http.message.BasicStatusLine;
import ru.radiomayak.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import javax.net.ssl.SSLSocketFactory;

import ru.radiomayak.StringUtils;
import ru.radiomayak.http.HttpRange;
import ru.radiomayak.http.HttpRequestParams;
import ru.radiomayak.http.HttpStatus;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.io.RandomAccessFileOutputStream;

class MediaProxyServerRunnable implements Runnable {
    private static final String TAG = MediaProxyServerRunnable.class.getSimpleName();

    private static final ContentLengthStrategy CONTENT_LENGTH_STRATEGY = new LaxContentLengthStrategy(0);

    private static final int BUFFER_SIZE = 10 * 1024;

    private final MediaProxyContext context;
    private final ServerSocket socket;

    private volatile MediaProxyClientSession session;

    MediaProxyServerRunnable(MediaProxyContext context, ServerSocket socket) {
        this.context = context;
        this.socket = socket;
    }

    public MediaProxyClientSession getSession() {
        return session;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket client = socket.accept();
                try {
                    processClient(client);
                } finally {
                    IOUtils.closeQuietly(client);
                }
            } catch (HttpException | IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    @VisibleForTesting
    void processClient(Socket client) throws IOException, HttpException {
        try (InputStream input = client.getInputStream(); OutputStream output = client.getOutputStream()) {
            HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();

            SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(metrics, BUFFER_SIZE);
            inBuffer.bind(input);

            SessionOutputBufferImpl outBuffer = new SessionOutputBufferImpl(metrics, BUFFER_SIZE);
            outBuffer.bind(output);

            processClient(inBuffer, outBuffer);
        }
    }

    private void processClient(SessionInputBuffer inputBuffer, SessionOutputBuffer outputBuffer) {
        try {
            session = new MediaProxyClientSession(inputBuffer, outputBuffer, context.getCacheDir());
            if (!streamFromFile(session)) {
                processRemoteRequest(session);
            }
        } catch (MalformedURLException e) {
            writeResponseHeader(outputBuffer, createResponse(HttpStatus.NOT_FOUND));
        } catch (IOException | HttpException | IllegalArgumentException e) {
            writeResponseHeader(outputBuffer, createResponse(HttpStatus.BAD_REQUEST));
        }
    }

    private HttpRequest parseRequest(SessionInputBuffer buffer) throws IOException, HttpException {
        HttpMessageParser<HttpRequest> parser = new DefaultHttpRequestParser(buffer);
        return parser.parse();
    }

    private void writeResponseHeader(SessionOutputBuffer outBuffer, HttpResponse response) {
        HttpMessageWriter<HttpResponse> responseWriter = new DefaultHttpResponseWriter(outBuffer);
        try {
            responseWriter.write(response);
            outBuffer.flush();
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

    private boolean streamFromFile(MediaProxyClientSession session) {
        try (RandomAccessFile source = new RandomAccessFile(session.file, "r")) {
            ByteMap byteMap = ByteMapUtils.readHeader(source);
            session.byteMap = byteMap;
            if (byteMap == null) {
                return false;
            }
            HttpRange range = HttpRange.parseBounding(HttpUtils.getFirstHeader(session.request, HttpHeaders.RANGE));
            if (byteMap.isPartial() && (range == null || !byteMap.contains(range.getFrom(), range.getTo()))) {
                return false;
            }

            HttpResponse response = createResponse(session.request.getProtocolVersion(), HttpStatus.OK);
            if (range == null) {
                streamFile(session, response, source);
            } else {
                streamFileRange(session, response, source, range);
            }
            session.outputBuffer.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void streamFile(MediaProxyClientSession session, HttpResponse response, RandomAccessFile file) throws IOException {
        long offset = file.getFilePointer();
        long length = file.length();

        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length - offset));
        writeResponseHeader(session.outputBuffer, response);

        transferFileBytes(session, file, offset, length - offset);
    }

    private void streamFileRange(MediaProxyClientSession session, HttpResponse response, RandomAccessFile file, HttpRange range) throws IOException {
        int capacity = session.byteMap.capacity();
        int to = range.getTo() > 0 ? range.getTo() : capacity;
        int length = to - range.getFrom() + 1;

        int offset = session.byteMap.toOffset(range.getFrom());
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        if (capacity > 0) {
            response.setHeader(HttpHeaders.CONTENT_RANGE, range.toString() + "/" + capacity);
        } else {
            response.setHeader(HttpHeaders.CONTENT_RANGE, range.toString());
        }
        response.setStatusCode(HttpStatus.PARTIAL_CONTENT.getCode());
        writeResponseHeader(session.outputBuffer, response);

        transferFileBytes(session, file, file.getFilePointer() + offset, length);
    }

    private static void transferFileBytes(final MediaProxyClientSession session, RandomAccessFile file, long offset, long count) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        file.getChannel().transferTo(offset, count, new WritableByteChannel() {
            @Override
            public int write(ByteBuffer src) throws IOException {
                int n = 0;
                while (src.remaining() > 0) {
                    int length = Math.min(src.remaining(), buffer.length);
                    src.get(buffer, 0, length);
                    session.outputBuffer.write(buffer, 0, length);
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

    private void processRemoteRequest(MediaProxyClientSession session) throws HttpException {
        long requestLength = CONTENT_LENGTH_STRATEGY.determineLength(session.request);
        if (requestLength > 0) {
            writeResponseHeader(session.outputBuffer, createResponse(session.request.getProtocolVersion(), HttpStatus.NOT_IMPLEMENTED));
            return;
        }

        String uri = StringUtils.nonEmpty(session.url.getPath(), "/");
        if (!StringUtils.isEmpty(session.url.getQuery())) {
            uri += "?" + session.url.getQuery();
        }
        if (!StringUtils.isEmpty(session.url.getRef())) {
            uri += "#" + session.url.getRef();
        }
        HttpRequest remoteRequest = new BasicHttpRequest(session.request.getRequestLine().getMethod(), uri, session.request.getProtocolVersion());
        remoteRequest.addHeader(HTTP.TARGET_HOST, session.url.getAuthority());
        for (Header header : session.request.getAllHeaders()) {
            if (!HTTP.TARGET_HOST.equalsIgnoreCase(header.getName())) {
                remoteRequest.addHeader(header);
            }
        }

        Socket socket = null;
        HttpClientConnection connection;
        try {
            socket = openConnection(session.url);

            connection = DefaultBHttpClientConnectionFactory.INSTANCE.createConnection(socket);
            connection.sendRequestHeader(remoteRequest);
            connection.flush();
        } catch (IOException e) {
            if (socket != null) {
                IOUtils.closeQuietly(socket);
            }
            writeResponseHeader(session.outputBuffer, createResponse(session.request.getProtocolVersion(), HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        try {
            processConnection(session, session.file, session.request, connection);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    private void processConnection(MediaProxyClientSession session, File file, HttpRequest request, HttpClientConnection connection) {
        HttpResponse response;
        HttpResponse remoteResponse;
        try {
            remoteResponse = connection.receiveResponseHeader();
            int status = remoteResponse.getStatusLine().getStatusCode();
            String message = remoteResponse.getStatusLine().getReasonPhrase();

            response = createResponse(request.getProtocolVersion(), status, message);
            setResponseHeaders(response, remoteResponse.getAllHeaders());
        } catch (IOException | HttpException e) {
            writeResponseHeader(session.outputBuffer, createResponse(request.getProtocolVersion(), HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        writeResponseHeader(session.outputBuffer, response);

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
        if (session.byteMap == null) {
            session.byteMap = new ByteMap(capacity);
        }
        try {
            connection.receiveResponseEntity(remoteResponse);
            processResponse(session, file, capacity, from, remoteResponse.getEntity().getContent(), length);
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

    private void processResponse(MediaProxyClientSession session, File file, int capacity, int from, InputStream content, int length) {
        int count = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream(length);
        try {
            session.from = from;
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (count < length && IOUtils.EOF != (n = content.read(buffer))) {
                output.write(buffer, 0, n);
                session.outputBuffer.write(buffer, 0, n);
                session.outputBuffer.flush();
                count += n;
                session.length += n;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            processBytesMapUpdate(session, capacity, from, from + count - 1, output);
            IOUtils.closeQuietly(output);
        }
    }

    private void processBytesMapUpdate(MediaProxyClientSession session, int capacity, int from, int to, ByteArrayOutputStream response) {
        File fileCopy = null;
        try (RandomAccessFile target = new RandomAccessFile(session.file, "rw")) {
            ByteMap byteMap = ByteMapUtils.readHeader(target);
            if (byteMap == null) {
                byteMap = new ByteMap(capacity, from, to);
                target.seek(0);
                ByteMapUtils.writeHeader(target, byteMap);
                response.writeTo(new RandomAccessFileOutputStream(target));
                target.getChannel().truncate(target.getFilePointer());
                notifyByteMapUpdate(session, byteMap);
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
            notifyByteMapUpdate(session, byteMap);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (fileCopy != null) {
                FileUtils.deleteQuietly(fileCopy);
            }
        }
    }

    private void notifyByteMapUpdate(MediaProxyClientSession session, ByteMap byteMap) {
        context.notifyUpdate(session.category, session.id, byteMap.size(), byteMap.isPartial());
    }
}
