package ru.radiomayak.podcasts;

import android.animation.ValueAnimator;
import android.app.FragmentManager;
import android.graphics.Bitmap;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Future;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.NetworkUtils;
import ru.radiomayak.R;
import ru.radiomayak.StringUtils;
import ru.radiomayak.animation.Interpolators;
import ru.radiomayak.content.Loader;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.widget.ToolbarCompat;

public class RecordsActivity extends LighthouseActivity implements PodcastAsyncTask.Listener, PageAsyncTask.Listener {

    public static final String ACTION_VIEW = RecordsActivity.class.getPackage().getName() + ".view";

    public static final String EXTRA_PODCAST = RecordsActivity.class.getPackage().getName() + ".PODCAST";
    public static final String EXTRA_PODCAST_ID = RecordsActivity.class.getPackage().getName() + ".PODCAST_ID";
    public static final String EXTRA_SEEN = RecordsActivity.class.getPackage().getName() + ".SEEN";

    private static final String STATE_CONTENT_VIEW = PodcastsActivity.class.getName() + "$contentView";
    private static final String STATE_FOOTER = RecordsActivity.class.getName() + "$footer";

    private static final String FRAGMENT_TAG = RecordsActivity.class.getName() + "$data";

    private final Loader.OnLoadListener<BitmapInfo> splashOnLoadListener = new Loader.OnLoadListener<BitmapInfo>() {
        @Override
        public void onLoadComplete(Loader<BitmapInfo> loader, BitmapInfo data) {
            onSplashLoadComplete(data);
        }
    };

    private RecordsAdapter adapter;

    private Podcast podcast;
    private Records records;

    private Bitmap splash;
    private float maxAlpha = -1.0f;
    private ValueAnimator alphaAnimator;

    private PodcastAsyncTask podcastAsyncTask;
    private PageAsyncTask pageAsyncTask;

    private Future<BitmapInfo> splashFuture;

    private RecordsPaginator paginator;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        boolean requestList = true;

        RecordsAdapter.FooterMode footerMode = null;
        if (state != null) {
            boolean isContentView = state.getBoolean(STATE_CONTENT_VIEW, true);
            if (isContentView) {
                FragmentManager fragmentManager = getFragmentManager();
                RecordsDataFragment dataFragment = (RecordsDataFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
                if (dataFragment != null) {
                    podcast = state.getParcelable(Podcast.class.getName());
                    records = dataFragment.getRecords();
                    paginator = dataFragment.getPaginator();
                    requestList = records == null || records.list().isEmpty();
                    footerMode = paginator != null && paginator.hasNext() ? RecordsAdapter.FooterMode.LOADING : RecordsAdapter.FooterMode.HIDDEN;
                    footerMode = RecordsAdapter.FooterMode.values()[state.getInt(STATE_FOOTER, footerMode.ordinal())];
                }
            }
        }
        if (podcast == null) {
            podcast = getPodcast();
        }
        if (podcast == null) {
            finish();
            return;
        }
        BitmapInfo splashInfo = PodcastImageCache.getInstance().getSplash(podcast.getId());
        if (records == null) {
            records = new Records();
        }

        adapter = new RecordsAdapter(this, podcast, records.list());
        if (footerMode != null) {
            adapter.setFooterMode(footerMode);
        }

        initializeView();

        if (requestList) {
            requestPodcast();
        } else {
            showContentView();
        }
        if (splashInfo != null) {
            setPodcastSplash(splashInfo);
        } else {
            requestPodcastSplash();
        }
    }

    @Override
    protected void onDestroy() {
        if (alphaAnimator != null) {
            alphaAnimator.cancel();
        }
        if (pageAsyncTask != null && !pageAsyncTask.isCancelled()) {
            pageAsyncTask.cancel(true);
        }
        if (podcastAsyncTask != null && !podcastAsyncTask.isCancelled()) {
            podcastAsyncTask.cancel(true);
        }
        if (splashFuture != null && !splashFuture.isDone()) {
            splashFuture.cancel(true);
            splashFuture = null;
        }
        super.onDestroy();
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
        } else if (splash == null) {
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

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (records.list().isEmpty()) {
            state.putBoolean(STATE_CONTENT_VIEW, getErrorView().getVisibility() == View.VISIBLE);
        } else {
            state.putBoolean(STATE_CONTENT_VIEW, true);
            state.putParcelable(Podcast.class.getName(), podcast);

            FragmentManager fragmentManager = getFragmentManager();
            RecordsDataFragment dataFragment = (RecordsDataFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
            if (dataFragment == null) {
                dataFragment = new RecordsDataFragment();
                fragmentManager.beginTransaction().add(dataFragment, FRAGMENT_TAG).commit();
            }
            dataFragment.setRecords(records);
            dataFragment.setPaginator(paginator);
            state.putInt(STATE_FOOTER, adapter.getFooterMode().ordinal());
        }
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        getAppBarLayout().setExpanded(false);
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
            getRefreshView().setRefreshing(false);
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
        getRecyclerView().setVisibility(View.GONE);
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
        if (!isCancelled && !isDestroyed()) {
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
            if (!adapter.isEmpty()) {
                adapter.notifyItemChanged(0);
            }
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
            new PodcastViewAsyncTask(this).executeOnExecutor(LighthouseApplication.NETWORK_SERIAL_EXECUTOR, podcast);
        }
        if (notifyDataSetChanged) {
            adapter.notifyDataSetChanged();
            getRefreshView().setEnabled(isScrollOnTop());
        }
        this.paginator = paginator;
        updateFooterMode();
    }

    @Override
    public void onPageLoaded(RecordsPaginator response, boolean isCancelled) {
        pageAsyncTask = null;
        if (!isCancelled && !isDestroyed()) {
            if (response == null) {
                Toast.makeText(this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
                adapter.setFooterMode(RecordsAdapter.FooterMode.BUTTON);
            } else {
                updatePageRecords(response);
            }
        }
    }

    private void updateFooterMode() {
        if (paginator != null && paginator.hasNext()) {
            adapter.setFooterMode(RecordsAdapter.FooterMode.LOADING);
        } else {
            adapter.setFooterMode(RecordsAdapter.FooterMode.HIDDEN);
        }
    }

    private void requestPodcastSplash() {
        Image icon = podcast.getIcon();
        Image splash = podcast.getSplash();
        if (splash == null && (icon == null || !PictureUrlUtils.isPictureUrl(icon.getUrl())) || splashFuture != null || this.splash != null) {
            return;
        }
        PodcastSplashLoader loader = new PodcastSplashLoader(this, podcast);
        splashFuture = getLighthouseApplication().getImageLoaderManager().execute(loader, splashOnLoadListener);
    }

    private void updatePageRecords(RecordsPaginator paginator) {
        boolean notifyDataSetChanged = adapter.isEmpty();

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
            getRefreshView().setEnabled(isScrollOnTop());
        }
        this.paginator = paginator;
        updateFooterMode();
    }

    private void updateRecordRow(Record record) {
        RecyclerView view = getRecyclerView();
        RecyclerView.ViewHolder viewHolder = view.findViewHolderForItemId(record.getId());
        if (viewHolder != null && viewHolder instanceof RecordsAdapter.ItemViewHolder) {
            ((RecordsAdapter.ItemViewHolder) viewHolder).bind(record);
        }
    }

    public void onSplashLoadComplete(BitmapInfo bitmapInfo) {
        podcastAsyncTask = null;
        if (bitmapInfo == null || isDestroyed()) {
            return;
        }
        PodcastImageCache.getInstance().setSplash(podcast.getId(), bitmapInfo);
        setPodcastSplash(bitmapInfo);
    }

    private void setPodcastSplash(BitmapInfo bitmapInfo) {
        setPodcastSplash(bitmapInfo.getBitmap());
        if (bitmapInfo.getPrimaryColor() != 0) {
            Image splash = podcast.getSplash();
            if (splash == null) {
                Image icon = Objects.requireNonNull(podcast.getIcon());
                splash = new Image(PictureUrlUtils.getPictureUrl(icon.getUrl(), PictureUrlUtils.Size.L));
            }
            splash.setColors(bitmapInfo.getPrimaryColor(), bitmapInfo.getSecondaryColor());
            updateToolbarColor();
        }
    }

    private void setPodcastSplash(Bitmap bitmap) {
        splash = bitmap;
        getToolbarImage().setImageBitmap(bitmap);
        if (podcastAsyncTask != null) {
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
        LighthouseTrack track = getTrack();
        if (track != null && track.getRecord().getId() == record.getId()) {
            if (isPlaying()) {
                pause();
            } else {
                play();
            }
            return;
        }

        try {
            setTrack(new LighthouseTrack(podcast, record));
        } catch (Exception e) {
            Toast.makeText(this, R.string.player_failed, Toast.LENGTH_SHORT).show();
        }
    }

    void loadMore() {
        adapter.setFooterMode(RecordsAdapter.FooterMode.LOADING);
        requestNextPage();
    }

    @Override
    protected void updatePlayerView(boolean animate) {
        super.updatePlayerView(animate);

        adapter.updateEqualizerAnimation();
        updateRecordsRows();
    }

    @Override
    protected void updateRecordPlayedState(Podcast podcast, Record record) {
        super.updateRecordPlayedState(podcast, record);
        if (this.podcast != null && this.podcast.getId() == podcast.getId()) {
            Record adapterRecord = records.get(record.getId());
            if (adapterRecord != null && adapterRecord != record) {
                adapterRecord.setPlayed(record.isPlayed());
            }
            updateRecordRow(record);
        }
    }

    private void updateRecordsRows() {
        RecyclerView recyclerView = getRecyclerView();
        for (int i = 0, n = recyclerView.getChildCount(); i < n; i++) {
            View view = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
            if (holder != null && holder.getItemViewType() == RecordsAdapter.ITEM_VIEW_TYPE) {
                Record record = adapter.getItem(holder.getAdapterPosition());
                ((RecordsAdapter.ItemViewHolder) holder).updatePlayPauseState(record);
            }
        }
    }
}
