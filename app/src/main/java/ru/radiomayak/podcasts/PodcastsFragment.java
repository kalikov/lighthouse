package ru.radiomayak.podcasts;

import ru.radiomayak.R;
import ru.radiomayak.content.Loader;

public class PodcastsFragment extends AbstractPodcastsFragment implements PodcastIconLoader.Listener {
    public static final String TAG = PodcastsFragment.class.getName();

    public PodcastsFragment() {
        super(R.string.podcasts, new PodcastsLoopback());
    }

    @Override
    protected Loader<Podcasts> createRemoteLoader() {
        return new PodcastsLoader();
    }

    @Override
    protected void storePodcasts(PodcastsWritableDatabase database, Podcasts podcasts) {
        database.storePodcasts(podcasts);
    }
}
