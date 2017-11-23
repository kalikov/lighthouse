package ru.radiomayak.media;

import android.net.Uri;

import java.io.IOException;

public interface MediaProxyServer {
    void start() throws IOException;

    void stop() throws InterruptedException;

    boolean isStarted();

    Uri formatUri(long category, long id, String targetUrl);
}
