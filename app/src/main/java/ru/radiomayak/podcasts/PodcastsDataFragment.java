package ru.radiomayak.podcasts;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class PodcastsDataFragment extends Fragment {
    private Podcasts podcasts;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setRetainInstance(true);
    }

    public void setPodcasts(Podcasts podcasts) {
        this.podcasts = podcasts;
    }

    public Podcasts getPodcasts() {
        return podcasts;
    }

}
