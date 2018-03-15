package ru.radiomayak.podcasts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class PodcastsUtilsTest {
    @Test
    public void shouldStorePodcastSeen() {
        Context context = InstrumentationRegistry.getTargetContext();

        Podcast podcast = new Podcast(10, "foobar");
        podcast.setLength(1000);
        podcast.setSeen(1);
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context);
        try (PodcastsWritableDatabase database = PodcastsWritableDatabase.get(helper)) {
            database.storePodcasts(new Podcasts(Collections.singletonList(podcast)));
            database.storePodcastSeen(podcast.getId(), 100);
        }

        Podcasts stored;
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            stored = database.loadPodcasts();
        }
        Podcast actual = stored.get(podcast.getId());
        Assert.assertNotNull(actual);
        Assert.assertNotSame(podcast, actual);
        Assert.assertEquals(100, actual.getSeen());
    }

    @After
    public void after() {
        Context context = InstrumentationRegistry.getTargetContext();
        String path = context.getDatabasePath(PodcastsReadableDatabase.DATABASE_NAME).getPath();
        SQLiteDatabase.deleteDatabase(new File(path));
    }
}
