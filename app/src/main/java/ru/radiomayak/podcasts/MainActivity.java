package ru.radiomayak.podcasts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseFragment;
import ru.radiomayak.R;

public class MainActivity extends LighthouseActivity {
    private static final int MAX_OPEN_PODCASTS = 10;

    @VisibleForTesting
    PodcastsFragment podcastsFragment;

    private final Deque<String> fragmentStack = new LinkedList<>();

    private final FragmentFactory<ArchiveFragment> archiveFactory = new FragmentFactory<ArchiveFragment>() {
        @Override
        public ArchiveFragment create() {
            return new ArchiveFragment();
        }
    };

    private final FragmentFactory<HistoryFragment> historyFactory = new FragmentFactory<HistoryFragment>() {
        @Override
        public HistoryFragment create() {
            return new HistoryFragment();
        }
    };

//    private Toast lastBackPressToast;

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
        getBottomBar().getMenu().findItem(R.id.podcasts).setChecked(true);

        Podcast podcast = getIntent().getParcelableExtra(MainActivity.EXTRA_PODCAST);
        if (podcast != null) {
            fragmentManager.executePendingTransactions();
            openPodcast(podcast);
        }

        getBottomBar().setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.podcasts:
                        openPodcasts();
                        break;
                    case R.id.archive:
                        openArchive();
                        break;
                    case R.id.history:
                        openHistory();
                        break;
                }
                return true;
            }
        });
        getBottomBar().setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.podcasts:
                        if (PodcastsFragment.TAG.equals(fragmentStack.peek())) {

                        } else {
                            openPodcasts();
                        }
                        break;
                    case R.id.archive:
                        if (ArchiveFragment.TAG.equals(fragmentStack.peek())) {

                        } else {
                            openArchive();
                        }
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        fragmentStack.clear();

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Podcast podcast = intent.getParcelableExtra(MainActivity.EXTRA_PODCAST);
        if (podcast != null) {
            openPodcast(podcast);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
    }

    @Override
    public void openPodcast(final Podcast podcast) {
        String tag = RecordsFragment.TAG + podcast.getId();
        openFragment(tag, new FragmentFactory<RecordsFragment>() {
            @Override
            public RecordsFragment create() {
                return createRecordsFragment(podcast);
            }
        }, podcast.isArchived() ? R.id.archive : R.id.podcasts);
    }

    private void openPodcasts() {
        showPodcasts();
        fragmentStack.remove(PodcastsFragment.TAG);
        fragmentStack.add(PodcastsFragment.TAG);
        getBottomBar().getMenu().findItem(R.id.podcasts).setChecked(true);
    }

    private void showPodcasts() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        attach(fragmentManager, podcastsFragment);
    }

    private void openArchive() {
        openFragment(ArchiveFragment.TAG, archiveFactory, R.id.archive);
    }

    private void openHistory() {
        openFragment(HistoryFragment.TAG, historyFactory, R.id.history);
    }

    private RecordsFragment createRecordsFragment(Podcast podcast) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(RecordsFragment.EXTRA_PODCAST, podcast);

        RecordsFragment recordsFragment = new RecordsFragment();
        recordsFragment.setArguments(arguments);
        return recordsFragment;
    }

    @SuppressWarnings("StringEquality")
    private void openFragment(String tag, FragmentFactory<?> fragmentFactory, int navigationItem) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            add(fragmentManager, fragmentFactory.create(), tag);
            fragmentStack.add(tag);
        } else {
            attach(fragmentManager, fragment);
            fragmentStack.remove(tag);
            fragmentStack.add(tag);
            if (fragmentStack.getFirst() == PodcastsFragment.TAG) {
                fragmentStack.removeFirst();
            }
        }
        if (isPodcastTag(tag) && fragmentStack.size() >= MAX_OPEN_PODCASTS) {
            int openPodcasts = 0;
            for (Fragment item : fragmentManager.getFragments()) {
                if (item instanceof RecordsFragment) {
                    openPodcasts++;
                }
            }
            if (openPodcasts > MAX_OPEN_PODCASTS) {
                Iterator<String> iterator = fragmentStack.iterator();
                while (iterator.hasNext()) {
                    String item = iterator.next();
                    if (isPodcastTag(item)) {
                        iterator.remove();
                        remove(fragmentManager, item);
                        break;
                    }
                }
            }
        }
        getBottomBar().getMenu().findItem(navigationItem).setChecked(true);
    }

    private static void attach(FragmentManager fragmentManager, Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment item : fragmentManager.getFragments()) {
            if (item != fragment) {
                transaction.detach(item);
            }
        }
        transaction.attach(fragment);
        transaction.commit();
    }

    private static void add(FragmentManager fragmentManager, Fragment fragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment item : fragmentManager.getFragments()) {
            transaction.detach(item);
        }
        transaction.add(android.R.id.tabcontent, fragment, tag);
        transaction.commit();
    }

    private static void remove(FragmentManager fragmentManager, String tag) {
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(fragment);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!fragmentStack.isEmpty()) {
            popFragmentStack();
            return;
        }
//        if (lastBackPressToast == null || lastBackPressToast.getView() == null || !lastBackPressToast.getView().isShown()) {
//            if (lastBackPressToast == null || lastBackPressToast.getView() == null) {
//                lastBackPressToast = Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT);
//            }
//            lastBackPressToast.show();
//            return;
//        }
        super.onBackPressed();
    }

    @Override
    public boolean isNavigateBackSupported() {
        return !fragmentStack.isEmpty();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!fragmentStack.isEmpty()) {
            return popFragmentStack();
        }
        if (super.onSupportNavigateUp()) {
            return true;
        }
        finish();
        return true;
    }

    private boolean popFragmentStack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentStack.size() == 1) {
            String tag = fragmentStack.pop();
            if (isPodcastTag(tag)) {
                remove(fragmentManager, tag);
            }
            showPodcasts();
        } else {
            Fragment tagFragment;
            do {
                String removed = fragmentStack.removeLast();
                if (isPodcastTag(removed)) {
                    remove(fragmentManager, removed);
                }
                String tag = fragmentStack.getLast();
                tagFragment = fragmentManager.findFragmentByTag(tag);
            } while (tagFragment == null && !fragmentStack.isEmpty());

            if (tagFragment == null) {
                showPodcasts();
            } else {
                attach(fragmentManager, tagFragment);
            }
        }
        updateBottomBar();
        return true;
    }

    private static boolean isPodcastTag(String tag) {
        return tag.startsWith(RecordsFragment.TAG);
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
        RecordsFragment podcastFragment = (RecordsFragment) fragmentManager.findFragmentByTag(RecordsFragment.TAG + podcast);
        if (podcastFragment != null) {
            podcastFragment.updateRecordPlaybackAttributes(state, record, position, duration);
        }
        HistoryFragment historyFragment = (HistoryFragment) fragmentManager.findFragmentByTag(HistoryFragment.TAG);
        if (historyFragment != null) {
            historyFragment.updateTrackPlaybackAttributes(state, podcast, record, position, duration);
        }
    }

    @SuppressWarnings("StringEquality")
    private void updateBottomBar() {
        String tag = null;
        Iterator<String> iterator = fragmentStack.descendingIterator();
        if (iterator.hasNext()) {
            tag = iterator.next();
        }
        if (tag == ArchiveFragment.TAG) {
            getBottomBar().getMenu().findItem(R.id.archive).setChecked(true);
        } else if (tag == HistoryFragment.TAG) {
            getBottomBar().getMenu().findItem(R.id.history).setChecked(true);
        } else if (tag == null || tag == PodcastsFragment.TAG) {
            getBottomBar().getMenu().findItem(R.id.podcasts).setChecked(true);
        } else {
            int item = R.id.podcasts;
            RecordsFragment fragment = (RecordsFragment) getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment != null) {
                Podcast podcast = fragment.getPodcast();
                item = podcast != null && podcast.isArchived() ? R.id.archive : R.id.podcasts;
            }
            getBottomBar().getMenu().findItem(item).setChecked(true);
        }
    }

    private BottomNavigationView getBottomBar() {
        return findViewById(R.id.bottom_bar);
    }
}
