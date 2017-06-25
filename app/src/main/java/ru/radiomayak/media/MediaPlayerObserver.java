package ru.radiomayak.media;

public interface MediaPlayerObserver {
    void onPrepared();

    void onFailed();

    void onCompleted();
}
