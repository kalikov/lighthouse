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
        PodcastsUtils.storePodcasts(context, new Podcasts(Collections.singletonList(podcast)));

        PodcastsUtils.storePodcastSeen(context, 10, 100);

        Podcasts stored = PodcastsUtils.loadPodcasts(context);
        Podcast actual = stored.get(10);
        Assert.assertNotNull(actual);
        Assert.assertNotSame(podcast, actual);
        Assert.assertEquals(100, actual.getSeen());
    }

    @After
    public void after() {
        Context context = InstrumentationRegistry.getTargetContext();
        String path = context.getDatabasePath(PodcastsUtils.PODCASTS_DATABASE_NAME).getPath();
        SQLiteDatabase.deleteDatabase(new File(path));
    }
}
