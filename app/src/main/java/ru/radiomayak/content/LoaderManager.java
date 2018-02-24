package ru.radiomayak.content;

import android.content.Context;
import android.support.annotation.MainThread;

import java.util.concurrent.Future;

public interface LoaderManager<T> {
    @MainThread
    Future<T> execute(Context context, Loader<T> loader, Loader.Listener<T> listener);
}
