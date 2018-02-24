package ru.radiomayak.content;

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

public abstract class Loader<T> {
    public interface Listener<T> {
        void onComplete(Loader<T> loader, T data);

        void onException(Loader<T> loader, Throwable exception);
    }

    @MainThread
    protected void onStartLoading() {
    }

    @Nullable
    @WorkerThread
    protected T onExecute(Context context, LoaderState state) {
        return null;
    }

    @MainThread
    final void startLoading() {
        onStartLoading();
    }

    @WorkerThread
    final T execute(Context context, LoaderState state) {
        return onExecute(context, state);
    }
}
