package ru.radiomayak.podcasts;

import android.content.Context;

import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderState;

class PodcastsLoopback extends Loader<Podcasts> {
    @Override
    protected Podcasts onExecute(Context context, LoaderState state) {
        return PodcastsUtils.loadPodcasts(context);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof PodcastsLoopback;
    }
}
