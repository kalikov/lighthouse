package ru.radiomayak.media;

import android.support.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

import ru.radiomayak.CacheUtils;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpVersion;
import ru.radiomayak.http.io.SessionOutputBuffer;
import ru.radiomayak.http.message.BasicHttpRequest;
import ru.radiomayak.http.util.CharArrayBuffer;

public class MediaProxyClientSessionTest {
    private File tempDir;

    private MediaProxyContext context;

    private ByteArrayOutputStream output;

    private String id;

    @Before
    public void before() throws IOException {
        tempDir = new File(System.getProperty("java.io.tmpdir"));

        context = Mockito.mock(MediaProxyContext.class);
        Mockito.doReturn(tempDir).when(context).getCacheDir();

        output = new ByteArrayOutputStream();

        id = UUID.randomUUID().toString();
    }

    @After
    public void after() {
//        File file = CacheUtils.getFile(tempDir, "foo", id);
//        FileUtils.deleteQuietly(file);
    }

    @Test
    public void testNoUrl() throws IOException, HttpException {
        String url = makeURL("foo", id, null);
        HttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        request.addHeader("Host", "0.0.0.0=5000");
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0");
        request.addHeader("Keep-Alive", "300");
        request.addHeader("Connection", "keep-alive");
        try {
            process(request, null);
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testNoIdentifier() throws IOException, HttpException {
        String url = makeURL("foo", null, "https://www.google.com");
        HttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        request.addHeader("Host", "0.0.0.0=5000");
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0");
        request.addHeader("Keep-Alive", "300");
        request.addHeader("Connection", "keep-alive");
        try {
            process(request, null);
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testNoCategory() throws IOException, HttpException {
        String url = makeURL(null, id, "https://www.google.com");
        HttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        request.addHeader("Host", "0.0.0.0=5000");
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0");
        request.addHeader("Keep-Alive", "300");
        request.addHeader("Connection", "keep-alive");
        try {
            process(request, null);
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testNoContentLength() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.google.com");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        request.addHeader("Host", "0.0.0.0=5000");
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0");
        request.addHeader("Keep-Alive", "300");
        request.addHeader("Connection", "keep-alive");

        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 200 OK\r\n\r\nUnused content!"), remoteOutput);

        process(request, remoteSocket);

        Assert.assertEquals("HTTP/1.1 200 OK\r\n\r\n", new String(output.toByteArray()));
        Assert.assertEquals("GET / HTTP/1.1\r\n" +
                "Host: www.google.com\r\n" +
                "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                "Keep-Alive: 300\r\n" +
                "Connection: keep-alive\r\n" +
                "\r\n", new String(remoteOutput.toByteArray()));
    }

    @Test
    public void testSimpleStreaming() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0");
        request.addHeader("Keep-Alive", "300");
        request.addHeader("Connection", "keep-alive");

        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "Hello, world!"), remoteOutput);

        process(request, remoteSocket);

        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "Hello, world!", new String(output.toByteArray()));

        Assert.assertEquals("GET /podcast/922 HTTP/1.1\r\n" +
                "Host: www.radiomayak.ru\r\n" +
                "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                "Keep-Alive: 300\r\n" +
                "Connection: keep-alive\r\n" +
                "\r\n", new String(remoteOutput.toByteArray()));
    }

    @Test
    public void testSimpleStreamingCacheWrite() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);

        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "Hello, world!"), remoteOutput);

        process(request, remoteSocket);

        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "Hello, world!", new String(output.toByteArray()));

        Assert.assertEquals("GET /podcast/922 HTTP/1.1\r\n" +
                "Host: www.radiomayak.ru\r\n" +
                "\r\n", new String(remoteOutput.toByteArray()));

//        File file = CacheUtils.getFile(tempDir, "foo", id);
//        Assert.assertTrue(file.exists());
//        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
//            ByteMap header = ByteMapUtils.readHeader(source);
//            Assert.assertNotNull(header);
//            Assert.assertFalse(header.isPartial());
//            Assert.assertEquals(13, header.capacity());
//            Assert.assertArrayEquals(new int[]{0, 12}, header.segments());
//        }
    }

    @Test
    public void testSimpleStreamingCacheRead() throws IOException, HttpException {
//        File file = CacheUtils.getFile(tempDir, "foo", id);
//        try (RandomAccessFile target = new RandomAccessFile(file, "rw")) {
//            ByteMap header = new ByteMap(13, new int[] {0, 12});
//            ByteMapUtils.writeHeader(target, header);
//            target.write("Hello, world!".getBytes());
//        }

        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);

        process(request, null);

        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "\r\n" +
                "Hello, world!", new String(output.toByteArray()));
    }

    @Test
    public void testRangeStreamingNoCapacity() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922?t=0#fragment");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        request.addHeader("Range", "bytes=4-9");

        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 206 Partial Content\r\n" +
                "Content-Length: 6\r\n" +
                "Content-Range: bytes 4-9\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "o, wor"), remoteOutput);

        process(request, remoteSocket);

        Assert.assertEquals("HTTP/1.1 206 Partial Content\r\n" +
                "Content-Length: 6\r\n" +
                "Content-Range: bytes 4-9\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "o, wor", new String(output.toByteArray()));

        Assert.assertEquals("GET /podcast/922?t=0#fragment HTTP/1.1\r\n" +
                "Host: www.radiomayak.ru\r\n" +
                "Range: bytes=4-9\r\n" +
                "\r\n", new String(remoteOutput.toByteArray()));
    }

    @Test
    public void testExceptionOnOpenConnection() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922?t=0#fragment");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);

        MediaProxyClientSession session = Mockito.spy(new MediaProxyClientSession(request, mockSessionOutputBuffer(output), 1000));
        Mockito.doThrow(new IOException()).when(session).openConnection(Mockito.any(URL.class));

        session.processRequest(context);
        Assert.assertEquals("HTTP/1.1 500 Internal Server Error\r\n" +
                "\r\n", new String(output.toByteArray()));
    }

    @Test
    public void testExceptionOnSocketUsage() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922?t=0#fragment");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);

        MediaProxyClientSession session = Mockito.spy(new MediaProxyClientSession(request, mockSessionOutputBuffer(output), 1000));
        Socket socket = Mockito.mock(Socket.class);
        Mockito.doReturn(socket).when(session).openConnection(Mockito.any(URL.class));
        Mockito.doThrow(new IOException()).when(socket).getOutputStream();

        session.processRequest(context);
        Assert.assertEquals("HTTP/1.1 500 Internal Server Error\r\n" +
                "\r\n", new String(output.toByteArray()));
    }

    @Test
    public void testNotOkResponse() throws IOException, HttpException {
        String url = makeURL("foo", id, "https://www.radiomayak.ru/podcast/922");
        BasicHttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);

        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 404 Not Found\r\n\r\n"), remoteOutput);

        process(request, remoteSocket);

        Assert.assertEquals("HTTP/1.1 404 Not Found\r\n\r\n", new String(output.toByteArray()));

        Assert.assertEquals("GET /podcast/922 HTTP/1.1\r\n" +
                "Host: www.radiomayak.ru\r\n" +
                "\r\n", new String(remoteOutput.toByteArray()));

//        File file = CacheUtils.getFile(tempDir, "foo", id);
//        Assert.assertFalse(file.exists());
    }

    private String makeURL(@Nullable String category, @Nullable String id, @Nullable String url) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder("/path");
        boolean hasParams = false;
        if (category != null) {
            builder.append('?');
            hasParams = true;
            builder.append(DefaultMediaProxyServer.CATEGORY_PARAMETER).append('=').append(category);
        }
        if (id != null) {
            builder.append(hasParams ? '&' : '?');
            hasParams = true;
            builder.append(DefaultMediaProxyServer.ID_PARAMETER).append('=').append(id);
        }
        if (url != null) {
            builder.append(hasParams ? '&' : '?');
            builder.append(DefaultMediaProxyServer.URL_PARAMETER).append('=').append(URLEncoder.encode(url, "UTF-8"));
        }
        return builder.toString();
    }

    private void process(HttpRequest request, Socket remoteSocket) throws IOException, HttpException {
        MediaProxyClientSession session = Mockito.spy(new MediaProxyClientSession(request, mockSessionOutputBuffer(output), 1000));

        if (remoteSocket == null) {
            session.processRequest(context);
            Mockito.verify(session, Mockito.never()).openConnection(Mockito.any(URL.class));
        } else {
            Mockito.doReturn(remoteSocket).when(session).openConnection(Mockito.any(URL.class));
            session.processRequest(context);
            Mockito.verify(remoteSocket).close();
        }
//        ByteArrayInputStream input = new ByteArrayInputStream(request.getBytes());
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//
//        Socket socket = mockSocket(input, output);
//        runnable.processClient(socket);

//        return new String(output.toByteArray());
    }

    private static SessionOutputBuffer mockSessionOutputBuffer(final OutputStream stream) throws IOException {
        SessionOutputBuffer buffer = Mockito.mock(SessionOutputBuffer.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                stream.write(invocation.getArgumentAt(0, int.class));
                return null;
            }
        }).when(buffer).write(Mockito.anyInt());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                stream.write(invocation.getArgumentAt(0, byte[].class));
                return null;
            }
        }).when(buffer).write(Mockito.any(byte[].class));
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                byte[] bytes = invocation.getArgumentAt(0, byte[].class);
                int offset = invocation.getArgumentAt(1, int.class);
                int length = invocation.getArgumentAt(2, int.class);
                stream.write(bytes, offset, length);
                return null;
            }
        }).when(buffer).write(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String string = invocation.getArgumentAt(0, String.class);
                stream.write(string.getBytes());
                stream.write("\r\n".getBytes());
                return null;
            }
        }).when(buffer).writeLine(Mockito.anyString());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CharArrayBuffer buffer = invocation.getArgumentAt(0, CharArrayBuffer.class);
                stream.write(buffer.toString().getBytes());
                stream.write("\r\n".getBytes());
                return null;
            }
        }).when(buffer).writeLine(Mockito.any(CharArrayBuffer.class));
        return buffer;
    }

    private static Socket mockSocket(InputStream input, OutputStream output) throws IOException {
        Socket socket = Mockito.mock(Socket.class);
        Mockito.doReturn(input).when(socket).getInputStream();
        Mockito.doReturn(output).when(socket).getOutputStream();
        return socket;
    }

    private static InputStream mockInput(String raw) {
        return new ByteArrayInputStream(raw.getBytes());
    }
}
