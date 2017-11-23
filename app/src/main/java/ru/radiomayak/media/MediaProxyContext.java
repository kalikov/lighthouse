package ru.radiomayak.media;

import java.io.File;

public interface MediaProxyContext {
    File getCacheDir();

    void notifyUpdate(String category, String id, int size, boolean partial);
}
