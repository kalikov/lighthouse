package ru.radiomayak.podcasts;

import android.content.Context;
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

import ru.radiomayak.R;
import ru.radiomayak.content.Loader;

@RunWith(AndroidJUnit4.class)
public class PodcastsActivityTest {
    @Rule
    public final ActivityTestRule<PodcastsActivity> rule = new ActivityTestRule<>(PodcastsActivity.class);

    private PodcastsActivity activity;

    @Before
    public void before() {
        activity = rule.getActivity();
    }

    @Test
    public void shouldBeInLoadingViewOnCreate() {
        assertLoadingView();
        Mockito.verify(activity.podcastsLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastsLoopback.class), Mockito.eq(activity.podcastsLoopbackListener));
    }

    @Test
    public void shouldBeInLoadingViewOnEmptyLoopback() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Mockito.reset(activity.podcastsLoaderManager);
                activity.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
            }
        });

        assertLoadingView();
        Mockito.verify(activity.podcastsLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastsLoader.class), Mockito.eq(activity.podcastsListener));
    }

    @Test
    public void shouldBeInErrorViewOnLoopbackFailure() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Mockito.reset(activity.podcastsLoaderManager);
                activity.podcastsLoopbackListener.onException(Mockito.mock(Loader.class), new RuntimeException());
            }
        });

        assertErrorView();
        Mockito.verifyZeroInteractions(activity.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInErrorViewOnRequestFailure() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                activity.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
                Mockito.reset(activity.podcastsLoaderManager);
                activity.podcastsListener.onException(Mockito.mock(Loader.class), new RuntimeException());
            }
        });

        assertErrorView();
        Mockito.verifyZeroInteractions(activity.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInErrorViewOnEmptyRequest() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                activity.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
                Mockito.reset(activity.podcastsLoaderManager);
                activity.podcastsListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
            }
        });

        assertErrorView();
        Mockito.verifyZeroInteractions(activity.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInListViewOnRequestData() throws InterruptedException {
        final Podcasts podcasts = new Podcasts();
        podcasts.add(new Podcast(1, "foobar"));
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                activity.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
                Mockito.reset(activity.podcastsLoaderManager);
                activity.podcastsListener.onComplete(Mockito.mock(Loader.class), podcasts);
            }
        });

        assertListView(false);
        Assert.assertEquals(1, activity.adapter.getCount());
        Mockito.verifyZeroInteractions(activity.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInBackgroundRequestOnLoopbackData() throws InterruptedException {
        final Podcasts podcasts = new Podcasts();
        podcasts.add(new Podcast(1, "foobar"));
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Mockito.reset(activity.podcastsLoaderManager);
                activity.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), podcasts);
            }
        });

        assertListView(true);
        Assert.assertEquals(1, activity.adapter.getCount());
        Mockito.verify(activity.podcastsLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastsLoader.class), Mockito.eq(activity.podcastsListener));
    }

    private void assertTitleEquals(int resourceId) {
        Assert.assertEquals(activity.getString(resourceId), activity.getToolbar().getTitle());
    }

    private void assertLoadingView() {
        Assert.assertEquals(View.VISIBLE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getListView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertFalse(activity.getRefreshView().isEnabled());

        Assert.assertNotNull(activity.podcastsFuture);
        assertTitleEquals(R.string.podcasts);
    }

    private void assertErrorView() {
        Assert.assertEquals(View.GONE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getListView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertTrue(activity.getRefreshView().isEnabled());

        Assert.assertNull(activity.podcastsFuture);
        assertTitleEquals(R.string.podcasts);
    }

    private void assertListView(boolean isBackgroundLoading) {
        Assert.assertEquals(View.GONE, activity.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, activity.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, activity.getListView().getVisibility());
        Assert.assertFalse(activity.getRefreshView().isRefreshing());
        Assert.assertTrue(activity.getRefreshView().isEnabled());
        if (isBackgroundLoading) {
            Assert.assertNotNull(activity.podcastsFuture);
            assertTitleEquals(R.string.refreshing);
        } else {
            Assert.assertNull(activity.podcastsFuture);
            assertTitleEquals(R.string.podcasts);
        }
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
