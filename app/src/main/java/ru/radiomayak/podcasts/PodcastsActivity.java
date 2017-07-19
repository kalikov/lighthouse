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

    private static final String FRAGMENT_TAG = PodcastsActivity.class.getName() + "$data";

    private static final int DEFAULT_IMAGES_CAPACITY = 100;

    private static final LoaderManager<Podcasts> podcastsLoaderManager = new LoaderManager<>();

    @VisibleForTesting
    PodcastsAdapter adapter;

    private Podcasts podcasts;

    private Future<Podcasts> podcastsFuture;

    private final LongSparseArray<Future<BitmapInfo>> futures = new LongSparseArray<>(DEFAULT_IMAGES_CAPACITY);

    private final BroadcastReceiver viewReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(RecordsActivity.EXTRA_PODCAST_ID, 0);
            Podcast podcast = podcasts.get(id);
            if (podcast != null) {
                podcast.setSeen(podcast.getLength());
                updatePodcastRow(id);
            }
        }
    };

    private final Loader.OnLoadListener<Podcasts> podcastsOnLoadListener = new Loader.OnLoadListener<Podcasts>() {
        @Override
        public void onLoadComplete(Loader<Podcasts> loader, Podcasts data) {
            onPodcastsLoadComplete(data);
        }
    };

    private final Loader.OnLoadListener<BitmapInfo> podcastIconOnLoadListener = new Loader.OnLoadListener<BitmapInfo>() {
        @Override
        public void onLoadComplete(Loader<BitmapInfo> loader, BitmapInfo bitmapInfo) {
            onIconLoadComplete(getPodcastId(loader), bitmapInfo);
        }

        private long getPodcastId(Loader<BitmapInfo> loader) {
            return ((PodcastIconLoader) loader).getPodcast().getId();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

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

        adapter = new PodcastsAdapter(getLighthouseApplication(), podcasts.list());
        initializeView();

        if (requestList) {
            requestList();
        } else {
            showContentView();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(viewReceiver);

        if (podcastsFuture != null && !podcastsFuture.isDone()) {
            podcastsFuture.cancel(true);
            podcastsFuture = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePlayerView();

        updatePodcastRows();
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

    private void initializeToolbar() {
        Toolbar actionBar = (Toolbar) findViewById(R.id.toolbar);
        actionBar.setTitle(R.string.podcasts);
        ToolbarCompat.setTitleTypeface(actionBar, getLighthouseApplication().getFontNormal());

        setSupportActionBar(actionBar);
    }

    private void initializeLoadingView() {
        TextView text = (TextView) getLoadingView().findViewById(android.R.id.progress);
        text.setText(R.string.podcasts_loading);
        text.setTypeface(getLighthouseApplication().getFontNormal());
    }

    private void initializeErrorView() {
        TextView text = (TextView) getErrorView();
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
                requestList();
            }
        });
        getRefreshView().setColorSchemeResources(R.color.colorPrimary);
        getRefreshView().setEnabled(true);
    }

    private ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    private View getLoadingView() {
        return findViewById(R.id.loading);
    }

    private View getErrorView() {
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
    void requestList() {
        if (podcastsFuture != null) {
            return;
        }
        boolean isConnected = NetworkUtils.isConnected(this);
        if (adapter.isEmpty() || isConnected) {
            podcastsFuture = podcastsLoaderManager.execute(new PodcastsLoader(this, adapter.isEmpty()), podcastsOnLoadListener);
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

    public void onPodcastsLoadComplete(@Nullable Podcasts podcasts) {
        podcastsFuture = null;
        if (!isDestroyed()) {
            if ((podcasts == null || podcasts.list().isEmpty()) && !adapter.isEmpty()) {
                Toast.makeText(this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
            } else if (podcasts != null && !podcasts.list().isEmpty()) {
                updatePodcasts(podcasts.list());
                new PodcastsStoreAsyncTask(getBaseContext()).executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, podcasts);
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
        futures.put(id, getLighthouseApplication().getImageLoaderManager().execute(loader, podcastIconOnLoadListener));
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
}
