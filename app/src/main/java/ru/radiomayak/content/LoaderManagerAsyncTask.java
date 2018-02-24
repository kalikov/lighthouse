package ru.radiomayak.content;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.MainThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

class LoaderManagerAsyncTask<T> extends AsyncTask<Context, Void, T> {
    private final LoaderManagerAsync<T> manager;
    private final Loader<T> loader;
    private final Collection<Loader.Listener<T>> listeners = new ArrayList<>(2);
    private final AtomicReference<Throwable> throwableReference = new AtomicReference<>();

    LoaderManagerAsyncTask(LoaderManagerAsync<T> manager, Loader<T> loader) {
        this.manager = manager;
        this.loader = loader;
    }

    Future<T> addListener(Loader.Listener<T> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        return new LoaderManagerFuture<>(this, listener);
    }

    boolean removeListener(Loader.Listener<T> listener) {
        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }

    int getListenersCount() {
        synchronized (listeners) {
            return listeners.size();
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        loader.startLoading();
    }

    @Override
    protected T doInBackground(Context... params) {
        publishProgress();
        try {
            return loader.execute(params[0], new LoaderState() {
                @Override
                public boolean isCancelled() {
                    return LoaderManagerAsyncTask.this.isCancelled();
                }
            });
        } catch (Throwable throwable) {
            throwableReference.set(throwable);
            return null;
        }
    }

    @Override
    @MainThread
    protected void onPostExecute(T response) {
        manager.remove(loader);
        Throwable throwable = throwableReference.get();
        synchronized (listeners) {
            if (throwable == null) {
                for (Loader.Listener<T> listener : listeners) {
                    listener.onComplete(loader, response);
                }
            } else {
                for (Loader.Listener<T> listener : listeners) {
                    listener.onException(loader, throwable);
                }
            }
        }
    }

    @Override
    @MainThread
    protected void onCancelled() {
        manager.remove(loader);
        synchronized (listeners) {
            for (Loader.Listener<T> listener : listeners) {
                listener.onException(loader, new CancellationException());
            }
        }
    }
}
