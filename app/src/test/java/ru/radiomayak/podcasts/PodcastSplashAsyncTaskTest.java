package ru.radiomayak.podcasts;

import android.content.Context;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

public class PodcastSplashAsyncTaskTest {
    private PodcastSplashAsyncTask task;

    @Before
    public void before() {
        task = new PodcastSplashAsyncTask(Mockito.mock(Context.class), Mockito.mock(PodcastSplashAsyncTask.Listener.class));
    }

    @Test
    public void shouldGetImageData() throws IOException {
        String url = "http://cdn-st4.rtr-vesti.ru/vh/pictures/bq/128/712/3.jpg";
//        ImageDataResponse response = task.doInBackground(url);
//        Assert.assertNotNull(response);
//        Assert.assertEquals(url, response.getUrl());
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("podcasts/image-1.jpg")) {
            byte[] bytes = IOUtils.toByteArray(resource);
//            Assert.assertArrayEquals(bytes, Base64.decodeBase64(response.getData()));
        }
    }
}
