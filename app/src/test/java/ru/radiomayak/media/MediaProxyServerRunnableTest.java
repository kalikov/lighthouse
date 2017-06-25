package ru.radiomayak.media;

import junit.framework.Assert;

import ru.radiomayak.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;

public class MediaProxyServerRunnableTest {
    private MediaProxyServerRunnable runnable;

    @Before
    public void before() throws IOException {
        runnable = Mockito.spy(new MediaProxyServerRunnable(null, Mockito.mock(ServerSocket.class)));
    }

    public void testNoUrlNoName() throws IOException, HttpException {
        test("GET /path HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n",
                "HTTP/1.0 400 Bad Request\r\n" +
                        "\r\n");
    }

    public void testUrlNoName() throws IOException, HttpException {
        test("GET /path?url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n",
                "HTTP/1.0 400 Bad Request\r\n" +
                        "\r\n");
    }

    public void testNameNoUrl() throws IOException, HttpException {
        test("GET /path?name=foobar HTTP/1.1\r\n" +
                        "Host: 0.0.0.0=5000\r\n" +
                        "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                        "Keep-Alive: 300\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n",
                "HTTP/1.0 400 Bad Request\r\n" +
                        "\r\n");
    }

    public void testNoContentLength() throws IOException, HttpException {
        HttpURLConnection connection = mockConnection(200, "OK");
        try (InputStream input = mockInput("Hello, world!")) {
            Mockito.doReturn(input).when(connection).getInputStream();
            test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
                            "Host: 0.0.0.0=5000\r\n" +
                            "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
                            "Keep-Alive: 300\r\n" +
                            "Connection: keep-alive\r\n" +
                            "\r\n",
                    "HTTP/1.1 200 OK\r\n" +
                            "\r\n");
            Mockito.verify(connection).setRequestMethod("GET");
            Mockito.verify(connection).setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0");
            Mockito.verify(connection).setRequestProperty("Host", "www.google.com");
        }
    }

    public void testSimpleStreaming() throws IOException, HttpException {
        HttpURLConnection connection = mockConnection(200, "OK");
        try (InputStream input = mockInput("Hello, world!")) {
            Mockito.doReturn(input).when(connection).getInputStream();
            Mockito.doReturn(13).when(connection).getContentLength();
            test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
                            "Content-Length: 13\r\n" +
                            "\r\n",
                    "HTTP/1.1 200 OK\r\n" +
                            "\r\n" +
                            "Hello, world!");
            Mockito.verify(connection).setRequestMethod("GET");
            Mockito.verify(connection).setRequestProperty("Host", "www.google.com");
        }
    }

    public void testExceptionOnOpenConnection() throws IOException, HttpException {
//        Mockito.doThrow(new IOException()).when(runnable).openConnection(Mockito.any(URL.class));
        test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
                        "Content-Length: 13\r\n" +
                        "\r\n",
                "HTTP/1.1 500 Internal Server Error\r\n" +
                        "\r\n");
    }

    public void testExceptionOnConnect() throws IOException, HttpException {
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
//        Mockito.doReturn(connection).when(runnable).openConnection(Mockito.any(URL.class));
        Mockito.doThrow(new IOException()).when(connection).connect();
        test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
                        "Content-Length: 13\r\n" +
                        "\r\n",
                "HTTP/1.1 500 Internal Server Error\r\n" +
                        "\r\n");
    }

    public void testNotOkResponse() throws IOException, HttpException {
        mockConnection(404, "Not Found");
        test("GET /path?name=foobar&url=" + URLEncoder.encode("https://www.google.com", "UTF-8") + " HTTP/1.1\r\n" +
                        "Content-Length: 13\r\n" +
                        "\r\n",
                "HTTP/1.1 404 Not Found\r\n" +
                        "\r\n");
    }

    private void test(String request, String response) throws IOException, HttpException {
        try (InputStream input = mockInput(request); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Socket socket = mockSocket(input, output);
            runnable.processClient(socket);

            Assert.assertEquals(response, new String(output.toByteArray()));
        }
    }

    private Socket mockSocket(InputStream input, OutputStream output) throws IOException {
        Socket socket = Mockito.mock(Socket.class);
        Mockito.doReturn(input).when(socket).getInputStream();
        Mockito.doReturn(output).when(socket).getOutputStream();
        return socket;
    }

    private InputStream mockInput(String raw) {
        return new ByteArrayInputStream(raw.getBytes());
    }

    private HttpURLConnection mockConnection(int code, String message) throws IOException {
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
//        Mockito.doReturn(connection).when(runnable).openConnection(Mockito.any(URL.class));
        Mockito.doReturn(code).when(connection).getResponseCode();
        Mockito.doReturn(message).when(connection).getResponseMessage();
        return connection;
    }
}
