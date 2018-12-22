package ru.radiomayak.podcasts;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Future;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseFragment;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;
import ru.radiomayak.StringUtils;
import ru.radiomayak.TrackId;
import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.widget.ToolbarCompat;

public class HistoryFragment extends LighthouseFragment implements PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener {
    public static final String TAG = HistoryFragment.class.getName() + "$";

    private static final String STATE_CONTENT_VIEW = HistoryFragment.class.getName() + "$contentView";

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 501;

    @VisibleForTesting
    final Loader.Listener<HistoryPage> tracksListener = new Loader.Listener<HistoryPage>() {
        @Override
        public void onComplete(Loader<HistoryPage> loader, HistoryPage data) {
            onTracksLoaded(data);
        }

        @Override
        public void onException(Loader<HistoryPage> loader, Throwable exception) {
            onTracksLoaded(null);
        }
    };

    @VisibleForTesting
    final Loader.Listener<HistoryPage> pageListener = new Loader.Listener<HistoryPage>() {
        @Override
        public void onComplete(Loader<HistoryPage> loader, HistoryPage data) {
            onPageLoaded(data);
        }

        @Override
        public void onException(Loader<HistoryPage> loader, Throwable exception) {
            onPageLoaded(null);
        }
    };

    @VisibleForTesting
    LoaderManager<HistoryPage> loaderManager;

    @VisibleForTesting
    Future<HistoryPage> tracksFuture;

    @VisibleForTesting
    HistoryAdapter adapter;
    private ItemTouchHelper touchHelper;

    private HistoryTracks tracks;

    private Future<HistoryPage> pageFuture;

    private int cursor;
    private boolean pageFailed;

    private HistoryTrack contextTrack;
    private HistoryTrack permissionRecord;

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setRetainInstance(true);

        loaderManager = requireLighthouseActivity().getLighthouseApplication().getModule().createLoaderManager();
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        boolean requestList = tracks == null || tracks.list().isEmpty();

        if (state != null) {
            boolean isContentView = state.getBoolean(STATE_CONTENT_VIEW, true);
            requestList = !isContentView;
        }
        if (tracks == null) {
            tracks = new HistoryTracks();
        }

        if (adapter == null) {
            adapter = new HistoryAdapter(this, tracks);
        }
        if (touchHelper == null) {
            touchHelper = new ItemTouchHelper(new HistorySwipeCallback(requireLighthouseActivity().getLighthouseApplication()) {
                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    HistoryTrack track = adapter.getItem(viewHolder.getAdapterPosition());
                    remove(track);
                }
            });
        }

        initializeView();

        if (requestList) {
            requestList();
        }
        updateView();
    }

    @Override
    public void onDetach() {
        touchHelper.attachToRecyclerView(null);

        super.onDetach();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.remove:
                remove(contextTrack);
                return true;
            case R.id.download:
                LighthouseActivity activity = requireLighthouseActivity();
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionRecord = contextTrack;
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    download(contextTrack);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    download(permissionRecord);
                }
            }
        }
    }

    private void download(HistoryTrack track) {
        Record record = track.getRecord();
        Uri uri = Uri.parse(record.getUrl());
        String filename = StringUtils.toFilename(record.getName()) + ".mp3";

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(record.getName());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, filename);

        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Toast.makeText(getContext(), R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
        } else {
            downloadManager.enqueue(request);
        }
    }

    private void remove(HistoryTrack track) {
        tracks.remove(track);
        adapter.notifyDataSetChanged();

        new RemoveHistoryTrackAsyncTask(getContext()).executeOnExecutor(LighthouseApplication.DATABASE_SERIAL_EXECUTOR, track);
        requireLighthouseActivity().onHistoryTrackRemoved(track.getPodcast().getId(), track.getRecord().getId());

        if (tracks.isEmpty()) {
            updateView();
        }
    }

    @Override
    public void onDestroy() {
        if (pageFuture != null && !pageFuture.isCancelled()) {
            pageFuture.cancel(true);
        }
        if (tracksFuture != null && !tracksFuture.isCancelled()) {
            tracksFuture.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.history, container, false);
    }

    private void initializeView() {
        initializeToolbar();

        initializeLoadingView();
        initializeEmptyView();
        initializeRecyclerView();

        initializeRefreshView();
    }

    private void initializeToolbar() {
        final Toolbar toolbar = getToolbar();
        toolbar.setTitle(R.string.history);

        LighthouseActivity activity = requireLighthouseActivity();
        ToolbarCompat.setTitleTypeface(toolbar, activity.getLighthouseApplication().getFontNormal());
        activity.setSupportActionBar(toolbar);

        if (activity.isNavigateBackSupported()) {
            ActionBar actionBar = Objects.requireNonNull(activity.getSupportActionBar());
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    private void initializeLoadingView() {
        TextView text = getLoadingView().findViewById(android.R.id.progress);
        text.setText(R.string.records_loading);
        text.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());
    }

    private void initializeEmptyView() {
        TextView text = getEmptyView();
        text.setText(R.string.history_empty);
        text.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());
    }

    private void initializeRecyclerView() {
        final RecyclerView view = getRecyclerView();
        view.setLayoutManager(new LinearLayoutManager(getContext()));
        view.setAdapter(adapter);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView view, int scrollState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (pageFuture == null && adapter.getFooterMode() == HistoryAdapter.FooterMode.LOADING && isFooterVisible(view)) {
                    requestNextPage();
                }
            }

            private boolean isFooterVisible(RecyclerView view) {
                int last = adapter.getItemCount() - 1;
                if (adapter.getItemViewType(last) != HistoryAdapter.FOOTER_VIEW_TYPE) {
                    return false;
                }
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(last);
                return viewHolder != null;
            }
        });
    }

    private void updateView() {
        if (getView() == null) {
            return;
        }
        if (tracks.isEmpty()) {
            if (tracksFuture != null) {
                showLoadingView();
            } else {
                showEmptyView();
            }
        } else {
            showRecyclerView();
        }
    }

    private void initializeRefreshView() {
        getRefreshView().setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshTracks();
            }
        });
        getRefreshView().setColorSchemeResources(R.color.colorPrimary);
        getRefreshView().setEnabled(true);
    }

    private Toolbar getToolbar() {
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
    TextView getEmptyView() {
        return requireActivity().findViewById(R.id.empty);
    }

    @VisibleForTesting
    SwipeRefreshLayout getRefreshView() {
        return requireActivity().findViewById(R.id.refresh);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        if (tracks == null || tracks.isEmpty()) {
            state.putBoolean(STATE_CONTENT_VIEW, tracks != null && tracksFuture == null);
        } else {
            state.putBoolean(STATE_CONTENT_VIEW, true);
        }
        super.onSaveInstanceState(state);
    }

    private void requestList() {
        if (tracksFuture != null) {
            return;
        }
        Context context = requireContext();
        tracksFuture = loaderManager.execute(context, new HistoryLoader(), tracksListener);
    }

    private void requestNextPage() {
        if (cursor == 0 || pageFuture != null) {
            return;
        }
        Context context = requireContext();
        pageFuture = loaderManager.execute(context, new HistoryLoader(cursor), pageListener);
    }

    private void showLoadingView() {
        getLoadingView().setVisibility(View.VISIBLE);
        getEmptyView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setEnabled(false);
        getRefreshView().setRefreshing(false);
    }

    private void showEmptyView() {
        touchHelper.attachToRecyclerView(null);
        getLoadingView().setVisibility(View.GONE);
        getEmptyView().setVisibility(View.VISIBLE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setEnabled(true);
        getRefreshView().setRefreshing(false);
    }

    private void showRecyclerView() {
        getLoadingView().setVisibility(View.GONE);
        getEmptyView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.VISIBLE);
        touchHelper.attachToRecyclerView(getRecyclerView());

        if (tracksFuture != null) {
            if (tracks.isEmpty()) {
                adapter.setFooterMode(HistoryAdapter.FooterMode.LOADING);
                getRefreshView().setEnabled(false);
                getRefreshView().setRefreshing(false);
            }
        } else {
            if (pageFuture == null) {
                getRefreshView().setRefreshing(false);
            }
            if (cursor > 0) {
                if (pageFuture != null || !pageFailed) {
                    adapter.setFooterMode(HistoryAdapter.FooterMode.LOADING);
                } else {
                    adapter.setFooterMode(HistoryAdapter.FooterMode.MORE);
                }
            } else {
                if (!tracks.isEmpty()) {
                    adapter.setFooterMode(HistoryAdapter.FooterMode.HIDDEN);
                } else {
                    adapter.setFooterMode(HistoryAdapter.FooterMode.ERROR);
                }
            }
        }
    }

    public void onTracksLoaded(HistoryPage response) {
        tracksFuture = null;
        if (getView() != null) {
            updateFirstPageTracks(response);
            getRefreshView().setEnabled(true);
            updateView();
        }
    }

    private void updateFirstPageTracks(HistoryPage response) {
        boolean notifyDataSetChanged = tracks.isEmpty();
        if (response != null) {
            Collection<HistoryTrack> remainingTracks = new HashSet<>(tracks.list());

            int index = 0;
            for (HistoryTrack item : response.getTracks().list()) {
                HistoryTrack track = tracks.get(item.getId());
                if (track == null) {
                    tracks.add(index, item);
                    notifyDataSetChanged = true;
                } else {
                    remainingTracks.remove(track);
                    boolean updated = track.update(item);
                    if (updated && !notifyDataSetChanged) {
                        updateTrackRow(track);
                    }
                    if (index != tracks.list().indexOf(track)) {
                        tracks.remove(track);
                        tracks.add(index, track);
                        notifyDataSetChanged = true;
                    }
                }
                index++;
            }
            if (!remainingTracks.isEmpty()) {
                notifyDataSetChanged = true;
                tracks.removeAll(remainingTracks);
            }
            this.cursor = response.getCursor();
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
        }
    }

    public void onPageLoaded(HistoryPage response) {
        pageFuture = null;
        pageFailed = response == null;
        if (getView() != null) {
            if (response != null) {
                updatePageRecords(response);
            }
            updateView();
        }
    }

    private void updatePageRecords(HistoryPage response) {
        boolean notifyDataSetChanged = tracks.isEmpty();

        for (HistoryTrack item : response.getTracks().list()) {
            HistoryTrack track = tracks.get(item.getId());
            if (track == null) {
                tracks.add(item);
                notifyDataSetChanged = true;
            }
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
        }
        this.cursor = response.getCursor();
    }

    private void updateTrackRow(HistoryTrack track) {
        RecyclerView view = getRecyclerView();
        RecyclerView.ViewHolder viewHolder = view.findViewHolderForItemId(track.getId().asLong());
        if (viewHolder instanceof HistoryAdapter.ItemViewHolder) {
            ((HistoryAdapter.ItemViewHolder) viewHolder).bind(track);
        }
    }

    void playRecord(HistoryTrack track) {
        LighthouseActivity activity = requireLighthouseActivity();
        LighthouseTrack currentTrack = activity.getTrack();
        if (currentTrack != null && currentTrack.getId().equals(track.getId())) {
            if (activity.isPlaying()) {
                activity.pause();
            } else {
                activity.play();
            }
            return;
        }

        try {
            activity.setTrack(new LighthouseTrack(track.getPodcast(), track.getRecord()));
        } catch (Exception e) {
            Toast.makeText(activity, R.string.player_failed, Toast.LENGTH_SHORT).show();
        }
    }

    void loadMore() {
        requestNextPage();
        updateView();
    }

    void refreshTracks() {
        requestList();
        updateView();
    }

    private void updateTracksRows() {
        RecyclerView recyclerView = getRecyclerView();
        for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
            View view = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
            if (holder != null && holder.getItemViewType() == HistoryAdapter.ITEM_VIEW_TYPE) {
                HistoryTrack track = adapter.getItem(holder.getAdapterPosition());
                if (track != null) {
                    ((HistoryAdapter.ItemViewHolder) holder).updatePlayPauseState(track);
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        RecyclerView.ViewHolder viewHolder = getRecyclerView().findContainingViewHolder(view);
        if (viewHolder == null) {
            return;
        }
        contextTrack = adapter.getItem(viewHolder.getAdapterPosition());
        if (contextTrack == null) {
            return;
        }
        requireActivity().getMenuInflater().inflate(R.menu.history_track_menu, menu);
        menu.setHeaderTitle(contextTrack.getRecord().getName());
    }

    public void onCreatePopupMenu(HistoryTrack track, View view) {
        contextTrack = track;
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.history_track_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.setOnDismissListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onContextItemSelected(item);
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        contextTrack = null;
    }

    @Override
    public void updatePlayerState() {
        adapter.updateEqualizerAnimation(requireLighthouseActivity().isPlaying());
        updateTracksRows();
    }

    void updateTrackPlaybackAttributes(int state, long podcast, long record, long position, long duration) {
        HistoryTrack track = tracks.get(new TrackId(podcast, record));
        if (track != null) {
            Record trackRecord = track.getRecord();
            trackRecord.setPosition((int) position);
            trackRecord.setLength((int) duration);
            if (getView() != null && (state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_ERROR || state == PlaybackStateCompat.STATE_NONE)) {
                updateTrackRow(track);
            }
        }
    }
}
