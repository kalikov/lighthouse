package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.WorkerThread;

import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderState;

public class HistoryLoader extends Loader<HistoryPage> {
    private static final int PAGE_SIZE = 20;
    private static final int CURSOR_BEGINNING = Integer.MAX_VALUE;

    private final int cursor;

    public HistoryLoader() {
        this(CURSOR_BEGINNING);
    }

    public HistoryLoader(int cursor) {
        this.cursor = cursor;
    }

    @WorkerThread
    protected HistoryPage onExecute(Context context, LoaderState state) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            return database.loadHistory(cursor, PAGE_SIZE);
        }
    }

    @Override
    public int hashCode() {
        return cursor;
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof HistoryLoader && ((HistoryLoader) object).cursor == cursor;
    }
}
