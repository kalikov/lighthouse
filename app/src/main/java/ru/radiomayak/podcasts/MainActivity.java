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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseFragment;
import ru.radiomayak.R;

public class MainActivity extends LighthouseActivity {
    private static final int MAX_OPEN_PODCASTS = 10;

    private static final String STATE_FRAGMENT_STACK = MainActivity.class.getName() + "fragmentStack";

    @VisibleForTesting
    PodcastsFragment podcastsFragment;

    private final Deque<String> fragmentStack = new LinkedList<>();

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

        if (state != null) {
            List<String> strings = state.getStringArrayList(STATE_FRAGMENT_STACK);
            if (strings != null) {
                fragmentStack.addAll(strings);
            }
        }

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
//                    case R.id.archive:
//                        openArchive();
//                        break;
                    case R.id.history:
                        openHistory();
                        break;
                }
                return true;
            }
        });
        getBottomBar().setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            @SuppressWarnings("StringEquality")
            public void onNavigationItemReselected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.podcasts:
                        if (!fragmentStack.isEmpty() && PodcastsFragment.TAG != fragmentStack.getLast()) {
                            openPodcasts();
                        }
                        break;
//                    case R.id.archive:
//                        if (fragmentStack.isEmpty() || ArchiveFragment.TAG != fragmentStack.getLast()) {
//                            openArchive();
//                        }
//                        break;
                    case R.id.history:
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
        state.putStringArrayList(STATE_FRAGMENT_STACK, new ArrayList<>(fragmentStack));
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
        }, /*podcast.isArchived() ? R.id.archive :*/ R.id.podcasts);
    }

    @Override
    public void onPodcastSeen(long podcast, int seen) {
        onPodcastSeen(podcastsFragment, podcast, seen);
    }

    @Override
    public void onHistoryTrackRemoved(long podcast, long record) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        RecordsFragment podcastFragment = (RecordsFragment) fragmentManager.findFragmentByTag(RecordsFragment.TAG + podcast);
        if (podcastFragment != null) {
            podcastFragment.updateRecordPosition(record, -1);
        }
    }

    private void onPodcastSeen(AbstractPodcastsFragment fragment, long podcast, int seen) {
        fragment.onPodcastSeen(podcast, seen);
    }

    private void openPodcasts() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment item : fragmentManager.getFragments()) {
            if (item != podcastsFragment) {
                transaction.detach(item);
            }
        }

        fragmentStack.remove(PodcastsFragment.TAG);
        if (!fragmentStack.isEmpty()) {
            fragmentStack.add(PodcastsFragment.TAG);
        }

        transaction.attach(podcastsFragment);
        transaction.commitNow();

        getBottomBar().getMenu().findItem(R.id.podcasts).setChecked(true);
    }

    private void openArchive() {
//        openFragment(ArchiveFragment.TAG, archiveFactory, R.id.archive);
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
    private void openFragment(final String tag, FragmentFactory<?> fragmentFactory, final int navigationItem) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragment = fragmentManager.findFragmentByTag(tag);

        Fragment removedFragment = null;
        if (isPodcastTag(tag) && fragmentStack.size() >= MAX_OPEN_PODCASTS) {
            final LinkedHashSet<String> openedPodcasts = new LinkedHashSet<>(MAX_OPEN_PODCASTS);
            for (String item : fragmentStack) {
                if (isPodcastTag(item)) {
                    Fragment itemFragment = fragmentManager.findFragmentByTag(item);
                    if (itemFragment != null && itemFragment != fragment) {
                        openedPodcasts.add(item);
                        removedFragment = removedFragment == null ? itemFragment : removedFragment;
                    }
                }
            }
            if (openedPodcasts.size() < MAX_OPEN_PODCASTS) {
                removedFragment = null;
            }
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (removedFragment != null) {
            transaction.remove(removedFragment);
        }
        for (Fragment item : fragmentManager.getFragments()) {
            if (item != fragment && item != removedFragment) {
                transaction.detach(item);
            }
        }

        if (removedFragment != null) {
            fragmentStack.remove(removedFragment.getTag());
        }
        fragmentStack.remove(tag);
        if (!fragmentStack.isEmpty() && PodcastsFragment.TAG == fragmentStack.getFirst()) {
            fragmentStack.remove(PodcastsFragment.TAG);
        }
        fragmentStack.add(tag);

        if (fragment == null) {
            transaction.add(android.R.id.tabcontent, fragmentFactory.create(), tag);
        } else {
            transaction.attach(fragment);
        }

        transaction.commitNow();

        getBottomBar().getMenu().findItem(navigationItem).setChecked(true);
    }

    @Override
    public void onBackPressed() {
        if (!fragmentStack.isEmpty()) {
            popFragmentStack();
            return;
        }
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

    @SuppressWarnings("StringEquality")
    private boolean popFragmentStack() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        Iterator<String> iterator = fragmentStack.descendingIterator();
        String tag = iterator.next();

        final Set<String> removed = new HashSet<>();
        removed.add(tag);

        Fragment removeFragment = null;
        if (isPodcastTag(tag)) {
            removeFragment = fragmentManager.findFragmentByTag(tag);
        }

        Fragment fragment = null;
        while (iterator.hasNext() && fragment == null) {
            tag = iterator.next();
            if (!removed.contains(tag)) {
                fragment = fragmentManager.findFragmentByTag(tag);
            }
            if (fragment == null) {
                removed.add(tag);
            }
        }

        final String targetTag = fragment == null ? PodcastsFragment.TAG : tag;
        final Fragment targetFragment = fragment == null ? podcastsFragment : fragment;

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment item : fragmentManager.getFragments()) {
            if (item != targetFragment) {
                if (item == removeFragment) {
                    transaction.remove(item);
                } else {
                    transaction.detach(item);
                }
            }
        }

        fragmentStack.removeAll(removed);
        fragmentStack.remove(targetTag);
        if (!fragmentStack.isEmpty() && PodcastsFragment.TAG == fragmentStack.getFirst()) {
            fragmentStack.remove(PodcastsFragment.TAG);
        }
        if (PodcastsFragment.TAG != targetTag || !fragmentStack.isEmpty()) {
            fragmentStack.add(targetTag);
        }
        getBottomBar().getMenu().findItem(getNavigationItem(targetTag, targetFragment)).setChecked(true);

        transaction.attach(targetFragment);
        transaction.commitNow();

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
    private int getNavigationItem(String tag, Fragment fragment) {
//        if (tag == ArchiveFragment.TAG) {
//            return R.id.archive;
//        }
        if (tag == HistoryFragment.TAG) {
            return R.id.history;
        }
        if (tag == null || tag == PodcastsFragment.TAG) {
            return R.id.podcasts;
        }
        int item = R.id.podcasts;
        if (fragment instanceof RecordsFragment) {
            Podcast podcast = ((RecordsFragment) fragment).getPodcast();
            item = /*podcast != null && podcast.isArchived() ? R.id.archive :*/ R.id.podcasts;
        }
        return item;
    }

    private BottomNavigationView getBottomBar() {
        return findViewById(R.id.bottom_bar);
    }
}
