package ru.radiomayak.podcasts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.Semaphore;

import ru.radiomayak.content.Loader;
import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.graphics.BitmapInfo;

@RunWith(AndroidJUnit4.class)
public class RecordsFragmentTest {
    @Rule
    public final ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class, false, false);

    private MainActivity activity;
    private RecordsFragment fragment;

    @Before
    public void before() {
        Context context = InstrumentationRegistry.getTargetContext();

        Podcast podcast = new Podcast(1, "test");
        podcast.setIcon(new Image(PictureUrlUtils.getPictureUrl("x/vh/pictures/ad/12/33/55.jpg", PictureUrlUtils.Size.L)));

        PodcastImageCache.getInstance().splashs.evictAll();

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_PODCAST, podcast);
        activity = rule.launchActivity(intent);
        fragment = (RecordsFragment) activity.getSupportFragmentManager().findFragmentByTag(RecordsFragment.TAG + podcast.getId());
    }

    @Test
    public void shouldBeInLoadingViewOnCreate() {
        assertLoadingView();
        Mockito.verify(fragment.podcastLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastLoader.class), Mockito.eq(fragment.podcastListener));

        LoaderManager<BitmapInfo> imageManager = activity.getLighthouseApplication().getImageLoaderManager();
        Mockito.verify(imageManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastSplashLoader.class), Mockito.eq(fragment.splashListener));

        Assert.assertNotNull(fragment.splashFuture);
    }

    @Test
    public void shouldBeInContentViewOnSplashCompleteAfterCreate() throws InterruptedException {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                fragment.splashListener.onComplete(Mockito.mock(Loader.class), new BitmapInfo(bitmap, 0, 0));
            }
        });
        assertRecyclerLoadingView();

        Assert.assertNull(fragment.splashFuture);
        Assert.assertSame(bitmap, fragment.splash);
    }

    @Test
    public void shouldBeInLoadingViewOnSplashFailAfterCreate() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                fragment.splashListener.onException(Mockito.mock(Loader.class), new Exception());
            }
        });
        assertLoadingView();

        Assert.assertNull(fragment.splashFuture);
        Assert.assertNull(fragment.splash);
    }

    @Test
    public void shouldBeInContentViewOnPodcastCompleteAfterCreate() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                Podcast remotePodcast = new Podcast(1, "test");
                remotePodcast.setLength(10);

                Records records = new Records();
                records.add(new Record(2, "name", "url"));
                RecordsPaginator paginator = new OnlineRecordsPaginator(1, records, 0);
                fragment.podcastListener.onComplete(Mockito.mock(Loader.class), new PodcastResponse(remotePodcast, paginator));
            }
        });
        assertRecyclerView();

        Assert.assertNotNull(fragment.splashFuture);
        Assert.assertNull(fragment.splash);
    }

    @Test
    public void shouldBeInErrorViewOnPodcastFailAfterCreate() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                fragment.podcastListener.onException(Mockito.mock(Loader.class), new Exception());
            }
        });
        assertErrorView();

        Assert.assertNotNull(fragment.splashFuture);
        Assert.assertNull(fragment.splash);
    }

    @Test
    public void shouldBeInContentViewOnSplashCompleteAfterPodcastFail() throws InterruptedException {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                fragment.podcastListener.onException(Mockito.mock(Loader.class), new Exception());
                fragment.splashListener.onComplete(Mockito.mock(Loader.class), new BitmapInfo(bitmap, 0, 0));
            }
        });
        assertRecyclerErrorView();

        Assert.assertNull(fragment.splashFuture);
        Assert.assertSame(bitmap, fragment.splash);
    }

    private void assertLoadingView() {
        Assert.assertEquals(View.GONE, fragment.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertFalse(fragment.getRefreshView().isEnabled());

        Assert.assertNotNull(fragment.podcastFuture);
    }

    private void assertErrorView() {
        Assert.assertEquals(View.GONE, fragment.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertTrue(fragment.getRefreshView().isEnabled());

        Assert.assertNull(fragment.podcastFuture);
    }

    private void assertRecyclerLoadingView() {
        Assert.assertEquals(View.VISIBLE, fragment.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertFalse(fragment.getRefreshView().isEnabled());

        Assert.assertEquals(RecordsAdapter.FooterMode.LOADING, fragment.adapter.getFooterMode());

        Assert.assertNotNull(fragment.podcastFuture);
    }

    private void assertRecyclerErrorView() {
        Assert.assertEquals(View.VISIBLE, fragment.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertTrue(fragment.getRefreshView().isEnabled());

        Assert.assertEquals(RecordsAdapter.FooterMode.ERROR, fragment.adapter.getFooterMode());

        Assert.assertNull(fragment.podcastFuture);
    }

    private void assertRecyclerView() {
        Assert.assertEquals(View.VISIBLE, fragment.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertTrue(fragment.getRefreshView().isEnabled());

        Assert.assertEquals(RecordsAdapter.FooterMode.HIDDEN, fragment.adapter.getFooterMode());

        Assert.assertNull(fragment.podcastFuture);
    }

    private void runOnUiThreadSync(final Runnable action) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } finally {
                    semaphore.release();
                }
            }
        });
        semaphore.acquire();
    }
}
