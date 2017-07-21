package ru.radiomayak.content;

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

public abstract class Loader<T> {
    public interface OnLoadListener<T> {
        void onLoadComplete(Loader<T> loader, T data);
    }

    private final Context context;

    private LoaderManagerAsyncTask<T> task;

    public Loader(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    void setTask(LoaderManagerAsyncTask<T> task) {
        this.task = task;
    }

    @MainThread
    protected void onStartLoading() {
    }

    @Nullable
    @WorkerThread
    protected T onExecute() {
        return null;
    }

    @MainThread
    protected void onEndLoading(T data) {
    }

    protected boolean isCancelled() {
        return task != null && task.isCancelled();
    }

    @MainThread
    void startLoading() {
        onStartLoading();
    }

    @WorkerThread
    T execute() {
        return onExecute();
    }

    @MainThread
    void endLoading(T data) {
        onEndLoading(data);
    }
}
