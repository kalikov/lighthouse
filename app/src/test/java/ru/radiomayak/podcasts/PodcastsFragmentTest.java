package ru.radiomayak.podcasts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PodcastsFragmentTest {
    private PodcastsFragment fragment;

    private ConnectivityManager connectivityManager;

    private SwipeRefreshLayout refreshLayout;

    @Before
    public void before() {
        fragment = Mockito.spy(new PodcastsFragment());

        Mockito.doNothing().when(fragment).toast(Mockito.anyInt(), Mockito.anyInt());

        refreshLayout = Mockito.mock(SwipeRefreshLayout.class);
        Mockito.doReturn(refreshLayout).when(fragment).getRefreshView();

        Context context = Mockito.mock(Context.class);
        Mockito.doReturn(context).when(fragment).getContext();
        Mockito.doReturn(Mockito.mock(View.class)).when(fragment).getView();

        connectivityManager = Mockito.mock(ConnectivityManager.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn(connectivityManager).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Test
    public void shouldResetRefreshingFlagWhenNoConnection() {
        Mockito.when(connectivityManager.getActiveNetworkInfo().isConnected()).thenReturn(Boolean.FALSE);

        fragment.adapter = Mockito.mock(PodcastsAdapter.class);
        Mockito.when(fragment.adapter.isEmpty()).thenReturn(Boolean.FALSE);

        fragment.requestRemoteList();

        Mockito.verify(refreshLayout).setRefreshing(false);
    }
}
