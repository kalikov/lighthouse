package ru.radiomayak.podcasts;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class PodcastRssParserTest {
    private PodcastRssParser parser;

    @Before
    public void before() {
        parser = new PodcastRssParser();
    }

    @Test
    public void shouldParseRss() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("podcasts/rss.xml")) {
            Records records = parser.parse(resource);
            Assert.assertEquals(records.list().size(), 10);
        }
    }
}
