package ru.radiomayak.media;

import android.database.Observable;

public class MediaPlayerObservable extends Observable<MediaPlayerObserver> {
    public boolean containsObserver(MediaPlayerObserver observer) {
        synchronized (mObservers) {
            return mObservers.contains(observer);
        }
    }

    public void notifyPrepared() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onPrepared();
            }
        }
    }

    public void notifyFailed() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onFailed();
            }
        }
    }

    public void notifyCompleted() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onCompleted();
            }
        }
    }
}
