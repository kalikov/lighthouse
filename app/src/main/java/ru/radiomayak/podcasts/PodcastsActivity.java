package ru.radiomayak.podcasts;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.LongSparseArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Future;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.NetworkUtils;
import ru.radiomayak.R;
import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.widget.ToolbarCompat;

public class PodcastsActivity extends LighthouseActivity {
    private static final String STATE_CONTENT_VIEW = PodcastsActivity.class.getName() + "$contentView";

    static final String FRAGMENT_TAG = PodcastsActivity.class.getName() + "$data";

    private static final int DEFAULT_IMAGES_CAPACITY = 100;

    private static final PodcastsLoopback podcastsLoopback = new PodcastsLoopback();

    @VisibleForTesting
    LoaderManager<Podcasts> podcastsLoaderManager;

    @VisibleForTesting
    PodcastsAdapter adapter;

    private Podcasts podcasts;

    @VisibleForTesting
    Future<Podcasts> podcastsFuture;

    private float toolbarTextSize;

    private final LongSparseArray<Future<BitmapInfo>> futures = new LongSparseArray<>(DEFAULT_IMAGES_CAPACITY);

    private final BroadcastReceiver viewReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(RecordsActivity.EXTRA_PODCAST_ID, 0);
            Podcast podcast = podcasts.get(id);
            if (podcast != null) {
                int seen = intent.getIntExtra(RecordsActivity.EXTRA_SEEN, 0);
                if (seen > 0) {
                    podcast.setSeen(seen);
                    updatePodcastRow(id);
                }
            }
        }
    };

    @VisibleForTesting
    final Loader.Listener<Podcasts> podcastsLoopbackListener = new Loader.Listener<Podcasts>() {
        @Override
        public void onComplete(Loader<Podcasts> loader, Podcasts data) {
            onLoopbackComplete(data);
        }

        @Override
        public void onException(Loader<Podcasts> loader, Throwable exception) {
            podcastsFuture = null;
            showErrorView();
        }
    };

    @VisibleForTesting
    final Loader.Listener<Podcasts> podcastsListener = new Loader.Listener<Podcasts>() {
        @Override
        public void onComplete(Loader<Podcasts> loader, Podcasts data) {
            onRemoteListComplete(data);
        }

        @Override
        public void onException(Loader<Podcasts> loader, Throwable exception) {
            onRemoteListComplete(null);
        }
    };

    private final Loader.Listener<BitmapInfo> podcastIconListener = new Loader.Listener<BitmapInfo>() {
        @Override
        public void onComplete(Loader<BitmapInfo> loader, BitmapInfo bitmapInfo) {
            onIconLoadComplete(getPodcastId(loader), bitmapInfo);
        }

        @Override
        public void onException(Loader<BitmapInfo> loader, Throwable exception) {
        }

        private long getPodcastId(Loader<BitmapInfo> loader) {
            return ((PodcastIconLoader) loader).getPodcast().getId();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        podcastsLoaderManager = getLighthouseApplication().getModule().createLoaderManager();

        IntentFilter filter = new IntentFilter();
        filter.addAction(RecordsActivity.ACTION_VIEW);
        registerReceiver(viewReceiver, filter);

        boolean requestList = true;
        if (state != null) {
            boolean isContentView = state.getBoolean(STATE_CONTENT_VIEW, true);
            if (isContentView) {
                FragmentManager fragmentManager = getFragmentManager();
                PodcastsDataFragment dataFragment = (PodcastsDataFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
                if (dataFragment != null) {
                    podcasts = dataFragment.getPodcasts();
                    requestList = podcasts == null || podcasts.list().isEmpty();
                }
            }
        }
        if (podcasts == null) {
            podcasts = new Podcasts();
        }

        adapter = new PodcastsAdapter(this, podcasts.list());
        initializeView();

        if (requestList) {
            requestLoopback();
        } else {
            showContentView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.podcasts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(viewReceiver);

        if (podcastsFuture != null && !podcastsFuture.isDone()) {
            podcastsFuture.cancel(true);
            podcastsFuture = null;
        }
        podcasts = null;
        super.onDestroy();
    }

    private void initializeView() {
        setContentView(R.layout.podcasts);

        initializeToolbar();

        initializeLoadingView();
        initializeErrorView();
        initializeListView();

        initializeRefreshView();

        initializePlayerView();
    }

    @VisibleForTesting
    Toolbar getToolbar() {
        return findViewById(R.id.toolbar);
    }

    private void initializeToolbar() {
        Toolbar actionBar = getToolbar();
        actionBar.setTitle(R.string.podcasts);
        ToolbarCompat.setTitleTypeface(actionBar, getLighthouseApplication().getFontNormal());
        toolbarTextSize = ToolbarCompat.getTitleTextSize(actionBar);

        setSupportActionBar(actionBar);
    }

    private void initializeLoadingView() {
        TextView text = getLoadingView().findViewById(android.R.id.progress);
        text.setText(R.string.podcasts_loading);
        text.setTypeface(getLighthouseApplication().getFontNormal());
    }

    private void initializeErrorView() {
        TextView text = getErrorView();
        text.setText(R.string.podcasts_error);
        text.setTypeface(getLighthouseApplication().getFontNormal());
    }

    private void initializeListView() {
        ListView list = getListView();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openPodcast(adapter.getItem(position));
            }
        });
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int firstVisibleItem;
            private int visibleItemCount;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                getRefreshView().setEnabled(isRefreshViewEnabled());
                if (visibleItemCount > 0 && (visibleItemCount != this.visibleItemCount || firstVisibleItem != this.firstVisibleItem)) {
                    this.firstVisibleItem = firstVisibleItem;
                    this.visibleItemCount = visibleItemCount;
                    requestIcons(firstVisibleItem, visibleItemCount);
                }
            }
        });
    }

    private boolean isRefreshViewEnabled() {
        return getRefreshView().isRefreshing() || isListViewScrollOnTop(getListView());
    }

    private static boolean isListViewScrollOnTop(AbsListView listView) {
        if (listView.getChildCount() == 0) {
            return true;
        }
        if (listView.getFirstVisiblePosition() > 0) {
            return false;
        }
        View topChild = listView.getChildAt(0);
        return topChild.getTop() >= 0;
    }

    private void initializeRefreshView() {
        getRefreshView().setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestRemoteList();
            }
        });
        getRefreshView().setColorSchemeResources(R.color.colorPrimary);
        getRefreshView().setEnabled(true);
    }

    @VisibleForTesting
    ListView getListView() {
        return findViewById(android.R.id.list);
    }

    @VisibleForTesting
    View getLoadingView() {
        return findViewById(R.id.loading);
    }

    @VisibleForTesting
    <T extends View> T getErrorView() {
        return findViewById(R.id.error);
    }

    @VisibleForTesting
    SwipeRefreshLayout getRefreshView() {
        return (SwipeRefreshLayout) findViewById(R.id.refresh);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (podcasts.list().isEmpty()) {
            state.putBoolean(STATE_CONTENT_VIEW, getErrorView().getVisibility() == View.VISIBLE);
        } else {
            state.putBoolean(STATE_CONTENT_VIEW, true);
            FragmentManager fragmentManager = getFragmentManager();
            PodcastsDataFragment dataFragment = (PodcastsDataFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
            if (dataFragment == null) {
                dataFragment = new PodcastsDataFragment();
                fragmentManager.beginTransaction().add(dataFragment, FRAGMENT_TAG).commit();
            }
            dataFragment.setPodcasts(podcasts);
        }
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    @VisibleForTesting
    void requestLoopback() {
        if (podcastsFuture != null) {
            return;
        }
        podcastsFuture = podcastsLoaderManager.execute(this, podcastsLoopback, podcastsLoopbackListener);
        showLoadingView();
    }

    @VisibleForTesting
    void requestRemoteList() {
        if (podcastsFuture != null) {
            return;
        }
        boolean isConnected = NetworkUtils.isConnected(this);
        if (adapter.isEmpty() || isConnected) {
            podcastsFuture = podcastsLoaderManager.execute(this, new PodcastsLoader(podcasts), podcastsListener);
            if (adapter.isEmpty()) {
                showLoadingView();
            }
        } else {
            toast(R.string.toast_no_connection, Toast.LENGTH_SHORT);
            getRefreshView().setRefreshing(false);
        }
    }

    @VisibleForTesting
    void toast(int id, int duration) {
        Toast.makeText(this, id, duration).show();
    }

    private void showLoadingView() {
        getLoadingView().setVisibility(View.VISIBLE);
        getErrorView().setVisibility(View.GONE);
        getListView().setVisibility(View.GONE);
        getRefreshView().setRefreshing(false);
        getRefreshView().setEnabled(false);
    }

    private void showErrorView() {
        getLoadingView().setVisibility(View.GONE);
        getErrorView().setVisibility(View.VISIBLE);
        getListView().setVisibility(View.GONE);
        getRefreshView().setRefreshing(false);
        getRefreshView().setEnabled(true);
    }

    private void showListView() {
        getLoadingView().setVisibility(View.GONE);
        getListView().setVisibility(View.VISIBLE);
        getRefreshView().setRefreshing(false);
        getRefreshView().setEnabled(isRefreshViewEnabled());
    }

    private void showContentView() {
        if (adapter.isEmpty()) {
            showErrorView();
        } else {
            showListView();
        }
    }

    private void openPodcast(Podcast podcast) {
        Intent intent = new Intent(this, RecordsActivity.class);
        intent.putExtra(RecordsActivity.EXTRA_PODCAST, podcast);
        startActivity(intent);
    }

    private void onLoopbackComplete(@Nullable Podcasts data) {
        podcastsFuture = null;
        if (!isDestroyed()) {
            if (data != null && !data.list().isEmpty()) {
                updatePodcasts(data.list());
            }
        }
        boolean isConnected = NetworkUtils.isConnected(this);
        if (!adapter.isEmpty() || !isConnected) {
            showContentView();
        }
        if (isConnected) {
            if (!adapter.isEmpty()) {
                getToolbar().setTitle(R.string.refreshing);
                if (toolbarTextSize > 3) {
                    ToolbarCompat.setTitleTextSize(getToolbar(), toolbarTextSize - 3);
                }
            }
            requestRemoteList();
        }
    }

    private void onRemoteListComplete(@Nullable final Podcasts data) {
        podcastsFuture = null;
        if (!isDestroyed()) {
            getToolbar().setTitle(R.string.podcasts);
            ToolbarCompat.setTitleTextSize(getToolbar(), toolbarTextSize);
            if ((data == null || data.list().isEmpty()) && !adapter.isEmpty()) {
                Toast.makeText(this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
            } else if (data != null && !data.list().isEmpty()) {
                updatePodcasts(data.list());
                LighthouseApplication.NETWORK_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        PodcastsUtils.storePodcasts(PodcastsActivity.this, data);
                    }
                });
            }
        }
        showContentView();
    }

    private void updatePodcasts(Iterable<Podcast> iterable) {
        boolean notifyDataSetChanged = adapter.isEmpty();

        Collection<Podcast> remainingPodcasts = new HashSet<>(podcasts.list());

        int index = 0;
        for (Podcast item : iterable) {
            Podcast podcast = podcasts.get(item.getId());
            if (podcast == null) {
                podcasts.add(index, item);
                notifyDataSetChanged = true;
                index++;
            } else {
                remainingPodcasts.remove(podcast);
                boolean updated = podcast.merge(item);
                if (updated && !notifyDataSetChanged) {
                    updatePodcastRow(podcast);
                }
                index = podcasts.list().indexOf(podcast);
            }
        }
        if (!remainingPodcasts.isEmpty()) {
            notifyDataSetChanged = true;
            for (Podcast podcast : remainingPodcasts) {
                podcasts.remove(podcast);
            }
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
            getRefreshView().setEnabled(isRefreshViewEnabled());
        }
    }

    private void requestIcons(int firstVisiblePosition, int visibleItemsCount) {
        int count = adapter.getCount();
        int first = Math.max(0, firstVisiblePosition - visibleItemsCount / 2);
        int last = Math.min(count - 1, firstVisiblePosition + visibleItemsCount - 1 + visibleItemsCount / 2);
        for (int i = firstVisiblePosition; i <= last; i++) {
            requestIcon(adapter.getItem(i));
        }
        for (int i = first; i < firstVisiblePosition; i++) {
            requestIcon(adapter.getItem(i));
        }
    }

    private void requestIcon(final Podcast podcast) {
        Image image = podcast.getIcon();
        if (image == null) {
            return;
        }
        long id = podcast.getId();
        if (futures.indexOfKey(id) >= 0 || PodcastImageCache.getInstance().getIcon(id) != null) {
            return;
        }
        PodcastIconLoader loader = new PodcastIconLoader(this, podcast);
        futures.put(id, getLighthouseApplication().getImageLoaderManager().execute(this, loader, podcastIconListener));
    }

    public void onIconLoadStarted(long id) {
        if (!isRowInIconLoadingRange(id)) {
            Future<BitmapInfo> future = futures.get(id);
            if (future != null) {
                future.cancel(true);
                futures.remove(id);
            }
        }
    }

    public void onIconLoadComplete(long id, BitmapInfo bitmapInfo) {
        futures.remove(id);
        if (isDestroyed()) {
            return;
        }
        if (bitmapInfo != null) {
            PodcastImageCache.getInstance().putIcon(id, bitmapInfo);
            Podcast podcast = podcasts.get(id);
            if (bitmapInfo.getPrimaryColor() != 0) {
                Image icon = Objects.requireNonNull(podcast.getIcon());
                icon.setColors(bitmapInfo.getPrimaryColor(), bitmapInfo.getSecondaryColor());
            }
            updatePodcastRow(podcast);
        }
    }

    private boolean isRowInIconLoadingRange(long id) {
        ListView list = getListView();
        int count = adapter.getCount();
        int first = list.getFirstVisiblePosition();
        int last = list.getLastVisiblePosition();
        int visible = last - first + 1;
        int start = Math.max(0, first - visible / 2);
        int end = Math.min(count - 1, last + visible / 2);
        for (int i = start; i <= end; i++) {
            if (id == adapter.getItemId(i)) {
                return true;
            }
        }
        return false;
    }

    private void updatePodcastRow(Podcast podcast) {
        updatePodcastRow(podcast.getId());
    }

    private void updatePodcastRow(long id) {
        ListView list = getListView();
        int first = list.getFirstVisiblePosition();
        int last = list.getLastVisiblePosition();
        for (int i = first; i <= last; i++) {
            if (id == list.getItemIdAtPosition(i)) {
                View view = list.getChildAt(i - first);
                list.getAdapter().getView(i, view, list);
                break;
            }
        }
    }

    private void updatePodcastRows() {
        ListView list = getListView();
        int first = list.getFirstVisiblePosition();
        int last = list.getLastVisiblePosition();
        for (int i = first; i <= last; i++) {
            View view = list.getChildAt(i - first);
            list.getAdapter().getView(i, view, list);
        }
    }

    @Override
    protected void updatePlayerView(boolean animate) {
        super.updatePlayerView(animate);

        adapter.updateEqualizerAnimation();
        updatePodcastRows();
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
