package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

class PageAsyncTask extends AsyncTask<Object, Void, RecordsPaginator> {
    private static final String LOG_TAG = PageAsyncTask.class.getSimpleName();

    private final Context context;
    private final Listener listener;

    interface Listener {
        void onPageLoaded(RecordsPaginator response, boolean isCancelled);
    }

    PageAsyncTask(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected RecordsPaginator doInBackground(Object... params) {
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
