package ru.radiomayak.podcasts;

import android.content.Context;

import org.apache.commons.io.IOUtils;
import org.jsoup.Connection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class PodcastsLoaderTest {
    private static final String RESPONSE_URL = "http://radiomayak.ru/podcasts/";

    private PodcastsLoader task;

    @Before
    public void before() {
        task = Mockito.spy(new PodcastsLoader(Mockito.mock(Context.class), true));
    }

    private static Connection.Response mockResponse(byte[] bytes, String charset, String url) throws IOException {
        Connection.Response response = Mockito.mock(Connection.Response.class);
        Mockito.doReturn(charset).when(response).charset();
        Mockito.doReturn(new URL(url)).when(response).url();
        Mockito.doReturn(bytes).when(response).bodyAsBytes();
        return response;
    }

    private static void mockResponse(PodcastsLoader task, byte[] bytes, String charset) throws IOException {
        Connection.Response response = mockResponse(bytes, charset, RESPONSE_URL);
//        Mockito.doReturn(response).when(task).request(RESPONSE_URL);
    }

    @Test
    public void shouldCorrectlyHandleUnsupportedResponse() throws IOException {
        mockResponse(task, "<html></html>".getBytes(), "utf-8");

        Podcasts podcasts = task.onExecute();
        Assert.assertTrue(podcasts.list().isEmpty());
    }

    @Test
    public void shouldGetPodcasts() throws IOException {
        testService("podcasts/podcasts-1");
    }

    @Test
    public void shouldSkipBrokenPodcasts() throws IOException {
        testService("podcasts/podcasts-2");
    }

    private void testService(String resourceName) throws IOException {
        Podcasts podcasts;
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourceName + ".html")) {
//            mockResponse(task, IOUtils.toByteArray(resource), "utf-8");
            podcasts = task.onExecute();
        }
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourceName + ".json")) {
            Assert.assertEquals(IOUtils.toString(resource, "utf-8"), podcasts.toJson().toString());
        }
    }
}
