package ru.radiomayak.media;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.apache.commons.io.IOUtils;

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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLSocketFactory;

import ru.radiomayak.StringUtils;
import ru.radiomayak.http.HttpRange;
import ru.radiomayak.http.HttpRequestParams;
import ru.radiomayak.http.HttpStatus;
import ru.radiomayak.http.HttpUtils;

class MediaProxyServerRunnable implements Runnable {
    private static final String TAG = MediaProxyServerRunnable.class.getSimpleName();

    private static final ContentLengthStrategy CONTENT_LENGTH_STRATEGY = new LaxContentLengthStrategy(0);

    private final StorageProvider storageProvider;
    private final ServerSocket socket;

    private volatile MediaProxyClientSession session;

    MediaProxyServerRunnable(StorageProvider storageProvider, ServerSocket socket) {
        this.storageProvider = storageProvider;
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

            SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(metrics, 10 * 1024);
            inBuffer.bind(input);

            SessionOutputBufferImpl outBuffer = new SessionOutputBufferImpl(metrics, 10 * 1024);
            outBuffer.bind(output);

            session = new MediaProxyClientSession(inBuffer, outBuffer);
            processClientSession(session);
        }
    }

    private void processClientSession(MediaProxyClientSession session) {
        try {
            HttpRequest request = parseRequest(session.inputBuffer);
            URI uri = URI.create(request.getRequestLine().getUri());

            String query = StringUtils.requireNonEmpty(uri.getRawQuery());
            String charset = HttpUtils.getCharset(request);
            HttpRequestParams params = HttpUtils.parseQuery(query, StringUtils.nonEmpty(charset, "ISO-8859-1"));

            String name = StringUtils.requireNonEmpty(params.getFirst("name"));
            String url = StringUtils.requireNonEmpty(params.getFirst("url"));

            session.name = name;

            if (!streamFromFile(name, request, session)) {
                processRequest(name, request, new URL(url), session);
            }
        } catch (MalformedURLException | IllegalArgumentException e) {
            writeResponseHeader(session.outputBuffer, createResponse(HttpStatus.NOT_FOUND));
        } catch (IOException | HttpException e) {
            writeResponseHeader(session.outputBuffer, createResponse(HttpStatus.BAD_REQUEST));
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

    private boolean streamFromFile(String name, HttpRequest request, final MediaProxyClientSession session) {
        File file = new File(storageProvider.getStorage(), name + ".bin");

        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
            BytesMap bytesMap = BytesMapUtils.readHeader(source);
            session.bytesMap = bytesMap;
            if (bytesMap == null) {
                return false;
            }
            HttpRange range = HttpRange.parseBounding(HttpUtils.getFirstHeader(request, HttpHeaders.RANGE));
            if (bytesMap.isPartial() && (range == null || !bytesMap.contains(range.getFrom(), range.getTo()))) {
                return false;
            }
            HttpResponse response = createResponse(request.getProtocolVersion(), HttpStatus.OK);
            if (range == null) {
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytesMap.capacity()));
                writeResponseHeader(session.outputBuffer, response);
                source.getChannel().transferTo(source.getFilePointer(), bytesMap.capacity(), new WritableByteChannel() {
                    @Override
                    public int write(ByteBuffer src) throws IOException {
                        int n = src.remaining();
                        byte[] buffer = new byte[1024 * 10];
                        while (src.remaining() > 0) {
                            int length = Math.min(src.remaining(), buffer.length);
                            src.get(buffer, 0, length);
                            session.outputBuffer.write(buffer, 0, length);
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
                session.outputBuffer.flush();
                return true;
            } else {
                int to = range.getTo() > 0 ? range.getTo() : bytesMap.capacity();
                int length = to - range.getFrom() + 1;
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
                if (bytesMap.capacity() > 0) {
                    response.setHeader(HttpHeaders.CONTENT_RANGE, range.toString() + "/" + bytesMap.capacity());
                } else {
                    response.setHeader(HttpHeaders.CONTENT_RANGE, range.toString());
                }
                response.setStatusCode(HttpStatus.PARTIAL_CONTENT.getCode());
                writeResponseHeader(session.outputBuffer, response);
                int offset = bytesMap.toOffset(range.getFrom());
                source.getChannel().transferTo(source.getFilePointer() + offset, length, new WritableByteChannel() {
                    @Override
                    public int write(ByteBuffer src) throws IOException {
                        int n = src.remaining();
                        byte[] buffer = new byte[1024 * 10];
                        while (src.remaining() > 0) {
                            int length = Math.min(src.remaining(), buffer.length);
                            src.get(buffer, 0, length);
                            session.outputBuffer.write(buffer, 0, length);
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
                session.outputBuffer.flush();
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    private void processRequest(String name, HttpRequest request, URL url, MediaProxyClientSession session) throws HttpException {
        request.removeHeaders(HTTP.TARGET_HOST);
        request.addHeader(HTTP.TARGET_HOST, url.getAuthority());

        HttpRequest connRequest = new BasicHttpRequest(request.getRequestLine().getMethod(), url.toString(), request.getProtocolVersion());
        for (Header header : request.getAllHeaders()) {
            connRequest.addHeader(header);
        }

        long requestLength = CONTENT_LENGTH_STRATEGY.determineLength(request);

//        HttpURLConnection connection;
        HttpClientConnection connection;
        try {
//            HttpRequestExecutor executor = new HttpRequestExecutor();
            Socket socket = "https".equalsIgnoreCase(url.getProtocol())
                    ? SSLSocketFactory.getDefault().createSocket(url.getHost(), url.getPort() > 0 ? url.getPort() : 443)
                    : new Socket(url.getHost(), url.getPort() > 0 ? url.getPort() : 80);

            connection = DefaultBHttpClientConnectionFactory.INSTANCE.createConnection(socket);
            connection.sendRequestHeader(connRequest);
//            connection = openConnection(request, url);
        } catch (IOException e) {
            writeResponseHeader(session.outputBuffer, createResponse(request.getProtocolVersion(), HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        try {
            if (requestLength == 0) {
                processConnectionResponse(name, request, connection, session);
            } else {
                processConnection(name, request, connection, session);
            }
        } finally {
//            connection.disconnect();
            IOUtils.closeQuietly(connection);
        }
    }

    private void processConnection(String name, HttpRequest request, HttpClientConnection connection, MediaProxyClientSession session) {
//        connection.setDoOutput(true);
//        Thread requestStreamingThread = new Thread(new RequestStreamingRunnable(requestLength, session.inputBuffer, connection));
//        requestStreamingThread.setName("MediaProxy Request Streaming Thread");
//        requestStreamingThread.start();

        try {
            processConnectionResponse(name, request, connection, session);
        } finally {
//            requestStreamingThread.interrupt();
//            try {
//                requestStreamingThread.join();
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }
    }

    private void processConnectionResponse(String name, HttpRequest request, HttpClientConnection connection, MediaProxyClientSession session) {
        HttpResponse response;
        HttpResponse connResponse;
        try {
            connection.flush();
            connResponse = connection.receiveResponseHeader();
            int status = connResponse.getStatusLine().getStatusCode();
            String message = connResponse.getStatusLine().getReasonPhrase();

            response = createResponse(request.getProtocolVersion(), status, message);
            setResponseHeaders(response, connResponse.getAllHeaders());
        } catch (IOException | HttpException e) {
            writeResponseHeader(session.outputBuffer, createResponse(request.getProtocolVersion(), HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }
        writeResponseHeader(session.outputBuffer, response);

        int status = response.getStatusLine().getStatusCode();
        if (status < 200 || status >= 400) {
            return;
        }
        int length = 0;// connection.getContentLength();
        try {
            length = (int) CONTENT_LENGTH_STRATEGY.determineLength(connResponse);
        } catch (HttpException ignored) {
        }
        if (length <= 0) {
            return;
        }
        int capacity = 0;
        int from = 0;
        HttpRange responseRange = HttpRange.parseFirst(HttpUtils.getFirstHeader(connResponse, HttpHeaders.CONTENT_RANGE));
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
        if (session.bytesMap == null) {
            session.bytesMap = new BytesMap(capacity);
        }
        try {
            connection.receiveResponseEntity(connResponse);
            processResponse(name, capacity, from, connResponse.getEntity().getContent()/*connection.getInputStream()*/, length, session);
        } catch (IOException | HttpException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void setResponseHeaders(HttpResponse response, Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isEmpty()) {
                continue;
            }
            for (String value : entry.getValue()) {
                response.setHeader(name, value);
            }
        }
    }

    private void setResponseHeaders(HttpResponse response, Header[] headers) {
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

    private void processResponse(String name, int capacity, int from, InputStream content, int length, MediaProxyClientSession session) {
        int count = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream(length);
        try {
            session.from = from;
            byte[] buffer = new byte[1024 * 10];
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
            processBytesMapUpdate(name, capacity, from, from + count - 1, output);
            IOUtils.closeQuietly(output);
        }
    }

    private void processBytesMapUpdate(String name, int capacity, int from, int to, ByteArrayOutputStream response) {
        File sourceFile = new File(storageProvider.getStorage(), name + ".bin");

        File targetFile;
        try (RandomAccessFile source = new RandomAccessFile(sourceFile, "rw")) {
            BytesMap bytesMap = BytesMapUtils.readHeader(source);
            if (bytesMap == null) {
                bytesMap = new BytesMap(capacity, from, to);
                source.seek(0);
                BytesMapUtils.writeHeader(source, bytesMap);
                response.writeTo(new RandomAccessFileOutputStream(source));
                source.getChannel().truncate(source.getFilePointer());
                return;
            }
            int overlap = bytesMap.merge(from, to);
            if (overlap < 0) {
                return;
            }
            if (capacity > 0) {
                bytesMap.capacity(capacity);
            }
            int offset = bytesMap.toOffset(from);

            targetFile = new File(storageProvider.getStorage(), name + ".tmp");
            if (!targetFile.delete()) {
                Log.w(TAG, "Failed to remove copy");
            }

            long sourceBytesOrigin = source.getFilePointer();

            try (RandomAccessFile target = new RandomAccessFile(targetFile, "rw")) {
                BytesMapUtils.writeHeader(target, bytesMap);
                source.getChannel().transferTo(target.getFilePointer(), offset, target.getChannel());
                response.writeTo(new RandomAccessFileOutputStream(target));
                source.seek(sourceBytesOrigin + offset + overlap);
                source.getChannel().transferTo(target.getFilePointer(), source.length() - source.getFilePointer(), target.getChannel());
                target.getChannel().truncate(target.getFilePointer());
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }
        if (!sourceFile.delete()) {
            Log.w(TAG, "");
            if (!targetFile.delete()) {
                Log.w(TAG, "");
            }
        } else if (!targetFile.renameTo(sourceFile)) {
            Log.w(TAG, "");
        }
    }
}
