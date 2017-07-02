package ru.radiomayak.podcasts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.v4.widget.SwipeRefreshLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PodcastsActivityTest {
    private PodcastsActivity activity;

    private ConnectivityManager connectivityManager;

    private SwipeRefreshLayout refreshLayout;

    @Before
    public void before() {
        activity = Mockito.spy(new PodcastsActivity());

        Mockito.doNothing().when(activity).toast(Mockito.anyInt(), Mockito.anyInt());

        refreshLayout = Mockito.mock(SwipeRefreshLayout.class);
        Mockito.doReturn(refreshLayout).when(activity).getRefreshView();

        connectivityManager = Mockito.mock(ConnectivityManager.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn(connectivityManager).when(activity).getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Test
    public void shouldResetRefreshingFlagWhenNoConnection() throws Exception {
        Mockito.when(connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting()).thenReturn(Boolean.FALSE);

        activity.adapter = Mockito.mock(PodcastsAdapter.class);
        Mockito.when(activity.adapter.isEmpty()).thenReturn(Boolean.FALSE);

        activity.requestList();

        Mockito.verify(refreshLayout).setRefreshing(false);
    }
}
