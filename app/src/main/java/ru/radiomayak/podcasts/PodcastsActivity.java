package ru.radiomayak.podcasts;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.NetworkUtils;
import ru.radiomayak.R;
import ru.radiomayak.widget.ToolbarCompat;

public class PodcastsActivity extends LighthouseActivity implements PodcastsAsyncTask.Listener, PodcastIconAsyncTask.Listener {
    private Podcasts podcasts;

    private PodcastsAdapter adapter;

    private PodcastsAsyncTask podcastsAsyncTask;

    private final LongSparseArray<Bitmap> images = new LongSparseArray<>(100);

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        podcasts = new Podcasts();

        adapter = new PodcastsAdapter(getLighthouseApplication(), this, podcasts.list(), images);
        adapter.setOnDisplayListener(new PodcastsAdapter.OnDisplayListener() {
            @Override
            public void onDisplay(int position) {
                onPodcastDisplay(adapter.getItem(position));
            }
        });

        initializeView();

        requestList();
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
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                getRefreshView().setEnabled(isRefreshViewEnabled());
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

    private SwipeRefreshLayout getRefreshView() {
        return (SwipeRefreshLayout) findViewById(R.id.refresh);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    private void requestList() {
        if (podcastsAsyncTask != null) {
            return;
        }
        boolean isConnected = NetworkUtils.isConnected(this);
        if (adapter.isEmpty() || isConnected) {
            podcastsAsyncTask = new PodcastsAsyncTask(this, this);
            if (adapter.isEmpty()) {
                podcastsAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, PodcastsAsyncTask.LOOPBACK);
                showLoadingView();
            } else {
                podcastsAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR);
            }
        } else {
            Toast.makeText(this, R.string.toast_no_connection, Toast.LENGTH_SHORT).show();
            showContentView();
        }
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

    @Override
    public void onPodcastsLoaded(Podcasts podcasts, boolean isCancelled) {
        podcastsAsyncTask = null;
        if (!isCancelled) {
            if (podcasts.list().isEmpty() && !adapter.isEmpty()) {
                Toast.makeText(this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
            } else {
                updatePodcasts(podcasts.list());
            }
        }
        showContentView();
    }

    private void schedulePodcastImageUpdate(Podcast podcast) {
        if (images.get(podcast.getId()) != null) {
            return;
        }
        images.put(podcast.getId(), null);
    }

    private void updatePodcasts(Iterable<Podcast> iterable) {
        boolean notifyDataSetChanged = adapter.isEmpty();

        Collection<Podcast> remainingPodcasts = new HashSet<>(podcasts.list());

        int index = 0;
        for (Podcast item : iterable) {
            if (item.getIcon() != null) {
                schedulePodcastImageUpdate(item);
            }
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

    private void onPodcastDisplay(final Podcast podcast) {
        getListView().post(new Runnable() {
            @Override
            public void run() {
                requestPodcastIcon(podcast);
            }
        });
    }

    private void requestPodcastIcon(final Podcast podcast) {
        Image image = podcast.getIcon();
        if (image == null) {
            return;
        }
        if (images.indexOfKey(podcast.getId()) < 0 || images.get(podcast.getId()) != null) {
            return;
        }
        images.remove(podcast.getId());
        PodcastIconAsyncTask task = new PodcastIconAsyncTask(this, this) {
            @Override
            protected LongSparseArray<BitmapInfo> doInBackground(Podcast... podcasts) {
                publishProgress();
                return super.doInBackground(podcasts);
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                if (getPodcastRow(podcast.getId()) < 0) {
                    images.put(podcast.getId(), null);
                    cancel(true);
                }
            }
        };
        task.executeOnExecutor(LighthouseApplication.NETWORK_POOL_EXECUTOR, podcast);
    }

    @Override
    public void onPodcastIconResponse(LongSparseArray<BitmapInfo> response) {
        int size = response.size();
        for (int i = 0; i < size; i++) {
            BitmapInfo bitmapInfo = response.valueAt(i);
            if (bitmapInfo != null) {
                long id = response.keyAt(i);
                images.put(id, bitmapInfo.getBitmap());
                Podcast podcast = podcasts.get(id);
                if (bitmapInfo.getPrimaryColor() != 0) {
                    Image icon = Objects.requireNonNull(podcast.getIcon());
                    icon.setColors(bitmapInfo.getPrimaryColor(), bitmapInfo.getSecondaryColor());
                }
                updatePodcastRow(podcast);
            }
        }
    }

    private int getPodcastRow(Podcast podcast) {
        return getPodcastRow(podcast.getId());
    }

    private int getPodcastRow(long id) {
        ListView list = getListView();
        int first = list.getFirstVisiblePosition();
        int last = list.getLastVisiblePosition();
        for (int i = first; i <= last; i++) {
            if (id == list.getItemIdAtPosition(i)) {
                return i - first;
            }
        }
        return -1;
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
