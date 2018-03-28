package ru.radiomayak.podcasts;

import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class PodcastLayoutParserTest {
    private static final String RESPONSE_URL = "http://example.com";

    private PodcastLayoutParser parser;

    @Before
    public void before() {
        parser = new PodcastLayoutParser();
    }

    @Test
    public void shouldGetRecords() throws IOException, JSONException {
        Records records;
        try (InputStream stream = getResource("podcasts/podcast-1.html")) {
            PodcastLayoutContent content = parser.parse(0, stream, "UTF-8", RESPONSE_URL);
            records = content.getRecords();
        }
        try (InputStream resource = getResource("podcasts/podcast-1.records.json")) {
            String json = IOUtils.toString(resource, "UTF-8");
            assertEquals(new JSONArray(json), JsonUtils.toJson(records));
        }
    }

    @Test
    public void shouldGetRecordsWithIncorrectHtmlEntities() throws IOException, JSONException {
        Records records;
        try (InputStream stream = getResource("podcasts/podcast-2.html")) {
            PodcastLayoutContent content = parser.parse(0, stream, "UTF-8", RESPONSE_URL);
            records = content.getRecords();
        }
        try (InputStream resource = getResource("podcasts/podcast-2.records.json")) {
            String json = IOUtils.toString(resource, "UTF-8");
            assertEquals(new JSONArray(json), JsonUtils.toJson(records));
        }
    }

    @Test
    public void shouldGetPodcast() throws IOException, JSONException {
        Podcast podcast;
        try (InputStream stream = getResource("podcasts/podcast-1.html")) {
            PodcastLayoutContent content = parser.parse(0, stream, "UTF-8", RESPONSE_URL);
            podcast = content.getPodcast();
        }
        Assert.assertNotNull(podcast);
        try (InputStream resource = getResource("podcasts/podcast-1.podcast.json")) {
            String json = IOUtils.toString(resource, "UTF-8");
            assertEquals(new JSONObject(json), JsonUtils.toJson(podcast));
        }
    }

    private static void assertEquals(JSONArray expected, JSONArray actual) throws JSONException {
        Assert.assertEquals(expected.length(), actual.length());
        for (int i = 0; i < expected.length(); i++) {
            Assert.assertEquals(expected.get(i).toString(), actual.get(i).toString());
        }
    }

    private static void assertEquals(JSONObject expected, JSONObject actual) throws JSONException {
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    private static InputStream getResource(String name) {
        return PodcastsLayoutParserTest.class.getClassLoader().getResourceAsStream(name);
    }
}
