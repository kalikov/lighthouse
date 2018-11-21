package ru.radiomayak.podcasts;

import android.content.Context;

import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderState;

class ArchiveLoopback extends Loader<Podcasts> {
    @Override
    protected Podcasts onExecute(Context context, LoaderState state) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            return database.loadArchivePodcasts();
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof ArchiveLoopback;
    }
}
