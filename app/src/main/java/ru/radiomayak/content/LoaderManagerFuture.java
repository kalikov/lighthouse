package ru.radiomayak.content;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class LoaderManagerFuture<T> implements Future<T> {
    private final LoaderManagerAsyncTask<T> task;
    private final Loader.Listener<T> listener;

    LoaderManagerFuture(LoaderManagerAsyncTask<T> task, Loader.Listener<T> listener) {
        this.task = task;
        this.listener = listener;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = task.removeListener(listener);
        if (task.getListenersCount() == 0) {
            task.cancel(mayInterruptIfRunning);
        }
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public boolean isDone() {
        return task.getStatus() == AsyncTask.Status.FINISHED;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return task.get();
    }

    @Override
    public T get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return task.get(timeout, unit);
    }
}
