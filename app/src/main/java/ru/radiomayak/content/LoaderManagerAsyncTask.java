package ru.radiomayak.content;

import android.os.AsyncTask;
import android.support.annotation.MainThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

class LoaderManagerAsyncTask<T> extends AsyncTask<Void, Void, T> {
    private final LoaderManager<T> manager;
    private final Loader<T> loader;
    private final Collection<Loader.OnLoadListener<T>> listeners = new ArrayList<>(2);

    LoaderManagerAsyncTask(LoaderManager<T> manager, Loader<T> loader) {
        this.manager = manager;
        this.loader = loader;
        loader.setTask(this);
    }

    Future<T> addListener(Loader.OnLoadListener<T> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        return new LoaderManagerFuture<>(this, listener);
    }

    boolean removeListener(Loader.OnLoadListener<T> listener) {
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
    protected T doInBackground(Void... params) {
        publishProgress();
        return loader.execute();
    }

    @Override
    @MainThread
    protected void onPostExecute(T response) {
        manager.remove(loader);
        loader.endLoading(response);
        synchronized (listeners) {
            for (Loader.OnLoadListener<T> listener : listeners) {
                listener.onLoadComplete(loader, response);
            }
        }
    }

    @Override
    @MainThread
    protected void onCancelled(T response) {
        manager.remove(loader);
    }
}
