package ru.radiomayak.content;

import android.content.Context;
import android.support.annotation.MainThread;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import ru.radiomayak.LighthouseApplication;

public class LoaderManagerAsync<T> implements LoaderManager<T> {
    private final Executor executor;

    private final Map<Loader, LoaderManagerAsyncTask<T>> tasks = new HashMap<>();

    public LoaderManagerAsync() {
        this(false);
    }

    public LoaderManagerAsync(boolean isPooled) {
        executor = isPooled ? LighthouseApplication.NETWORK_POOL_EXECUTOR : LighthouseApplication.NETWORK_SERIAL_EXECUTOR;
    }

    @Override
    @MainThread
    public Future<T> execute(Context context, Loader<T> loader, Loader.Listener<T> listener) {
        LoaderManagerAsyncTask<T> task = tasks.get(loader);
        if (task == null) {
            task = new LoaderManagerAsyncTask<>(this, loader);
            tasks.put(loader, task);
            task.executeOnExecutor(executor, context);
        }
        return task.addListener(listener);
    }

    @MainThread
    LoaderManagerAsyncTask<T> remove(Loader<T> loader) {
        return tasks.remove(loader);
    }
}
