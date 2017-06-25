package ru.radiomayak.http;

import java.io.IOException;
import java.net.URL;

public interface HttpClientConnectionFactory {
    HttpClientConnection openConnection(URL url) throws IOException;
}
