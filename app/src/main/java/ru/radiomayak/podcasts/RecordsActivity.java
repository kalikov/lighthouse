package ru.radiomayak.podcasts;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.NetworkUtils;
import ru.radiomayak.R;
import ru.radiomayak.StringUtils;
import ru.radiomayak.widget.ToolbarCompat;

public class RecordsActivity extends LighthouseActivity
        implements PodcastAsyncTask.Listener, PageAsyncTask.Listener, PodcastSplashAsyncTask.Listener, RecordsPlayer {

    public static final String EXTRA_PODCAST = "ru.radiomayak.podcasts.PODCAST";

    private static final double SPLASH_ASPECT_RATIO = 0.3;

    private RecordsAdapter adapter;

    private Podcast podcast;
    private Records records;

    private PodcastAsyncTask podcastAsyncTask;
    private PodcastSplashAsyncTask splashAsyncTask;
    private PageAsyncTask pageAsyncTask;

    private RecordsPaginator paginator;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        podcast = getPodcast();
        if (podcast == null) {
            finish();
            return;
        }
        records = new Records();

        adapter = new RecordsAdapter(getLighthouseApplication(), this, records.list(), this);

        initializeView();

        requestPodcast();
        requestPodcastSplash();
    }

    @Nullable
    private Podcast getPodcast() {
        return getIntent().getParcelableExtra(EXTRA_PODCAST);
    }

    private void initializeView() {
        setContentView(R.layout.podcast);

        initializeToolbar();

        initializeLoadingView();
        initializeErrorView();
        initializeRecyclerView();

        initializeRefreshView();

        initializePlayerView();
    }

    private void initializeToolbar() {
        final Toolbar toolbar = getToolbar();
        toolbar.setTitle("");
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        setSupportActionBar(toolbar);

        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        AppBarLayout appBarLayout = getAppBarLayout();
        int appBarHeight = (int) (appBarLayout.getWidth() * SPLASH_ASPECT_RATIO);
        appBarLayout.layout(appBarLayout.getLeft(), appBarLayout.getTop(), appBarLayout.getRight(), appBarHeight);
        appBarLayout.setVisibility(View.GONE);

        final TextView toolbarTitle = getToolbarTitle();
        toolbarTitle.setText(podcast.getName());
        toolbarTitle.setVisibility(View.GONE);
        toolbarTitle.setTypeface(getLighthouseApplication().getFontNormal());

        final ImageView toolbarImage = getToolbarImage();

        final CollapsingToolbarLayout toolbarLayout = getToolbarLayout();
        toolbarLayout.setTitleEnabled(false);

        getAppBarLayout().addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            private int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                getRefreshView().setEnabled(verticalOffset >= 0);
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                int toolbarHeight = toolbar.getHeight();
                int collapsingSize = scrollRange + verticalOffset;
                if (collapsingSize <= 0) {
                    toolbarImage.setVisibility(View.GONE);
                    toolbarTitle.setVisibility(View.VISIBLE);
                    int offset = toolbar.getTop();
                    toolbarTitle.layout(toolbarTitle.getLeft(), offset, toolbarTitle.getRight(), offset + toolbarTitle.getHeight());
                } else if (toolbarImage.getDrawable() == null) {
                    toolbarImage.setVisibility(View.GONE);
                    toolbarTitle.setVisibility(View.VISIBLE);
                    int offset = toolbar.getTop() + collapsingSize;
                    toolbarTitle.layout(toolbarTitle.getLeft(), offset, toolbarTitle.getRight(), offset + toolbarTitle.getHeight());
                } else if (collapsingSize > toolbarHeight) {
                    toolbarImage.setVisibility(View.VISIBLE);
                    toolbarImage.setImageAlpha((int) (255f * (collapsingSize - toolbarHeight) / (scrollRange - toolbarHeight)));
                    toolbarTitle.setVisibility(View.GONE);
                } else {
                    toolbarImage.setVisibility(View.GONE);
                    toolbarTitle.setVisibility(View.VISIBLE);
                    int offset = toolbar.getTop() + 2 * collapsingSize;
                    toolbarTitle.layout(toolbarTitle.getLeft(), offset, toolbarTitle.getRight(), offset + toolbarTitle.getHeight());
                }
            }
        });

        updateToolbarColor();
    }

    private void updateToolbarColor() {
        int primaryColor = 0;
        int secondaryColor = 0;
        if (podcast.getSplash() != null && podcast.getSplash().getPrimaryColor() != 0) {
            primaryColor = podcast.getSplash().getPrimaryColor();
            secondaryColor = podcast.getSplash().getSecondaryColor();
        } else if (podcast.getIcon() != null && podcast.getIcon().getPrimaryColor() != 0) {
            primaryColor = podcast.getIcon().getPrimaryColor();
            secondaryColor = podcast.getIcon().getSecondaryColor();
        }

        if (primaryColor != 0) {
            if (secondaryColor == 0) {
                int textColor = ResourcesCompat.getColor(getResources(), R.color.titleTextColor, getTheme());
                int textColorInverse = ResourcesCompat.getColor(getResources(), R.color.titleTextColorInverse, getTheme());
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
            ToolbarCompat.setTitleTypeface(toolbar, getLighthouseApplication().getFontBold());

            Drawable homeIcon = toolbar.getNavigationIcon();
            if (homeIcon != null) {
                homeIcon.setColorFilter(secondaryColor, PorterDuff.Mode.MULTIPLY);
            }

            getToolbarTitle().setTextColor(secondaryColor);
        } else {
            Toolbar toolbar = getToolbar();
            Drawable homeIcon = toolbar.getNavigationIcon();
            if (homeIcon != null) {
                int textColor = ResourcesCompat.getColor(getResources(), R.color.titleTextColor, getTheme());
                homeIcon.setColorFilter(textColor, PorterDuff.Mode.MULTIPLY);
            }
        }
    }

    private void initializeLoadingView() {
        TextView text = (TextView) getLoadingView().findViewById(android.R.id.progress);
        text.setText(R.string.records_loading);
        text.setTypeface(getLighthouseApplication().getFontNormal());
    }

    private void initializeErrorView() {
        TextView text = (TextView) getErrorView();
        text.setText(R.string.records_error);
        text.setTypeface(getLighthouseApplication().getFontNormal());
    }

    private void initializeRecyclerView() {
        RecyclerView view = getRecyclerView();
        view.setLayoutManager(new LinearLayoutManager(this));

        view.setAdapter(adapter);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {
            }

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if (pageAsyncTask == null && adapter.getFooterMode() == RecordsAdapter.FooterMode.LOADING && isFooterVisible(view)) {
                    requestNextPage();
                }
            }

            private boolean isFooterVisible(RecyclerView view) {
                if (adapter.getItemViewType(adapter.getItemCount() - 1) != RecordsAdapter.FOOTER_VIEW_TYPE) {
                    return false;
                }
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(adapter.getItemCount() - 1);
                return viewHolder != null;
            }
        });
    }

    private boolean isScrollOnTop() {
        return getAppBarLayout().getTop() >= 0;
    }

    private void initializeRefreshView() {
        getRefreshView().setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestPodcast();
            }
        });
        getRefreshView().setColorSchemeResources(R.color.colorPrimary);
        getRefreshView().setEnabled(true);
    }

    private AppBarLayout getAppBarLayout() {
        return (AppBarLayout) findViewById(R.id.app_bar);
    }

    private Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.toolbar);
    }

    private CollapsingToolbarLayout getToolbarLayout() {
        return (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
    }

    private TextView getToolbarTitle() {
        return (TextView) findViewById(android.R.id.title);
    }

    private ImageView getToolbarImage() {
        return (ImageView) findViewById(android.R.id.icon);
    }

    private RecyclerView getRecyclerView() {
        return (RecyclerView) findViewById(android.R.id.list);
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

    private void requestPodcast() {
        if (podcastAsyncTask != null) {
            return;
        }
        if (adapter.isEmpty() || NetworkUtils.isConnected(this)) {
            podcastAsyncTask = new PodcastAsyncTask(this, this);
            if (adapter.isEmpty()) {
                podcastAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, podcast.getId(), PodcastAsyncTask.LOOPBACK);
                showLoadingView();
            } else {
                podcastAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, podcast.getId());
            }
        } else {
            Toast.makeText(this, R.string.toast_no_connection, Toast.LENGTH_SHORT).show();
            showContentView();
        }
    }

    private void requestNextPage() {
        if (paginator == null || pageAsyncTask != null) {
            return;
        }
        pageAsyncTask = new PageAsyncTask(this, this);
        pageAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, paginator);
    }

    private void showLoadingView() {
        getLoadingView().setVisibility(View.VISIBLE);
        getErrorView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.GONE);
        getRefreshView().setEnabled(false);
        getRefreshView().setRefreshing(false);
    }

    private void showErrorView() {
        getLoadingView().setVisibility(View.GONE);
        getErrorView().setVisibility(View.VISIBLE);
        getRefreshView().setEnabled(true);
        getRefreshView().setRefreshing(false);
    }

    private void showListView() {
        getAppBarLayout().setVisibility(View.VISIBLE);
        getLoadingView().setVisibility(View.GONE);
        getRecyclerView().setVisibility(View.VISIBLE);
        getRefreshView().setEnabled(isScrollOnTop());
        getRefreshView().setRefreshing(false);
    }

    private void showContentView() {
        if (adapter.isEmpty()) {
            showErrorView();
        } else {
            showListView();
        }
    }

    @Override
    public void onPodcastLoaded(PodcastResponse response, boolean isCancelled) {
        podcastAsyncTask = null;
        if (!isCancelled) {
            if (response.getPaginator() == null && !adapter.isEmpty()) {
                Toast.makeText(this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
            } else {
                if (response.getPodcast() != null) {
                    updatePodcast(response.getPodcast());
                }
                updateFirstPageRecords(response.getPaginator());
            }
        }
        showContentView();
    }

    private void updatePodcast(Podcast podcast) {
        boolean updated = this.podcast.merge(podcast);
        if (updated) {
            requestPodcastSplash();
        }
    }

    private void updateFirstPageRecords(RecordsPaginator paginator) {
        boolean notifyDataSetChanged = adapter.isEmpty();
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
                    boolean updated = record.merge(item);
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
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
            getRefreshView().setEnabled(isScrollOnTop());
        }
        this.paginator = paginator;
        updateLoadMoreView();
    }

    @Override
    public void onPageLoaded(RecordsPaginator response, boolean isCancelled) {
        pageAsyncTask = null;
        if (!isCancelled) {
            if (response == null) {
                Toast.makeText(this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
                adapter.setFooterMode(RecordsAdapter.FooterMode.BUTTON);
            } else {
                updatePageRecords(response);
            }
        }
    }

    private void updateLoadMoreView() {
        if (paginator != null && paginator.hasNext()) {
            adapter.setFooterMode(RecordsAdapter.FooterMode.LOADING);
        } else {
            adapter.setFooterMode(RecordsAdapter.FooterMode.HIDDEN);
        }
    }

    private void requestPodcastSplash() {
        Image splash = podcast.getSplash();
        if (splash == null || splashAsyncTask != null || getToolbarImage().getDrawable() != null) {
            return;
        }
        splashAsyncTask = new PodcastSplashAsyncTask(this, this);
        splashAsyncTask.executeOnExecutor(LighthouseApplication.NETWORK_POOL_EXECUTOR, podcast);
    }

    private void updatePageRecords(RecordsPaginator paginator) {
        boolean recordsChanged = false;
        boolean notifyDataSetChanged = adapter.isEmpty();

        for (Record item : paginator.current()) {
            Record record = records.get(item.getId());
            if (record == null) {
                records.add(item);
                recordsChanged = true;
                notifyDataSetChanged = true;
            } else {
                boolean update = false;
                if (item.getDescription() != null) {
                    update = !StringUtils.equals(item.getDescription(), record.getDescription());
                    record.setDescription(item.getDescription());
                }
                if (update && !notifyDataSetChanged) {
                    recordsChanged = true;
                    updateRecordRow(record);
                }
            }
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
            getRefreshView().setEnabled(isScrollOnTop());
        }
        this.paginator = paginator;
        updateLoadMoreView();

        if (recordsChanged) {
        }
    }

    private void updateRecordRow(Record record) {
        RecyclerView view = getRecyclerView();
        RecyclerView.ViewHolder viewHolder = view.findViewHolderForItemId(record.getId());
        if (viewHolder != null && viewHolder instanceof RecordsAdapter.ItemViewHolder) {
            ((RecordsAdapter.ItemViewHolder) viewHolder).bind(record);
        }
    }

    @Override
    public void onPodcastSplashResponse(LongSparseArray<BitmapInfo> array, boolean isCancelled) {
        podcastAsyncTask = null;
        BitmapInfo bitmapInfo = array.get(podcast.getId());
        if (bitmapInfo == null || isCancelled) {
            return;
        }
        getToolbarImage().setImageBitmap(bitmapInfo.getBitmap());
        if (bitmapInfo.getPrimaryColor() != 0) {
            Image splash = Objects.requireNonNull(podcast.getSplash());
            splash.setColors(bitmapInfo.getPrimaryColor(), bitmapInfo.getSecondaryColor());
            updateToolbarColor();
        }
    }

    @Override
    public void playRecord(Record record) {
        LighthouseTrack track = getTrack();
        if (track != null && track.getRecord().getId() == record.getId()) {
            if (isPlaying()) {
                pause();
            } else {
                play();
            }
            updatePlayerView();
            return;
        }

        try {
            setTrack(new LighthouseTrack(podcast, record));
        } catch (Exception e) {
            Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();
        }

        updatePlayerView();
    }

    @Override
    public void loadMore() {
        adapter.setFooterMode(RecordsAdapter.FooterMode.LOADING);
        requestNextPage();
    }
}
