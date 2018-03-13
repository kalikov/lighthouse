package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import ru.radiomayak.LighthouseApplication;

class PageAsyncTask extends AsyncTask<Object, Void, RecordsPaginator> {
    private static final String LOG_TAG = PageAsyncTask.class.getSimpleName();

    private final WeakReference<Context> contextRef;
    private final Listener listener;

    interface Listener {
        void onPageLoaded(RecordsPaginator response, boolean isCancelled);
    }

    PageAsyncTask(LighthouseApplication context, Listener listener) {
        this.contextRef = new WeakReference<Context>(context);
        this.listener = listener;
    }

    @Override
    protected RecordsPaginator doInBackground(Object... params) {
        Context context = contextRef.get();
        if (context == null) {
            return null;
        }
        try {
            RecordsPaginator paginator = (RecordsPaginator) params[0];
            return paginator.advance(context);
        } catch (Throwable e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(RecordsPaginator response) {
        listener.onPageLoaded(response, false);
    }

    @Override
    protected void onCancelled(RecordsPaginator response) {
        listener.onPageLoaded(response, true);
    }
}
