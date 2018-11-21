package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.annotation.WorkerThread;

import ru.radiomayak.LighthouseTracks;
import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderState;

public class HistoryLoader extends Loader<LighthouseTracks> {
    @WorkerThread
    protected LighthouseTracks onExecute(Context context, LoaderState state) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            return database.loadHistory();
        }
    }
}
