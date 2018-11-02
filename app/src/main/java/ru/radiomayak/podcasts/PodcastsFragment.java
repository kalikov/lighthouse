package ru.radiomayak.podcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseFragment;
import ru.radiomayak.NetworkUtils;
import ru.radiomayak.R;
import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.widget.ToolbarCompat;

public class PodcastsFragment extends LighthouseFragment implements PodcastIconLoader.Listener {
    public static final String TAG = PodcastsFragment.class.getName();

    private static final String STATE_CONTENT_VIEW = PodcastsFragment.class.getName() + "$contentView";

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

    private int firstVisibleItem;
    private int visibleItemCount;

    private boolean hasFavoriteChanges;

    private int sortingMessage;

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
                    updatePodcastRow(podcast);
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
            if (!isDetached()) {
                showErrorView();
            }
        }
    };

    @VisibleForTesting
    final Loader.Listener<Podcasts> podcastsSortingListener = new Loader.Listener<Podcasts>() {
        @Override
        public void onComplete(Loader<Podcasts> loader, Podcasts data) {
            podcastsFuture = null;
            if (isDetached()) {
                return;
            }
            if (data != null && !data.list().isEmpty()) {
                updatePodcasts(data.list());
            }
            toast(sortingMessage, Toast.LENGTH_SHORT);
            getRefreshView().setRefreshing(false);
        }

        @Override
        public void onException(Loader<Podcasts> loader, Throwable exception) {
            podcastsFuture = null;
            toast(sortingMessage, Toast.LENGTH_SHORT);
            getRefreshView().setRefreshing(false);
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
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setRetainInstance(true);

        LighthouseActivity activity = requireLighthouseActivity();

        IntentFilter filter = new IntentFilter();
        filter.addAction(RecordsActivity.ACTION_VIEW);
        activity.registerReceiver(viewReceiver, filter);

        podcastsLoaderManager = activity.getLighthouseApplication().getModule().createLoaderManager();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.podcasts, container, false);
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        boolean requestList = podcasts == null || podcasts.list().isEmpty();
        if (state != null) {
            requestList = !state.getBoolean(STATE_CONTENT_VIEW, true);
        }
        if (podcasts == null) {
            podcasts = new Podcasts();
        }

        LighthouseActivity activity = requireLighthouseActivity();
        if (adapter == null || adapter.getActivity() != activity) {
            adapter = new PodcastsAdapter(activity, podcasts.list());
            initializeAdapter();
        }
        initializeView();

        if (requestList) {
            requestLoopback();
        } else {
            showContentView();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        if (podcasts.list().isEmpty()) {
            state.putBoolean(STATE_CONTENT_VIEW, getErrorView().getVisibility() == View.VISIBLE);
        } else {
            state.putBoolean(STATE_CONTENT_VIEW, true);
//            FragmentManager fragmentManager = requireFragmentManager();
//            PodcastsDataFragment dataFragment = (PodcastsDataFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
//            if (dataFragment == null) {
//                dataFragment = new PodcastsDataFragment();
//                fragmentManager.beginTransaction().add(dataFragment, FRAGMENT_TAG).commit();
//            }
//            dataFragment.setPodcasts(podcasts);
        }
        super.onSaveInstanceState(state);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.podcasts_menu, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.settings:
//                openSettings();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    @Override
    public void onDestroy() {
        LighthouseActivity activity = requireLighthouseActivity();
        activity.unregisterReceiver(viewReceiver);

        if (podcastsFuture != null && !podcastsFuture.isDone()) {
            podcastsFuture.cancel(true);
            podcastsFuture = null;
        }
        podcasts = null;
        super.onDestroy();
    }

    private void initializeAdapter() {
        adapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Podcast podcast = findContainingAdapterItem(view);
                if (podcast != null) {
                    openPodcast(podcast);
                }
            }
        });
        adapter.setFavoriteClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Podcast podcast = findContainingAdapterItem(view);
                if (podcast != null) {
                    togglePodcastRating(podcast);
                }
            }
        });
    }

    private void initializeView() {
        initializeToolbar();

        initializeLoadingView();
        initializeErrorView();
        initializeRecyclerView();

        initializeRefreshView();
    }

    private void initializeToolbar() {
        Toolbar actionBar = getToolbar();
        actionBar.setTitle(R.string.podcasts);
        ToolbarCompat.setTitleTypeface(actionBar, requireLighthouseActivity().getLighthouseApplication().getFontNormal());
        toolbarTextSize = ToolbarCompat.getTitleTextSize(actionBar);

        requireLighthouseActivity().setSupportActionBar(actionBar);
    }

    private void initializeLoadingView() {
        TextView text = getLoadingView().findViewById(android.R.id.progress);
        text.setText(R.string.podcasts_loading);
        text.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());
    }

    private void initializeErrorView() {
        View errorView = getErrorView();
        TextView text = errorView.findViewById(android.R.id.text1);
        text.setText(R.string.podcasts_error);
        text.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());

        Button retryButton = errorView.findViewById(R.id.retry);
        retryButton.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestRemoteList();
            }
        });
    }

    private void initializeRecyclerView() {
        RecyclerView view = getRecyclerView();
        view.setLayoutManager(new LinearLayoutManager(requireActivity()));
        view.setAdapter(adapter);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView view, int scrollState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                requestIcons(false);
            }
        });
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
    Toolbar getToolbar() {
        return requireActivity().findViewById(R.id.toolbar);
    }

    @VisibleForTesting
    RecyclerView getRecyclerView() {
        return requireActivity().findViewById(android.R.id.list);
    }

    @VisibleForTesting
    View getLoadingView() {
        return requireActivity().findViewById(R.id.loading);
    }

    @VisibleForTesting
    View getErrorView() {
        return requireActivity().findViewById(R.id.error);
    }

    @VisibleForTesting
    SwipeRefreshLayout getRefreshView() {
        return (SwipeRefreshLayout) requireActivity().findViewById(R.id.refresh);
    }

    private void showLoadingView() {
        getLoadingView().setVisibility(View.VISIBLE);
        getErrorView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setRefreshing(false);
        getRefreshView().setEnabled(false);
    }

    private void showErrorView() {
        getLoadingView().setVisibility(View.GONE);
        getErrorView().setVisibility(View.VISIBLE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setRefreshing(false);
        getRefreshView().setEnabled(true);
    }

    private void showListView() {
        getLoadingView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.VISIBLE);
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

    private boolean isRefreshViewEnabled() {
        return getRefreshView().isRefreshing() || isListViewScrollOnTop(getRecyclerView());
    }

    private static boolean isListViewScrollOnTop(RecyclerView listView) {
        if (listView.getChildCount() == 0) {
            return true;
        }
        View firstChild = listView.getChildAt(0);
        return listView.getChildAdapterPosition(firstChild) <= 0 && firstChild.getTop() >= 0;
    }

    private void requestLoopback() {
        if (podcastsFuture != null) {
            return;
        }
        podcastsFuture = podcastsLoaderManager.execute(requireActivity(), podcastsLoopback, podcastsLoopbackListener);
        showLoadingView();
    }

    @VisibleForTesting
    void requestLoopbackSorting(int messageId) {
        if (podcastsFuture != null) {
            return;
        }
        if (!hasFavoriteChanges) {
            toast(messageId, Toast.LENGTH_SHORT);
            getRefreshView().setRefreshing(false);
        } else {
            sortingMessage = messageId;
            podcastsFuture = podcastsLoaderManager.execute(getContext(), podcastsLoopback, podcastsSortingListener);
        }
    }

    private void requestRemoteList() {
        if (podcastsFuture != null) {
            return;
        }
        boolean isConnected = NetworkUtils.isConnected(requireActivity());
        if (adapter.isEmpty() || isConnected) {
            podcastsFuture = podcastsLoaderManager.execute(requireActivity(), new PodcastsLoader(), podcastsListener);
            if (adapter.isEmpty()) {
                showLoadingView();
            }
        } else {
            requestLoopbackSorting(R.string.toast_no_connection);
        }
    }

    private void onLoopbackComplete(@Nullable Podcasts data) {
        podcastsFuture = null;
        LighthouseActivity activity = getLighthouseActivity();
        if (activity != null) {
            if (data != null && !data.list().isEmpty()) {
                updatePodcasts(data.list());
            }
            boolean isConnected = NetworkUtils.isConnected(requireActivity());
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
    }

    private void onRemoteListComplete(@Nullable final Podcasts data) {
        podcastsFuture = null;
        if (isDetached()) {
            return;
        }
        if (isVisible()) {
            getToolbar().setTitle(R.string.podcasts);
            ToolbarCompat.setTitleTextSize(getToolbar(), toolbarTextSize);
        }
        if ((data == null || data.list().isEmpty()) && !adapter.isEmpty()) {
            requestLoopbackSorting(R.string.toast_loading_error);
        } else {
            if (data != null && !data.list().isEmpty()) {
                if (adapter.isEmpty()) {
                    for (Podcast podcast : data.list()) {
                        podcast.setSeen(podcast.getLength());
                    }
                }
                updatePodcasts(data.list());
                LighthouseApplication.NETWORK_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        PodcastsOpenHelper helper = new PodcastsOpenHelper(getContext());
                        try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
                            database.storePodcasts(data);
                        }
                    }
                });
            }
            showContentView();
        }
    }

    private void updatePodcasts(List<Podcast> list) {
        boolean notifyDataSetChanged = adapter.isEmpty();

        Collection<Podcast> remainingPodcasts = new HashSet<>(podcasts.list());

        int index = 0;
        int ratingIndex = 0;
        for (Podcast item : list) {
            Podcast podcast = podcasts.get(item.getId());
            if (podcast == null) {
                podcasts.add(index, item);
                notifyDataSetChanged = true;
            } else {
                remainingPodcasts.remove(podcast);
                item.setFavorite(podcast.getFavorite());
                boolean updated = podcast.update(item);
                if (updated && !notifyDataSetChanged) {
                    updatePodcastRow(podcast);
                }
                int targetIndex = index;
                if (podcast.getFavorite() != 0) {
                    targetIndex = ratingIndex;
                    ratingIndex++;
                }
                if (targetIndex != podcasts.list().indexOf(podcast)) {
                    podcasts.remove(podcast);
                    podcasts.add(targetIndex, podcast);
                    notifyDataSetChanged = true;
                }
            }
            index++;
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
            requestIcons(true);
        }
        hasFavoriteChanges = false;
    }

    private void requestIcons(boolean force) {
        RecyclerView view = getRecyclerView();
        int visibleItemCount = view.getChildCount();
        if (visibleItemCount <= 0) {
            return;
        }
        View firstItem = view.getChildAt(0);
        int firstVisibleItem = view.getChildAdapterPosition(firstItem);
        getRefreshView().setEnabled(isRefreshViewEnabled());
        if (visibleItemCount != this.visibleItemCount || firstVisibleItem != this.firstVisibleItem || force) {
            this.firstVisibleItem = firstVisibleItem;
            this.visibleItemCount = visibleItemCount;
            requestIcons(firstVisibleItem, visibleItemCount);
        }
    }

    private void requestIcons(int firstVisiblePosition, int visibleItemsCount) {
        int count = adapter.getItemCount();
        int first = Math.max(0, firstVisiblePosition - visibleItemsCount / 2);
        int last = Math.min(count - 1, firstVisiblePosition + visibleItemsCount - 1 + visibleItemsCount / 2);
        for (int i = Math.max(0, firstVisiblePosition); i <= last; i++) {
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
        futures.put(id, requireLighthouseActivity().getLighthouseApplication().getImageLoaderManager().execute(getContext(), loader, podcastIconListener));
    }

    @Override
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
        if (isDetached()) {
            return;
        }
        if (bitmapInfo != null) {
            PodcastImageCache.getInstance().putIcon(id, bitmapInfo);
            Podcast podcast = podcasts.get(id);
            if (bitmapInfo.getPrimaryColor() != 0 || bitmapInfo.getSecondaryColor() != 0) {
                Image icon = Objects.requireNonNull(podcast.getIcon());
                icon.setColors(bitmapInfo.getPrimaryColor(), bitmapInfo.getSecondaryColor());
            }
            updatePodcastRow(podcast);
        }
    }

    private boolean isRowInIconLoadingRange(long id) {
        RecyclerView recyclerView = getRecyclerView();
        int visible = recyclerView.getChildCount();
        int first = visible > 0 ? recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0)) : 0;
        int last = first + visible - 1;
        int start = Math.max(0, first - visible / 2);
        int end = Math.min(adapter.getItemCount() - 1, last + visible / 2);
        for (int i = start; i <= end; i++) {
            if (id == adapter.getItemId(i)) {
                return true;
            }
        }
        return false;
    }

    private void updatePodcastRows() {
        RecyclerView recyclerView = getRecyclerView();
        for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
            View view = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
            if (viewHolder != null && viewHolder instanceof PodcastsAdapter.ViewHolder) {
                Podcast podcast = podcasts.get(viewHolder.getItemId());
                if (podcast != null) {
                    ((PodcastsAdapter.ViewHolder) viewHolder).bind(podcast);
                }
            }
        }
    }

    private void updatePodcastRow(Podcast podcast) {
        RecyclerView recyclerView = getRecyclerView();
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForItemId(podcast.getId());
        if (viewHolder != null && viewHolder instanceof PodcastsAdapter.ViewHolder) {
            ((PodcastsAdapter.ViewHolder) viewHolder).bind(podcast);
        }
    }

    @VisibleForTesting
    void toast(int id, int duration) {
        Toast.makeText(getContext(), id, duration).show();
    }

    private void openPodcast(Podcast podcast) {
//        Intent intent = new Intent(getContext(), RecordsActivity.class);
//        intent.putExtra(RecordsActivity.EXTRA_PODCAST, podcast);
//        startActivity(intent);
        requireLighthouseActivity().openPodcast(podcast);
    }

    private void togglePodcastRating(final Podcast podcast) {
        hasFavoriteChanges = true;
        podcast.setFavorite(podcast.getFavorite() == 0 ? 1 : 0);
        updatePodcastRow(podcast);
        LighthouseApplication.NETWORK_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                PodcastsOpenHelper helper = new PodcastsOpenHelper(getContext());
                try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
                    database.storePodcastRating(podcast.getId(), podcast.getFavorite());
                }
            }
        });
    }

    @Nullable
    private Podcast findContainingAdapterItem(View view) {
        RecyclerView.ViewHolder viewHolder = getRecyclerView().findContainingViewHolder(view);
        if (viewHolder == null) {
            return null;
        }
        int position = viewHolder.getAdapterPosition();
        if (position >= 0) {
            return adapter.getItem(position);
        }
        return null;
    }

    @Override
    public void updatePlayerState() {
        adapter.updateEqualizerAnimation();

        updatePodcastRows();
    }

//    private void openSettings() {
//        Intent intent = new Intent(this, SettingsActivity.class);
//        startActivity(intent);
//    }
}