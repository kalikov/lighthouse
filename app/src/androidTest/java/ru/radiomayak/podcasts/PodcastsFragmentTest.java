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
public class PodcastsFragmentTest {
    @Rule
    public final ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

    private MainActivity activity;
    private PodcastsFragment fragment;

    @Before
    public void before() {
        activity = rule.getActivity();
        fragment = activity.podcastsFragment;
    }

    @Test
    public void shouldBeInLoadingViewOnCreate() {
        assertLoadingView();
        Mockito.verify(fragment.podcastsLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastsLoopback.class), Mockito.eq(fragment.podcastsLoopbackListener));
    }

    @Test
    public void shouldBeInLoadingViewOnEmptyLoopback() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Mockito.reset(fragment.podcastsLoaderManager);
                fragment.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
            }
        });

        assertLoadingView();
        Mockito.verify(fragment.podcastsLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastsLoader.class), Mockito.eq(fragment.podcastsListener));
    }

    @Test
    public void shouldBeInErrorViewOnLoopbackFailure() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Mockito.reset(fragment.podcastsLoaderManager);
                fragment.podcastsLoopbackListener.onException(Mockito.mock(Loader.class), new RuntimeException());
            }
        });

        assertErrorView();
        Mockito.verifyZeroInteractions(fragment.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInErrorViewOnRequestFailure() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                fragment.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
                Mockito.reset(fragment.podcastsLoaderManager);
                fragment.podcastsListener.onException(Mockito.mock(Loader.class), new RuntimeException());
            }
        });

        assertErrorView();
        Mockito.verifyZeroInteractions(fragment.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInErrorViewOnEmptyRequest() throws InterruptedException {
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                fragment.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
                Mockito.reset(fragment.podcastsLoaderManager);
                fragment.podcastsListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
            }
        });

        assertErrorView();
        Mockito.verifyZeroInteractions(fragment.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInListViewOnRequestData() throws InterruptedException {
        final Podcasts podcasts = new Podcasts();
        podcasts.add(new Podcast(1, "foobar"));
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                fragment.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), new Podcasts());
                Mockito.reset(fragment.podcastsLoaderManager);
                fragment.podcastsListener.onComplete(Mockito.mock(Loader.class), podcasts);
            }
        });

        assertListView(false);
        Assert.assertEquals(1, fragment.adapter.getItemCount());
        Mockito.verifyZeroInteractions(fragment.podcastsLoaderManager);
    }

    @Test
    public void shouldBeInBackgroundRequestOnLoopbackData() throws InterruptedException {
        final Podcasts podcasts = new Podcasts();
        podcasts.add(new Podcast(1, "foobar"));
        runOnUiThreadSync(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Mockito.reset(fragment.podcastsLoaderManager);
                fragment.podcastsLoopbackListener.onComplete(Mockito.mock(Loader.class), podcasts);
            }
        });

        assertListView(true);
        Assert.assertEquals(1, fragment.adapter.getItemCount());
        Mockito.verify(fragment.podcastsLoaderManager).execute(Mockito.any(Context.class), Mockito.isA(PodcastsLoader.class), Mockito.eq(fragment.podcastsListener));
    }

    private void assertTitleEquals(int resourceId) {
        Assert.assertEquals(activity.getString(resourceId), fragment.getToolbar().getTitle());
    }

    private void assertLoadingView() {
        Assert.assertEquals(View.VISIBLE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertFalse(fragment.getRefreshView().isEnabled());

        Assert.assertNotNull(fragment.podcastsFuture);
        assertTitleEquals(R.string.podcasts);
    }

    private void assertErrorView() {
        Assert.assertEquals(View.GONE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertTrue(fragment.getRefreshView().isEnabled());

        Assert.assertNull(fragment.podcastsFuture);
        assertTitleEquals(R.string.podcasts);
    }

    private void assertListView(boolean isBackgroundLoading) {
        Assert.assertEquals(View.GONE, fragment.getLoadingView().getVisibility());
        Assert.assertEquals(View.GONE, fragment.getErrorView().getVisibility());
        Assert.assertEquals(View.VISIBLE, fragment.getRecyclerView().getVisibility());
        Assert.assertFalse(fragment.getRefreshView().isRefreshing());
        Assert.assertTrue(fragment.getRefreshView().isEnabled());
        if (isBackgroundLoading) {
            Assert.assertNotNull(fragment.podcastsFuture);
            assertTitleEquals(R.string.refreshing);
        } else {
            Assert.assertNull(fragment.podcastsFuture);
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
