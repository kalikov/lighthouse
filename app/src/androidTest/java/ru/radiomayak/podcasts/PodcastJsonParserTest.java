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
import java.io.InputStreamReader;

import ru.radiomayak.NetworkUtils;

@RunWith(AndroidJUnit4.class)
public class PodcastJsonParserTest {
    private static final String RESPONSE_URL = "http://example.com";

    private PodcastJsonParser parser;

    @Before
    public void before() {
        parser = new PodcastJsonParser();
    }

    @Test
    public void shouldGetRecords() throws IOException, JSONException {
        Records records;
        try (InputStream stream = getResource("podcasts/page-1.json")) {
            PodcastLayoutContent content = parser.parse(new InputStreamReader(stream), NetworkUtils.toOptURI(RESPONSE_URL));
            records = content.getRecords();
        }
        try (InputStream resource = getResource("podcasts/page-1.records.json")) {
            String json = IOUtils.toString(resource, "UTF-8");
            Assert.assertEquals(new JSONArray(json), records.toJson());
        }
    }

    private static InputStream getResource(String name) {
        return PodcastsLayoutParserTest.class.getClassLoader().getResourceAsStream(name);
    }
}
