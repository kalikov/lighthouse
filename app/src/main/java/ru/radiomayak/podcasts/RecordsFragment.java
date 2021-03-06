package ru.radiomayak.podcasts;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import ru.radiomayak.NetworkUtils;
import ru.radiomayak.R;
import ru.radiomayak.StringUtils;
import ru.radiomayak.animation.Interpolators;
import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.widget.ToolbarCompat;

public class RecordsFragment extends LighthouseFragment implements PageAsyncTask.Listener,
        PopupMenu.OnDismissListener, PopupMenu.OnMenuItemClickListener {
    public static final String TAG = RecordsFragment.class.getName() + "$";

    public static final String EXTRA_PODCAST = RecordsFragment.class.getPackage().getName() + ".PODCAST";

    private static final String STATE_CONTENT_VIEW = RecordsFragment.class.getName() + "$contentView";

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 501;

    @VisibleForTesting
    final Loader.Listener<BitmapInfo> splashListener = new Loader.Listener<BitmapInfo>() {
        @Override
        public void onComplete(Loader<BitmapInfo> loader, BitmapInfo data) {
            onSplashLoadComplete(data);
        }

        @Override
        public void onException(Loader<BitmapInfo> loader, Throwable exception) {
            splashFuture = null;
        }
    };

    @VisibleForTesting
    final Loader.Listener<PodcastResponse> podcastListener = new Loader.Listener<PodcastResponse>() {
        @Override
        public void onComplete(Loader<PodcastResponse> loader, PodcastResponse data) {
            onPodcastLoaded(data);
        }

        @Override
        public void onException(Loader<PodcastResponse> loader, Throwable exception) {
            podcastFuture = null;
            updateView();
        }
    };

    @VisibleForTesting
    LoaderManager<PodcastResponse> podcastLoaderManager;

    @VisibleForTesting
    Future<PodcastResponse> podcastFuture;

    @VisibleForTesting
    RecordsAdapter adapter;

    private Podcast podcast;
    private Records records;

    @VisibleForTesting
    Future<BitmapInfo> splashFuture;

    @VisibleForTesting
    boolean splashSet;
    private float maxAlpha = -1.0f;
    private ValueAnimator alphaAnimator;

    private PageAsyncTask pageAsyncTask;

    private RecordsPaginator paginator;
    private boolean pageFailed;

    private Record contextRecord;
    private Record permissionRecord;

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setRetainInstance(true);

        podcastLoaderManager = requireLighthouseActivity().getLighthouseApplication().getModule().createLoaderManager();
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        boolean requestList = records == null || records.list().isEmpty();

        if (state != null) {
            boolean isContentView = state.getBoolean(STATE_CONTENT_VIEW, true);
            requestList = !isContentView;
            getAppBarLayout().setExpanded(false);
        }
        if (podcast == null) {
            podcast = getPodcast();
        }
        if (podcast == null) {
            requireFragmentManager().popBackStack();
            return;
        }

        if (records == null) {
            records = new Records();
        }

        if (adapter == null) {
            adapter = new RecordsAdapter(this, podcast, records.list());
        }

        initializeView();

        if (requestList) {
            requestPodcast();
        }
        BitmapInfo splashInfo = PodcastImageCache.getInstance().getSplash(podcast.getId());
        if (splashInfo != null) {
            setPodcastSplash(splashInfo);
        } else {
            splashSet = false;
            requestPodcastSplash();
        }
        updateView();
    }

    @Override
    public void onViewStateRestored(Bundle state) {
        super.onViewStateRestored(state);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                LighthouseActivity activity = requireLighthouseActivity();
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionRecord = contextRecord;
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    download(contextRecord);
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

    private void download(Record record) {
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

    @Override
    public void onPause() {
        if (alphaAnimator != null) {
            alphaAnimator.removeAllUpdateListeners();
            alphaAnimator.cancel();
        }
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (pageAsyncTask != null && !pageAsyncTask.isCancelled()) {
            pageAsyncTask.cancel(true);
        }
        if (podcastFuture != null && !podcastFuture.isCancelled()) {
            podcastFuture.cancel(true);
        }
        if (splashFuture != null && !splashFuture.isDone()) {
            splashFuture.cancel(true);
            splashFuture = null;
        }
        super.onDestroy();
    }

    @Nullable
    public Podcast getPodcast() {
        Bundle arguments = getArguments();
        return arguments != null ? (Podcast) arguments.getParcelable(EXTRA_PODCAST) : null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.podcast, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initializeView() {
        initializeToolbar();

        initializeLoadingView();
        initializeErrorView();
        initializeRecyclerView();

        initializeRefreshView();
    }

    private void initializeToolbar() {
        final Toolbar toolbar = getToolbar();
        toolbar.setTitle("");
        toolbar.setBackgroundColor(Color.TRANSPARENT);

        LighthouseActivity activity = requireLighthouseActivity();
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = Objects.requireNonNull(activity.getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        AppBarLayout appBarLayout = getAppBarLayout();
        appBarLayout.setVisibility(View.GONE);

        final TextView toolbarTitle = getToolbarTitle();
        toolbarTitle.setText(podcast.getName());
        toolbarTitle.setVisibility(View.GONE);
        toolbarTitle.setTypeface(activity.getLighthouseApplication().getFontNormal());

        final ImageView toolbarImage = getToolbarImage();

        final CollapsingToolbarLayout toolbarLayout = getToolbarLayout();
        toolbarLayout.setTitleEnabled(false);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            private int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset >= 0 && podcastFuture == null) {
                    getRefreshView().setEnabled(true);
                } else if (!getRefreshView().isRefreshing()) {
                    getRefreshView().setEnabled(false);
                }
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                int collapsingSize = scrollRange + verticalOffset;
                updateToolbarControls(toolbarImage, toolbarTitle, collapsingSize, scrollRange);
            }
        });

        updateToolbarColor();
    }

    private void updateToolbarControls(ImageView toolbarImage, TextView toolbarTitle, int collapsingSize, int scrollRange) {
        if (collapsingSize <= 0) {
            toolbarImage.setVisibility(View.GONE);
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setAlpha(1.0f);
            maxAlpha = 1.0f;
            if (alphaAnimator != null) {
                alphaAnimator.cancel();
            }
        } else if (!splashSet) {
            toolbarImage.setVisibility(View.GONE);
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setAlpha(1.0f);
        } else if (collapsingSize > scrollRange / 2) {
            toolbarImage.setVisibility(View.VISIBLE);
            float alpha = (2.0f * collapsingSize - scrollRange) / scrollRange;
            if (alpha <= maxAlpha) {
                maxAlpha = 1.0f;
                if (alphaAnimator != null) {
                    alphaAnimator.cancel();
                }
            } else if (maxAlpha >= 0) {
                alpha = maxAlpha;
            } else {
                alpha = 0;
            }
            toolbarImage.setImageAlpha((int) (255f * alpha));

            if (maxAlpha < 0) {
                toolbarTitle.setVisibility(View.VISIBLE);
                toolbarTitle.setAlpha(-maxAlpha);
            } else {
                toolbarTitle.setVisibility(View.GONE);
            }
        } else {
            toolbarImage.setVisibility(View.GONE);
            toolbarTitle.setVisibility(View.VISIBLE);
            float alpha = 1.0f - 2f * collapsingSize / scrollRange;
            if (alpha >= -maxAlpha) {
                maxAlpha = 1.0f;
                if (alphaAnimator != null) {
                    alphaAnimator.cancel();
                }
            } else if (maxAlpha < 0) {
                alpha = -maxAlpha;
            }
            toolbarTitle.setAlpha(alpha);
        }
    }

    private void updateToolbarColor() {
        int primaryColor = 0;
        int secondaryColor = 0;
        if (podcast.getSplash() != null && podcast.getSplash().hasColor()) {
            primaryColor = podcast.getSplash().getPrimaryColor();
            secondaryColor = podcast.getSplash().getSecondaryColor();
        } else if (podcast.getIcon() != null && podcast.getIcon().hasColor()) {
            primaryColor = podcast.getIcon().getPrimaryColor();
            secondaryColor = podcast.getIcon().getSecondaryColor();
        }

        LighthouseActivity activity = requireLighthouseActivity();
        if (primaryColor != 0) {
            if (secondaryColor == 0) {
                int textColor = ResourcesCompat.getColor(getResources(), R.color.titleTextColor, activity.getTheme());
                int textColorInverse = ResourcesCompat.getColor(getResources(), R.color.titleTextColorInverse, activity.getTheme());
                if (ColorUtils.calculateContrast(textColor, primaryColor) >= ColorUtils.calculateContrast(textColorInverse, primaryColor)) {
                    secondaryColor = textColor;
                } else {
                    secondaryColor = textColorInverse;
                }
            }
            CollapsingToolbarLayout toolbarLayout = getToolbarLayout();
            toolbarLayout.setBackgroundColor(primaryColor);

            toolbarLayout.setExpandedTitleColor(secondaryColor);
            toolbarLayout.setCollapsedTitleTextColor(secondaryColor);

            Toolbar toolbar = getToolbar();
            toolbar.setTitleTextColor(secondaryColor);
            ToolbarCompat.setTitleTypeface(toolbar, activity.getLighthouseApplication().getFontBold());

            Drawable homeIcon = toolbar.getNavigationIcon();
            if (homeIcon != null) {
                homeIcon.setColorFilter(secondaryColor, PorterDuff.Mode.MULTIPLY);
            }
            Drawable menuIcon = toolbar.getOverflowIcon();
            if (menuIcon != null) {
                menuIcon.setColorFilter(secondaryColor, PorterDuff.Mode.MULTIPLY);
            }

            getToolbarTitle().setTextColor(secondaryColor);
        } else {
            Toolbar toolbar = getToolbar();
            Drawable homeIcon = toolbar.getNavigationIcon();
            if (homeIcon != null) {
                int textColor = ResourcesCompat.getColor(getResources(), R.color.titleTextColor, activity.getTheme());
                homeIcon.setColorFilter(textColor, PorterDuff.Mode.MULTIPLY);
            }
        }
    }

    private void initializeLoadingView() {
        TextView text = getLoadingView().findViewById(android.R.id.progress);
        text.setText(R.string.records_loading);
        text.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());
    }

    private void initializeErrorView() {
        View errorView = getErrorView();
        TextView text = errorView.findViewById(android.R.id.text1);
        text.setText(R.string.records_error);
        text.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());

        Button retryButton = errorView.findViewById(R.id.retry);
        retryButton.setTypeface(requireLighthouseActivity().getLighthouseApplication().getFontNormal());
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshPodcast();
            }
        });
    }

    private void initializeRecyclerView() {
        final RecyclerView view = getRecyclerView();
        view.setNestedScrollingEnabled(false);
        view.setLayoutManager(new LinearLayoutManager(getContext()) {
            @Override
            public void onLayoutCompleted(RecyclerView.State state) {
                super.onLayoutCompleted(state);

                if (!view.isNestedScrollingEnabled()) {
                    setAppBarCollapsingEnabled(hasContentScroll());
                }
            }
        });

        view.setAdapter(adapter);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView view, int scrollState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (pageAsyncTask == null && adapter.getFooterMode() == RecordsAdapter.FooterMode.LOADING && isFooterVisible(view)) {
                    requestNextPage();
                }
            }

            private boolean isFooterVisible(RecyclerView view) {
                int last = adapter.getItemCount() - 1;
                if (adapter.getItemViewType(last) != RecordsAdapter.FOOTER_VIEW_TYPE) {
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
        if (!splashSet && records.isEmpty()) {
            if (podcastFuture != null) {
                showLoadingView();
            } else {
                showErrorView();
            }
        } else {
            showRecyclerView();
        }
    }

    private void initializeRefreshView() {
        getRefreshView().setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPodcast();
            }
        });
        getRefreshView().setColorSchemeResources(R.color.colorPrimary);
        getRefreshView().setEnabled(true);
    }

    @VisibleForTesting
    AppBarLayout getAppBarLayout() {
        return requireActivity().findViewById(R.id.app_bar);
    }

    private Toolbar getToolbar() {
        return requireActivity().findViewById(R.id.toolbar);
    }

    private CollapsingToolbarLayout getToolbarLayout() {
        return (CollapsingToolbarLayout) requireActivity().findViewById(R.id.toolbar_layout);
    }

    private TextView getToolbarTitle() {
        return getAppBarLayout().findViewById(android.R.id.title);
    }

    private ImageView getToolbarImage() {
        return requireActivity().findViewById(android.R.id.icon);
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
        return requireActivity().findViewById(R.id.refresh);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        if (records == null || records.list().isEmpty()) {
            state.putBoolean(STATE_CONTENT_VIEW, records != null && podcastFuture == null);
        } else {
            state.putBoolean(STATE_CONTENT_VIEW, true);
        }
        super.onSaveInstanceState(state);
    }

    private void requestPodcast() {
        if (podcastFuture != null) {
            return;
        }
        Context context = requireContext();
        if (records.isEmpty() || NetworkUtils.isConnected(context)) {
            podcastFuture = podcastLoaderManager.execute(context, new PodcastLoader(podcast.getId()), podcastListener);
        } else {
            Toast.makeText(context, R.string.toast_no_connection, Toast.LENGTH_SHORT).show();
            getRefreshView().setRefreshing(false);
        }
    }

    private void requestNextPage() {
        if (paginator == null || pageAsyncTask != null) {
            return;
        }
        pageAsyncTask = new PageAsyncTask(requireLighthouseActivity().getLighthouseApplication(), this);
        pageAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, paginator);
    }

    private void showLoadingView() {
        getAppBarLayout().setVisibility(View.GONE);
        getLoadingView().setVisibility(View.VISIBLE);
        getErrorView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setEnabled(false);
        getRefreshView().setRefreshing(false);
    }

    private void showErrorView() {
        getAppBarLayout().setVisibility(View.GONE);
        getLoadingView().setVisibility(View.GONE);
        getErrorView().setVisibility(View.VISIBLE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setEnabled(true);
        getRefreshView().setRefreshing(false);
    }

    private void showRecyclerView() {
        getAppBarLayout().setVisibility(View.VISIBLE);
        getLoadingView().setVisibility(View.GONE);
        getErrorView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.VISIBLE);

        if (podcastFuture != null) {
            if (records.isEmpty()) {
                adapter.setFooterMode(RecordsAdapter.FooterMode.LOADING);
                getRefreshView().setEnabled(false);
                getRefreshView().setRefreshing(false);
            }
        } else {
            if (pageAsyncTask == null) {
                getRefreshView().setRefreshing(false);
            }
            if (paginator != null && paginator.hasNext()) {
                if (pageAsyncTask != null || !pageFailed) {
                    adapter.setFooterMode(RecordsAdapter.FooterMode.LOADING);
                } else {
                    adapter.setFooterMode(RecordsAdapter.FooterMode.MORE);
                }
            } else {
                if (!records.isEmpty()) {
                    adapter.setFooterMode(RecordsAdapter.FooterMode.HIDDEN);
                } else {
                    adapter.setFooterMode(RecordsAdapter.FooterMode.ERROR);
                }
            }
        }
    }

    public void onPodcastLoaded(PodcastResponse response) {
        podcastFuture = null;
        if (getView() != null) {
            if (response.getPaginator() == null && !records.isEmpty()) {
                Toast.makeText(requireContext(), R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
            } else {
                if (response.getPodcast() != null) {
                    updatePodcast(response.getPodcast(), response.isArchived());
                }
                updateFirstPageRecords(response.getPaginator());
            }
            getRefreshView().setEnabled(true);
            updateView();
        }
    }


    private void updatePodcast(Podcast podcast, Boolean isArchived) {
        boolean hasNoLength = this.podcast.getLength() <= 0;
        podcast.setFavorite(this.podcast.getFavorite());
        boolean updated = this.podcast.update(podcast);
        adapter.setIsArchived(isArchived);
        if (updated) {
            if (hasNoLength && isArchived == null) {
                adapter.notifyItemInserted(0);
            } else {
                adapter.notifyItemChanged(0);
            }
            requestPodcastSplash();
        }
    }

    private void updateFirstPageRecords(RecordsPaginator paginator) {
        boolean notifyDataSetChanged = records.isEmpty();
        if (paginator != null) {
            Collection<Record> remainingRecords = new HashSet<>(records.list());

            int index = 0;
            for (Record item : paginator.current()) {
                Record record = records.get(item.getId());
                if (record == null) {
                    records.add(index, item);
                    notifyDataSetChanged = true;
                    index++;
                } else {
                    remainingRecords.remove(record);
                    boolean updated = record.update(item);
                    if (updated && !notifyDataSetChanged) {
                        updateRecordRow(record);
                    }
                    index = records.list().indexOf(record);
                }
            }
            if (!remainingRecords.isEmpty()) {
                notifyDataSetChanged = true;
                for (Record record : remainingRecords) {
                    records.remove(record);
                }
            }
            new StorePodcastSeenAsyncTask(getContext()).executeOnExecutor(LighthouseApplication.DATABASE_SERIAL_EXECUTOR, podcast);
            requireLighthouseActivity().onPodcastSeen(podcast.getId(), podcast.getLength());
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
        }
        this.paginator = paginator;
    }

    private boolean hasContentScroll() {
        RecyclerView recyclerView = getRecyclerView();
        int offset = recyclerView.computeVerticalScrollOffset();
        int range = recyclerView.computeVerticalScrollRange() - recyclerView.computeVerticalScrollExtent();
        return range > 0 && (offset > 0 || offset < range - 1);
    }

    private void setAppBarCollapsingEnabled(boolean isEnabled) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getAppBarLayout().getLayoutParams();
        if (params != null) {
            AppBarLayout.Behavior behaviour = ((AppBarLayout.Behavior) params.getBehavior());
            if (behaviour != null) {
                behaviour.setDragCallback(isEnabled ? null : new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                        return false;
                    }
                });
            }
        }
        getRecyclerView().setNestedScrollingEnabled(isEnabled);

//        if (isEnabled && params != null) {
//            AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
//            behavior.onNestedPreScroll((CoordinatorLayout) requireActivity().findViewById(android.R.id.button3), getAppBarLayout(), null, 0, 30, new int[]{0, 0}, 0);
//            getAppBarLayout().requestLayout();
//        }
    }

    @Override
    public void onPageLoaded(RecordsPaginator response, boolean isCancelled) {
        pageAsyncTask = null;
        pageFailed = response == null;
        if (getView() != null) {
            if (!isCancelled) {
                if (response == null) {
                    Toast.makeText(getContext(), R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
                } else {
                    updatePageRecords(response);
                }
            }
            updateView();
        }
    }

    private void updatePageRecords(RecordsPaginator paginator) {
        boolean notifyDataSetChanged = records.isEmpty();

        for (Record item : paginator.current()) {
            Record record = records.get(item.getId());
            if (record == null) {
                records.add(item);
                notifyDataSetChanged = true;
            } else {
                boolean update = false;
                if (item.getDescription() != null) {
                    update = !StringUtils.equals(item.getDescription(), record.getDescription());
                    record.setDescription(item.getDescription());
                }
                if (update && !notifyDataSetChanged) {
                    updateRecordRow(record);
                }
            }
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
        }
        this.paginator = paginator;
    }

    private void updateRecordRow(Record record) {
        RecyclerView view = getRecyclerView();
        RecyclerView.ViewHolder viewHolder = view.findViewHolderForItemId(record.getId());
        if (viewHolder instanceof RecordsAdapter.ItemViewHolder) {
            ((RecordsAdapter.ItemViewHolder) viewHolder).bind(record);
        }
    }

    private void requestPodcastSplash() {
        Image icon = podcast.getIcon();
        Image splash = podcast.getSplash();
        if (splash == null && (icon == null || !PictureUrlUtils.isPictureUrl(icon.getUrl())) || splashFuture != null || splashSet) {
            return;
        }
        PodcastSplashLoader loader = new PodcastSplashLoader(podcast);
        splashFuture = requireLighthouseActivity().getLighthouseApplication().getImageLoaderManager().execute(getContext(), loader, splashListener);
    }

    public void onSplashLoadComplete(BitmapInfo bitmapInfo) {
        splashFuture = null;
        if (bitmapInfo == null || getView() == null) {
            return;
        }
        PodcastImageCache.getInstance().setSplash(podcast.getId(), bitmapInfo);
        setPodcastSplash(bitmapInfo);
        updateView();
    }

    private void setPodcastSplash(BitmapInfo bitmapInfo) {
        setPodcastSplash(bitmapInfo.getBitmap());
        if (bitmapInfo.getPrimaryColor() != 0 || bitmapInfo.getSecondaryColor() != 0) {
            Image splash = podcast.getSplash();
            if (splash == null) {
                Image icon = Objects.requireNonNull(podcast.getIcon());
                splash = new Image(PictureUrlUtils.getPictureUrl(icon.getUrl(), PictureUrlUtils.Size.L));
                podcast.setSplash(splash);
            }
            splash.setColors(bitmapInfo.getPrimaryColor(), bitmapInfo.getSecondaryColor());
            updateToolbarColor();
        }
    }

    private void setPodcastSplash(Bitmap bitmap) {
        splashSet = true;
        getToolbarImage().setImageBitmap(bitmap);
        if (podcastFuture != null) {
            maxAlpha = 1.0f;
            return;
        }
        alphaAnimator = ValueAnimator.ofFloat(-1.0f, 1.0f);
        alphaAnimator.setInterpolator(Interpolators.LINEAR);
        alphaAnimator.setDuration(800);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                maxAlpha = (float) animation.getAnimatedValue();
                int scrollRange = getAppBarLayout().getTotalScrollRange();
                updateToolbarControls(getToolbarImage(), getToolbarTitle(), scrollRange + getAppBarLayout().getVerticalScrollbarPosition(), scrollRange);
            }
        });
        alphaAnimator.start();
    }

    void playRecord(Record record) {
        LighthouseActivity activity = requireLighthouseActivity();
        LighthouseTrack track = activity.getTrack();
        if (track != null && track.getRecord().getId() == record.getId()) {
            if (activity.isPlaying()) {
                activity.pause();
            } else {
                activity.play();
            }
            return;
        }

        try {
            activity.setTrack(new LighthouseTrack(podcast, record));
        } catch (Exception e) {
            Toast.makeText(activity, R.string.player_failed, Toast.LENGTH_SHORT).show();
        }
    }

    void loadMore() {
        requestNextPage();
        updateView();
    }

    void refreshPodcast() {
        requestPodcast();
        updateView();
    }

    private void updateRecordsRows() {
        RecyclerView recyclerView = getRecyclerView();
        for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
            View view = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
            if (holder != null && holder.getItemViewType() == RecordsAdapter.ITEM_VIEW_TYPE) {
                Record record = adapter.getItem(holder.getAdapterPosition());
                if (record != null) {
                    ((RecordsAdapter.ItemViewHolder) holder).updatePlayPauseState(record);
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
        contextRecord = adapter.getItem(viewHolder.getAdapterPosition());
        if (contextRecord == null) {
            return;
        }
        requireActivity().getMenuInflater().inflate(R.menu.record_menu, menu);
        menu.setHeaderTitle(contextRecord.getName());
    }

    public void onCreatePopupMenu(Record record, View view) {
        contextRecord = record;
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.record_menu, popup.getMenu());
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
        contextRecord = null;
    }

    @Override
    public void updatePlayerState() {
        adapter.updateEqualizerAnimation(requireLighthouseActivity().isPlaying());
        updateRecordsRows();
    }

    void updateRecordPlaybackAttributes(int state, long record, long position, long duration) {
        Record podcastRecord = records.get(record);
        if (podcastRecord != null) {
            podcastRecord.setPosition((int) position);
            podcastRecord.setLength((int) duration);
            if (getView() != null && (state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_ERROR || state == PlaybackStateCompat.STATE_NONE)) {
                updateRecordRow(podcastRecord);
            }
        }
    }

    void updateRecordPosition(long record, long position) {
        Record podcastRecord = records.get(record);
        if (podcastRecord != null) {
            podcastRecord.setPosition((int) position);
            if (getView() != null) {
                updateRecordRow(podcastRecord);
            }
        }
    }
}
