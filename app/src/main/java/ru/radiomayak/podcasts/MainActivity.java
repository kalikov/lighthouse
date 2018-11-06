package ru.radiomayak.podcasts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseFragment;
import ru.radiomayak.R;

public class MainActivity extends LighthouseActivity {
    @VisibleForTesting
    PodcastsFragment podcastsFragment;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        podcastsFragment = (PodcastsFragment) fragmentManager.findFragmentByTag(PodcastsFragment.TAG);
        if (podcastsFragment == null) {
            podcastsFragment = new PodcastsFragment();

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(android.R.id.tabcontent, podcastsFragment, PodcastsFragment.TAG);
            transaction.commit();
        }

        Podcast podcast = getIntent().getParcelableExtra(MainActivity.EXTRA_PODCAST);
        if (podcast != null) {
            Fragment fragment = fragmentManager.findFragmentByTag(RecordsFragment.TAG + podcast.getId());
            if (fragment == null) {
                openPodcast(podcast);
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Podcast podcast = intent.getParcelableExtra(MainActivity.EXTRA_PODCAST);
        if (podcast != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentByTag(RecordsFragment.TAG + podcast.getId());
            if (fragment == null) {
                openPodcast(podcast);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
    }

    @Override
    public void openPodcast(Podcast podcast) {
        RecordsFragment recordsFragment = createRecordsFragment(podcast);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(android.R.id.tabcontent, recordsFragment, RecordsFragment.TAG + podcast.getId());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private RecordsFragment createRecordsFragment(Podcast podcast) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(RecordsFragment.EXTRA_PODCAST, podcast);

        RecordsFragment recordsFragment = new RecordsFragment();
        recordsFragment.setArguments(arguments);
        return recordsFragment;
    }

    @Override
    public boolean onSupportNavigateUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            return fragmentManager.popBackStackImmediate();
        }
        if (super.onSupportNavigateUp()) {
            return true;
        }
        finish();
        return true;
    }

    @Override
    protected void updatePlayerView(boolean animate) {
        super.updatePlayerView(animate);

        FragmentManager fragmentManager = getSupportFragmentManager();
        LighthouseFragment fragment = (LighthouseFragment) fragmentManager.findFragmentById(android.R.id.tabcontent);
        if (fragment != null) {
            fragment.updatePlayerState();
        }
    }

    @Override
    protected void updateRecordPlaybackAttributes(int state, long podcast, long record, long position, long duration) {
        super.updateRecordPlaybackAttributes(state, podcast, record, position, duration);

        FragmentManager fragmentManager = getSupportFragmentManager();
        RecordsFragment fragment = (RecordsFragment) fragmentManager.findFragmentByTag(RecordsFragment.TAG + podcast);
        if (fragment != null) {
            fragment.updateRecordPlaybackAttributes(state, record, position, duration);
        }
    }
}
