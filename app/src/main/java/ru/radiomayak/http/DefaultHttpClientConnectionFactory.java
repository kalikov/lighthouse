package ru.radiomayak.http;

import ru.radiomayak.http.impl.DefaultBHttpClientConnectionFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.SSLSocketFactory;

public class DefaultHttpClientConnectionFactory implements HttpClientConnectionFactory {
    public static DefaultHttpClientConnectionFactory INSTANCE = new DefaultHttpClientConnectionFactory();

    @Override
    public HttpClientConnection openConnection(URL url) throws IOException {
        Socket socket = "https".equalsIgnoreCase(url.getProtocol())
                ? SSLSocketFactory.getDefault().createSocket(url.getHost(), url.getPort() > 0 ? url.getPort() : 443)
                : new Socket(url.getHost(), url.getPort() > 0 ? url.getPort() : 80);

        return DefaultBHttpClientConnectionFactory.INSTANCE.createConnection(socket);
    }
}
