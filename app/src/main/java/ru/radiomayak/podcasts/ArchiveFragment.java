package ru.radiomayak.podcasts;

import ru.radiomayak.R;
import ru.radiomayak.content.Loader;

public class ArchiveFragment extends AbstractPodcastsFragment implements PodcastIconLoader.Listener {
    public static final String TAG = ArchiveFragment.class.getName();

    public ArchiveFragment() {
        super(R.string.archive, new ArchiveLoopback());
    }

    @Override
    protected Loader<Podcasts> createRemoteLoader() {
        return new ArchiveLoader();
    }

    @Override
    protected void storePodcasts(PodcastsWritableDatabase database, Podcasts podcasts) {
        database.storeArchivePodcasts(podcasts);
    }
}
