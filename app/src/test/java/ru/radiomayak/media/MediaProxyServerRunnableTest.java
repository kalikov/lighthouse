package ru.radiomayak.media;

import ru.radiomayak.http.HttpException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

public class MediaProxyServerRunnableTest {
    /*private MediaProxyServerRunnable runnable;

    private String id;

    @Before
    public void before() throws IOException {
        MediaProxyContext context = Mockito.mock(MediaProxyContext.class);
        runnable = Mockito.spy(new MediaProxyServerRunnable(context, Mockito.mock(ServerSocket.class)));

        id = UUID.randomUUID().toString();
    }

    @After
    public void after() {
//        File file = new File(MediaProxyServerRunnable.FILE_PREFIX + id + MediaProxyServerRunnable.FILE_SUFFIX);
//        FileUtils.deleteQuietly(file);
    }

    @Test
    public void testNoUrlNoIdentifier() throws IOException, HttpException {
        Assert.assertEquals("HTTP/1.0 400 Bad Request\r\n" +
                        "\r\n",
                process("GET /path HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n"));
        Mockito.verify(runnable, Mockito.never()).openConnection(Mockito.any(URL.class));
    }

    @Test
    public void testUrlNoIdentifier() throws IOException, HttpException {
        String url = "/path?" + DefaultMediaProxyServer.URL_PARAMETER + "=" + URLEncoder.encode("https://www.google.com", "UTF-8");
        Assert.assertEquals("HTTP/1.0 400 Bad Request\r\n" +
                        "\r\n",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n"));
        Mockito.verify(runnable, Mockito.never()).openConnection(Mockito.any(URL.class));
    }

    @Test
    public void testIdentifierNoUrl() throws IOException, HttpException {
        String url = "/path?" + DefaultMediaProxyServer.ID_PARAMETER + "=foobar";
        Assert.assertEquals("HTTP/1.0 400 Bad Request\r\n" +
                        "\r\n",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n"));
        Mockito.verify(runnable, Mockito.never()).openConnection(Mockito.any(URL.class));
    }

    @Test
    public void testNoContentLength() throws IOException, HttpException {
        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 200 OK\r\n\r\nUnused content!"), remoteOutput);

        Mockito.doReturn(remoteSocket).when(runnable).openConnection(Mockito.any(URL.class));

        String url = "/path?" + DefaultMediaProxyServer.ID_PARAMETER + "=foobar&" +
                DefaultMediaProxyServer.URL_PARAMETER + "=" + URLEncoder.encode("https://www.google.com", "UTF-8");
        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                        "\r\n",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n"));

        Assert.assertEquals("GET / HTTP/1.1\r\n" +
                        "Host: www.google.com\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n",
                new String(remoteOutput.toByteArray()));

        Mockito.verify(remoteSocket).close();
    }

    @Test
    public void testSimpleStreaming() throws IOException, HttpException {
        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "Hello, world!"), remoteOutput);

        Mockito.doReturn(remoteSocket).when(runnable).openConnection(Mockito.any(URL.class));

        String url = "/path?" + DefaultMediaProxyServer.ID_PARAMETER + "=" + id + "&" +
                DefaultMediaProxyServer.URL_PARAMETER + "=" + URLEncoder.encode("https://www.radiomayak.ru/podcast/922", "UTF-8");
        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 13\r\n" +
                        "Header: test\r\n" +
                        "\r\n" +
                        "Hello, world!",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n"));

        Assert.assertEquals("GET /podcast/922 HTTP/1.1\r\n" +
                        "Host: www.radiomayak.ru\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n",
                new String(remoteOutput.toByteArray()));

        Mockito.verify(remoteSocket).close();
    }

    @Test
    public void testSimpleStreamingCacheWrite() throws IOException, HttpException {
        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "Hello, world!"), remoteOutput);

        Mockito.doReturn(remoteSocket).when(runnable).openConnection(Mockito.any(URL.class));

        String url = "/path?" + DefaultMediaProxyServer.ID_PARAMETER + "=" + id + "&" +
                DefaultMediaProxyServer.URL_PARAMETER + "=" + URLEncoder.encode("https://www.radiomayak.ru/podcast/922", "UTF-8");
        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 13\r\n" +
                        "Header: test\r\n" +
                        "\r\n" +
                        "Hello, world!",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "\r\n"));

        Assert.assertEquals("GET /podcast/922 HTTP/1.1\r\n" +
                        "Host: www.radiomayak.ru\r\n" +
                        "\r\n",
                new String(remoteOutput.toByteArray()));

        Mockito.verify(remoteSocket).close();

        File file = new File(MediaProxyServerRunnable.FILE_PREFIX + id + MediaProxyServerRunnable.FILE_SUFFIX);
        Assert.assertTrue(file.exists());
        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
            ByteMap header = ByteMapUtils.readHeader(source);
            Assert.assertNotNull(header);
            Assert.assertFalse(header.isPartial());
            Assert.assertEquals(13, header.capacity());
            Assert.assertArrayEquals(new int[]{0, 12}, header.segments());
        }
    }

    @Test
    public void testSimpleStreamingCacheRead() throws IOException, HttpException {
        File file = new File(MediaProxyServerRunnable.FILE_PREFIX + id + MediaProxyServerRunnable.FILE_SUFFIX);
        try (RandomAccessFile target = new RandomAccessFile(file, "rw")) {
            ByteMap header = new ByteMap(13, new int[] {0, 12});
            ByteMapUtils.writeHeader(target, header);
            target.write("Hello, world!".getBytes());
        }

        String url = "/path?" + DefaultMediaProxyServer.ID_PARAMETER + "=" + id + "&" +
                DefaultMediaProxyServer.URL_PARAMETER + "=" + URLEncoder.encode("https://www.radiomayak.ru/podcast/922", "UTF-8");
        Assert.assertEquals("HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 13\r\n" +
                        "\r\n" +
                        "Hello, world!",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "\r\n"));

        Mockito.verify(runnable, Mockito.never()).openConnection(Mockito.any(URL.class));
    }

    @Test
    public void testRangeStreamingNoCapacity() throws IOException, HttpException {
        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
        Socket remoteSocket = mockSocket(mockInput("HTTP/1.1 206 Partial Content\r\n" +
                "Content-Length: 6\r\n" +
                "Content-Range: bytes 4-9\r\n" +
                "Header: test\r\n" +
                "\r\n" +
                "o, wor"), remoteOutput);

        Mockito.doReturn(remoteSocket).when(runnable).openConnection(Mockito.any(URL.class));

        String url = "/path?" + DefaultMediaProxyServer.ID_PARAMETER + "=" + id + "&" +
                DefaultMediaProxyServer.URL_PARAMETER + "=" + URLEncoder.encode("https://www.radiomayak.ru/podcast/922?t=0#fragment", "UTF-8");
        Assert.assertEquals("HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Length: 6\r\n" +
                        "Content-Range: bytes 4-9\r\n" +
                        "Header: test\r\n" +
                        "\r\n" +
                        "o, wor",
                process("GET " + url + " HTTP/1.1\r\n" +
                        "Range: bytes=4-9\r\n" +
                        "\r\n"));

        Assert.assertEquals("GET /podcast/922?t=0#fragment HTTP/1.1\r\n" +
                        "Host: www.radiomayak.ru\r\n" +
                        "Range: bytes=4-9\r\n" +
                        "\r\n",
                new String(remoteOutput.toByteArray()));

        Mockito.verify(remoteSocket).close();
    }

    @Test
    public void testExceptionOnOpenConnection() throws IOException, HttpException {
//        Mockito.doThrow(new IOException()).when(runnable).openConnection(Mockito.any(URL.class));
//        test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
//                        "Content-Length: 13\r\n" +
//                        "\r\n",
//                "HTTP/1.1 500 Internal Server Error\r\n" +
//                        "\r\n");
    }

    @Test
    public void testExceptionOnConnect() throws IOException, HttpException {
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
//        Mockito.doReturn(connection).when(runnable).openConnection(Mockito.any(URL.class));
        Mockito.doThrow(new IOException()).when(connection).connect();
//        test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
//                        "Content-Length: 13\r\n" +
//                        "\r\n",
//                "HTTP/1.1 500 Internal Server Error\r\n" +
//                        "\r\n");
    }

    @Test
    public void testNotOkResponse() throws IOException, HttpException {
//        mockConnection(404, "Not Found");
//        test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
//                        "Content-Length: 13\r\n" +
//                        "\r\n",
//                "HTTP/1.1 404 Not Found\r\n" +
//                        "\r\n");
    }

    private String process(String request) throws IOException, HttpException {
        ByteArrayInputStream input = new ByteArrayInputStream(request.getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Socket socket = mockSocket(input, output);
        runnable.processClient(socket);

        return new String(output.toByteArray());
    }

    private Socket mockSocket(InputStream input, OutputStream output) throws IOException {
        Socket socket = Mockito.mock(Socket.class);
        Mockito.doReturn(input).when(socket).getInputStream();
        Mockito.doReturn(output).when(socket).getOutputStream();
        return socket;
    }

    private InputStream mockInput(String raw) {
        return new ByteArrayInputStream(raw.getBytes());
    }*/
}
