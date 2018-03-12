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
public class RecordsActivityTest {
    @Rule
    public final ActivityTestRule<RecordsActivity> rule = new ActivityTestRule<>(RecordsActivity.class, false, false);

    private Podcast podcast;
    private RecordsActivity activity;

    @Before
    public void before() {
        Context context = InstrumentationRegistry.getTargetContext();

        podcast = new Podcast(1, "test");
        podcast.setIcon(new Image(PictureUrlUtils.getPictureUrl("x/vh/pictures/ad/12/33/55.jpg", PictureUrlUtils.Size.L)));

        Intent intent = new Intent(context, RecordsActivity.class);
        intent.putExtra(RecordsActivity.EXTRA_PODCAST, podcast);
        activity = rule.launchActivity(intent);
    }

    @Test
    public void shouldBeInLoadingViewOnCreate() {
        assertLoadingView();
        Mockito.verify(activity.podcastLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastLoader.class), Mockito.eq(activity.podcastListener));

        LoaderManager<BitmapInfo> imageManager = activity.getLighthouseApplication().getImageLoaderManager();
        Mockito.verify(imageManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastSplashLoader.class), Mockito.eq(activity.splashListener));

        Assert.assertNotNull(activity.splashFuture);
    }

    @Test
    public void shouldBeInContentViewOnSplashCompleteAfterCreate() throws InterruptedException {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                activity.splashListener.onComplete(Mockito.mock(Loader.class), new BitmapInfo(bitmap, 0, 0));
            }
        });
        assertRecyclerLoadingView();

        Assert.assertNull(activity.splashFuture);
        Assert.assertSame(bitmap, activity.splash);
    }

    @Test
    public void shouldBeInLoadingViewOnSplashFailAfterCreate() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                activity.splashListener.onException(Mockito.mock(Loader.class), new Exception());
            }
        });
        assertLoadingView();

        Assert.assertNull(activity.splashFuture);
        Assert.assertNull(activity.splash);
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
                activity.podcastListener.onComplete(Mockito.mock(Loader.class), new PodcastResponse(remotePodcast, paginator));
            }
        });
        assertRecyclerView();

        Assert.assertNotNull(activity.splashFuture);
        Assert.assertNull(activity.splash);
    }

    @Test
    public void shouldBeInErrorViewOnPodcastFailAfterCreate() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                activity.podcastListener.onException(Mockito.mock(Loader.class), new Exception());
            }
        });
        assertErrorView();

        Assert.assertNotNull(activity.splashFuture);
        Assert.assertNull(activity.splash);
    }

    @Test
    public void shouldBeInContentViewOnSplashCompleteAfterPodcastFail() throws InterruptedException {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        runOnUiThreadSync(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                activity.podcastListener.onException(Mockito.mock(Loader.class), new Exception());
                activity.splashListener.onComplete(Mockito.mock(Loader.class), new BitmapInfo(bitmap, 0, 0));
            }
        });
        assertRecyclerErrorView();

        Assert.assertNull(activity.splashFuture);
        Assert.assertSame(bitmap, activity.splash);
    }

    private void assertLoadingView() {
        Assert.assertEquals(View.GONE, activity.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getRecyclerView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertFalse(activity.getRefreshView().isEnabled());

        Assert.assertNotNull(activity.podcastFuture);
    }

    private void assertErrorView() {
        Assert.assertEquals(View.GONE, activity.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getRecyclerView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertTrue(activity.getRefreshView().isEnabled());

        Assert.assertNull(activity.podcastFuture);
    }

    private void assertRecyclerLoadingView() {
        Assert.assertEquals(View.VISIBLE, activity.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getRecyclerView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertFalse(activity.getRefreshView().isEnabled());

        Assert.assertEquals(RecordsAdapter.FooterMode.LOADING, activity.adapter.getFooterMode());

        Assert.assertNotNull(activity.podcastFuture);
    }

    private void assertRecyclerErrorView() {
        Assert.assertEquals(View.VISIBLE, activity.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getRecyclerView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertTrue(activity.getRefreshView().isEnabled());

        Assert.assertEquals(RecordsAdapter.FooterMode.BUTTON, activity.adapter.getFooterMode());

        Assert.assertNull(activity.podcastFuture);
    }

    private void assertRecyclerView() {
        Assert.assertEquals(View.VISIBLE, activity.getAppBarLayout().getVisibility());
        Assert.assertEquals(View.GONE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getRecyclerView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertTrue(activity.getRefreshView().isEnabled());

        Assert.assertEquals(RecordsAdapter.FooterMode.HIDDEN, activity.adapter.getFooterMode());

        Assert.assertNull(activity.podcastFuture);
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
