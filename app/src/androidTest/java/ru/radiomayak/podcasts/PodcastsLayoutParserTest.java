package ru.radiomayak.podcasts;

import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class PodcastsLayoutParserTest {
    private static final String RESPONSE_URL = "http://radiomayak.ru/podcasts/";

    private PodcastsLayoutParser parser;

    @Before
    public void before() {
        parser = new PodcastsLayoutParser();
    }

    @Test
    public void shouldCorrectlyHandleUnsupportedResponse() throws IOException {
        Podcasts podcasts = parser.parse(IOUtils.toInputStream("<html></html>", "UTF-8"), "UTF-8", RESPONSE_URL);
        Assert.assertTrue(podcasts.list().isEmpty());
    }

    @Test
    public void shouldGetPodcasts() throws IOException, JSONException {
        testResource("podcasts/podcasts-1");
    }

    @Test
    public void shouldSkipBrokenPodcasts() throws IOException, JSONException {
        testResource("podcasts/podcasts-2");
    }

    @Test
    public void shouldHandleHtmlEntities() throws IOException, JSONException {
        testResource("podcasts/podcasts-3");
    }

    private void testResource(String resourceName) throws IOException, JSONException {
        Podcasts podcasts;
        try (InputStream stream = getResource(resourceName + ".html")) {
            podcasts = parser.parse(stream, "UTF-8", RESPONSE_URL);
        }
        try (InputStream resource = getResource(resourceName + ".json")) {
            String json = IOUtils.toString(resource, "UTF-8");
            assertEquals(new JSONArray(json), JsonUtils.toJson(podcasts));
        }
    }

    private static void assertEquals(JSONArray expected, JSONArray actual) throws JSONException {
        Assert.assertEquals(expected.length(), actual.length());
        for (int i = 0; i < expected.length(); i++) {
            Assert.assertEquals(expected.get(i).toString(), actual.get(i).toString());
        }
    }

    private static InputStream getResource(String name) {
        return PodcastsLayoutParserTest.class.getClassLoader().getResourceAsStream(name);
    }
}
