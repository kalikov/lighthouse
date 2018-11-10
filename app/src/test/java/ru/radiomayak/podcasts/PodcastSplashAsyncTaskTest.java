package ru.radiomayak.podcasts;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class PodcastSplashAsyncTaskTest {

    @Before
    public void before() {
        PodcastSplashLoader task = new PodcastSplashLoader(null);
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
