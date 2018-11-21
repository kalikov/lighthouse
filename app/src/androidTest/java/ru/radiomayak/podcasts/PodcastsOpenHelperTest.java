package ru.radiomayak.podcasts;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class PodcastsOpenHelperTest {
    private static final String TEST_DATABASE_NAME = "test_podcasts";

    @Before
    public void before() {
        deleteDatabase();
    }

    @After
    public void after() {
        deleteDatabase();
    }

    private void deleteDatabase() {
        Context context = InstrumentationRegistry.getTargetContext();
        String path = context.getDatabasePath(TEST_DATABASE_NAME).getPath();
        File file = new File(path);
        if (file.exists()) {
            SQLiteDatabase.deleteDatabase(new File(path));
        }
    }

    @Test
    public void shouldCreateDatabase() {
        Context context = InstrumentationRegistry.getTargetContext();
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, TEST_DATABASE_NAME);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            database.loadPodcasts();
        }
    }

    @Test
    public void shouldUpgradeFromVersion1() {
        Podcast podcastVersion1 = new Podcast(1, "name");
        podcastVersion1.setDescription("description");
        podcastVersion1.setLength(100);
        podcastVersion1.setIcon(new Image("icon_url", 1, 2));
        podcastVersion1.setSplash(new Image("splash_url", 3, 4));
        podcastVersion1.setSeen(100);

        Context context = InstrumentationRegistry.getTargetContext();
        PodcastsOpenHelperVersion1 helperVersion1 = new PodcastsOpenHelperVersion1(context);
        try (SQLiteDatabase database = helperVersion1.getWritableDatabase()) {
            ContentValues podcast = new ContentValues();
            podcast.put("id", 1);
            podcast.put("name", "name");
            podcast.put("description", "description");
            podcast.put("length", 100);
            podcast.put("icon_url", "icon_url");
            podcast.put("icon_rgb", 1);
            podcast.put("icon_rgb2", 2);
            podcast.put("splash_url", "splash_url");
            podcast.put("splash_rgb", 3);
            podcast.put("splash_rgb2", 4);
            podcast.put("ord", 1);
            database.insert("podcasts", null, podcast);

            ContentValues record = new ContentValues();
            record.put("podcast_id", 1);
            record.put("id", 1);
            record.put("name", "name");
            record.put("url", "url");
            record.put("description", "description");
            record.put("date", "date");
            record.put("duration", "duration");
            record.put("played", 1);
            database.insert("records", null, record);
        }

        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, TEST_DATABASE_NAME);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            Podcasts podcasts = database.loadPodcasts();
            Assert.assertEquals(1, podcasts.list().size());
            Assert.assertTrue(PodcastsTestUtils.equals(podcasts.get(1), podcastVersion1));
        }
    }

    @Test
    public void shouldUpgradeFromVersion2_3() {
        Podcast podcastVersion1 = new Podcast(1, "name");
        podcastVersion1.setDescription("description");
        podcastVersion1.setLength(100);
        podcastVersion1.setIcon(new Image("icon_url", 1, 2));
        podcastVersion1.setSplash(new Image("splash_url", 3, 4));
        podcastVersion1.setSeen(50);

        Podcast podcastVersion2 = new Podcast(2, "name");
        podcastVersion2.setDescription("description");
        podcastVersion2.setLength(10);
        podcastVersion2.setSeen(0);

        Context context = InstrumentationRegistry.getTargetContext();
        PodcastsOpenHelperVersion2_3 helperVersion2 = new PodcastsOpenHelperVersion2_3(context);
        try (SQLiteDatabase database = helperVersion2.getWritableDatabase()) {
            ContentValues podcast1 = new ContentValues();
            podcast1.put("id", 1);
            podcast1.put("name", "name");
            podcast1.put("description", "description");
            podcast1.put("length", 100);
            podcast1.put("seen", 50);
            podcast1.put("icon_url", "icon_url");
            podcast1.put("icon_rgb", 1);
            podcast1.put("icon_rgb2", 2);
            podcast1.put("splash_url", "splash_url");
            podcast1.put("splash_rgb", 3);
            podcast1.put("splash_rgb2", 4);
            podcast1.put("ord", 1);
            database.insert("podcasts", null, podcast1);

            ContentValues podcast2 = new ContentValues();
            podcast2.put("id", 2);
            podcast2.put("name", "name");
            podcast2.put("description", "description");
            podcast2.put("length", 10);
            podcast2.put("seen", 0);
            podcast2.put("ord", 2);
            podcast2.put("icon_rgb", 0);
            podcast2.put("icon_rgb2", 0);
            podcast2.put("splash_rgb", 0);
            podcast2.put("splash_rgb2", 0);
            database.insert("podcasts", null, podcast2);

            ContentValues record = new ContentValues();
            record.put("podcast_id", 1);
            record.put("id", 1);
            record.put("name", "name");
            record.put("url", "url");
            record.put("description", "description");
            record.put("date", "date");
            record.put("duration", "duration");
            record.put("played", 1);
            database.insert("records", null, record);
        }

        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, TEST_DATABASE_NAME);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            Podcasts podcasts = database.loadPodcasts();
            Assert.assertEquals(2, podcasts.list().size());
            Assert.assertTrue(PodcastsTestUtils.equals(podcasts.get(1), podcastVersion1));
            Assert.assertTrue(PodcastsTestUtils.equals(podcasts.get(2), podcastVersion2));
        }
    }

    @Test
    public void shouldUpgradeFromVersion4() {
        Podcast podcastVersion1 = new Podcast(1, "name");
        podcastVersion1.setDescription("description");
        podcastVersion1.setLength(100);
        podcastVersion1.setIcon(new Image("icon_url", 1, 2));
        podcastVersion1.setSplash(new Image("splash_url", 3, 4));
        podcastVersion1.setSeen(50);

        Context context = InstrumentationRegistry.getTargetContext();
        PodcastsOpenHelperVersion4 helperVersion4 = new PodcastsOpenHelperVersion4(context);
        try (SQLiteDatabase database = helperVersion4.getWritableDatabase()) {
            ContentValues podcast1 = new ContentValues();
            podcast1.put("id", 1);
            podcast1.put("name", "name");
            podcast1.put("description", "description");
            podcast1.put("length", 100);
            podcast1.put("seen", 50);
            podcast1.put("icon_url", "icon_url");
            podcast1.put("icon_rgb", 1);
            podcast1.put("icon_rgb2", 2);
            podcast1.put("splash_url", "splash_url");
            podcast1.put("splash_rgb", 3);
            podcast1.put("splash_rgb2", 4);
            podcast1.put("ord", 1);
            database.insert("podcasts", null, podcast1);

            ContentValues record = new ContentValues();
            record.put("podcast_id", 1);
            record.put("id", 1);
            record.put("name", "name");
            record.put("url", "url");
            record.put("description", "description");
            record.put("date", "date");
            record.put("duration", "duration");
            record.put("played", 1);
            database.insert("records", null, record);
        }

        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, TEST_DATABASE_NAME);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            Podcasts podcasts = database.loadPodcasts();
            Assert.assertEquals(1, podcasts.list().size());
            Assert.assertTrue(PodcastsTestUtils.equals(podcasts.get(1), podcastVersion1));
        }
    }

    @Test
    public void shouldUpgradeFromVersion5() {
        Podcast podcastVersion1 = new Podcast(1, "name");
        podcastVersion1.setDescription("description");
        podcastVersion1.setLength(100);
        podcastVersion1.setIcon(new Image("icon_url", 1, 2));
        podcastVersion1.setSplash(new Image("splash_url", 3, 4));
        podcastVersion1.setSeen(50);

        Context context = InstrumentationRegistry.getTargetContext();
        PodcastsOpenHelperVersion5 helperVersion5 = new PodcastsOpenHelperVersion5(context);
        try (SQLiteDatabase database = helperVersion5.getWritableDatabase()) {
            ContentValues podcast1 = new ContentValues();
            podcast1.put("id", 1);
            podcast1.put("name", "name");
            podcast1.put("description", "description");
            podcast1.put("length", 100);
            podcast1.put("seen", 50);
            podcast1.put("icon_url", "icon_url");
            podcast1.put("icon_rgb", 1);
            podcast1.put("icon_rgb2", 2);
            podcast1.put("splash_url", "splash_url");
            podcast1.put("splash_rgb", 3);
            podcast1.put("splash_rgb2", 4);
            podcast1.put("ord", 1);
            database.insert("podcasts", null, podcast1);

            ContentValues record = new ContentValues();
            record.put("podcast_id", 1);
            record.put("id", 1);
            record.put("name", "name");
            record.put("url", "url");
            record.put("description", "description");
            record.put("date", "date");
            record.put("duration", "duration");
            record.put("played", 1);
            database.insert("records", null, record);

            ContentValues player = new ContentValues();
            player.put("podcast_id", 1);
            player.put("record_id", 1);
            player.put("position", 100);
            database.insert("players", null, player);
        }

        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, TEST_DATABASE_NAME);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            Podcasts podcasts = database.loadPodcasts();
            Assert.assertEquals(1, podcasts.list().size());
            Assert.assertTrue(PodcastsTestUtils.equals(podcasts.get(1), podcastVersion1));

            Record record = new Record(1, "name", "url");

            Records records = new Records();
            records.add(record);
            database.loadRecordsPositionAndLength(podcastVersion1.getId(), records);
            Assert.assertEquals(100, record.getPosition());
            Assert.assertEquals(0, record.getLength());
        }
    }

    @Test
    public void shouldUpgradeFromVersion6() {
        Podcast podcastVersion1 = new Podcast(1, "name");
        podcastVersion1.setDescription("description");
        podcastVersion1.setLength(100);
        podcastVersion1.setIcon(new Image("icon_url", 1, 2));
        podcastVersion1.setSplash(new Image("splash_url", 3, 4));
        podcastVersion1.setSeen(50);

        Context context = InstrumentationRegistry.getTargetContext();
        PodcastsOpenHelperVersion6 helperVersion6 = new PodcastsOpenHelperVersion6(context);
        try (SQLiteDatabase database = helperVersion6.getWritableDatabase()) {
            ContentValues podcast1 = new ContentValues();
            podcast1.put("id", 1);
            podcast1.put("name", "name");
            podcast1.put("description", "description");
            podcast1.put("length", 100);
            podcast1.put("seen", 50);
            podcast1.put("icon_url", "icon_url");
            podcast1.put("icon_rgb", 1);
            podcast1.put("icon_rgb2", 2);
            podcast1.put("splash_url", "splash_url");
            podcast1.put("splash_rgb", 3);
            podcast1.put("splash_rgb2", 4);
            podcast1.put("ord", 1);
            database.insert("podcasts", null, podcast1);

            ContentValues record = new ContentValues();
            record.put("podcast_id", 1);
            record.put("id", 1);
            record.put("name", "name");
            record.put("url", "url");
            record.put("description", "description");
            record.put("date", "date");
            record.put("duration", "duration");
            record.put("played", 1);
            database.insert("records", null, record);

            ContentValues player = new ContentValues();
            player.put("podcast_id", 1);
            player.put("record_id", 1);
            player.put("position", 100);
            player.put("length", 1000);
            database.insert("players", null, player);
        }

        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, TEST_DATABASE_NAME);
        try (PodcastsReadableDatabase database = PodcastsReadableDatabase.get(helper)) {
            Podcasts podcasts = database.loadPodcasts();
            Assert.assertEquals(1, podcasts.list().size());
            Assert.assertTrue(PodcastsTestUtils.equals(podcasts.get(1), podcastVersion1));

            Record record = new Record(1, "name", "url");

            Records records = new Records();
            records.add(record);
            database.loadRecordsPositionAndLength(podcastVersion1.getId(), records);
            Assert.assertEquals(100, record.getPosition());
            Assert.assertEquals(1000, record.getLength());
        }
    }

    public class PodcastsOpenHelperVersion1 extends SQLiteOpenHelper {
        PodcastsOpenHelperVersion1(Context context) {
            super(context, TEST_DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE podcasts (" +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "length INTEGER NOT NULL," +
                    "icon_url TEXT," +
                    "icon_rgb INTEGER NOT NULL," +
                    "icon_rgb2 INTEGER NOT NULL," +
                    "splash_url TEXT," +
                    "splash_rgb INTEGER NOT NULL," +
                    "splash_rgb2 INTEGER NOT NULL," +
                    "ord INTEGER NOT NULL," +
                    "PRIMARY KEY (id))");

            db.execSQL("CREATE INDEX idx_podcasts__ord ON podcasts (ord DESC)");

            db.execSQL("CREATE TABLE records (" +
                    "podcast_id INTEGER NOT NULL," +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "url TEXT NOT NULL," +
                    "description TEXT," +
                    "date TEXT," +
                    "duration TEXT," +
                    "played INTEGER NOT NULL," +
                    "PRIMARY KEY (podcast_id, id))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException();
        }
    }

    public class PodcastsOpenHelperVersion2_3 extends SQLiteOpenHelper {
        PodcastsOpenHelperVersion2_3(Context context) {
            super(context, TEST_DATABASE_NAME, null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE podcasts (" +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "length INTEGER NOT NULL," +
                    "seen INTEGER NOT NULL DEFAULT 0," +
                    "icon_url TEXT," +
                    "icon_rgb INTEGER NOT NULL," +
                    "icon_rgb2 INTEGER NOT NULL," +
                    "splash_url TEXT," +
                    "splash_rgb INTEGER NOT NULL," +
                    "splash_rgb2 INTEGER NOT NULL," +
                    "ord INTEGER NOT NULL," +
                    "PRIMARY KEY (id))");

            db.execSQL("CREATE INDEX idx_podcasts__ord ON podcasts (ord DESC)");

            db.execSQL("CREATE TABLE records (" +
                    "podcast_id INTEGER NOT NULL," +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "url TEXT NOT NULL," +
                    "description TEXT," +
                    "date TEXT," +
                    "duration TEXT," +
                    "played INTEGER NOT NULL," +
                    "PRIMARY KEY (podcast_id, id))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException();
        }
    }

    public class PodcastsOpenHelperVersion4 extends SQLiteOpenHelper {
        PodcastsOpenHelperVersion4(Context context) {
            super(context, TEST_DATABASE_NAME, null, 4);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE podcasts (" +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "length INTEGER NOT NULL," +
                    "seen INTEGER NOT NULL DEFAULT 0," +
                    "icon_url TEXT," +
                    "icon_rgb INTEGER NOT NULL DEFAULT 0," +
                    "icon_rgb2 INTEGER NOT NULL DEFAULT 0," +
                    "splash_url TEXT," +
                    "splash_rgb INTEGER NOT NULL DEFAULT 0," +
                    "splash_rgb2 INTEGER NOT NULL DEFAULT 0," +
                    "ord INTEGER NOT NULL," +
                    "PRIMARY KEY (id))");

            db.execSQL("CREATE TABLE records (" +
                    "podcast_id INTEGER NOT NULL," +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "url TEXT NOT NULL," +
                    "description TEXT," +
                    "date TEXT," +
                    "duration TEXT," +
                    "PRIMARY KEY (podcast_id, id))");

            db.execSQL("CREATE TABLE players ("
                    + "podcast_id INTEGER NOT NULL,"
                    + "record_id INTEGER NOT NULL,"
                    + "position INTEGER NOT NULL,"
                    + "PRIMARY KEY (podcast_id, record_id)"
                    + ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException();
        }
    }

    public class PodcastsOpenHelperVersion5 extends SQLiteOpenHelper {
        PodcastsOpenHelperVersion5(Context context) {
            super(context, TEST_DATABASE_NAME, null, 5);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE podcasts (" +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "length INTEGER NOT NULL," +
                    "seen INTEGER NOT NULL DEFAULT 0," +
                    "icon_url TEXT," +
                    "icon_rgb INTEGER NOT NULL DEFAULT 0," +
                    "icon_rgb2 INTEGER NOT NULL DEFAULT 0," +
                    "splash_url TEXT," +
                    "splash_rgb INTEGER NOT NULL DEFAULT 0," +
                    "splash_rgb2 INTEGER NOT NULL DEFAULT 0," +
                    "ord INTEGER NOT NULL," +
                    "rating INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (id))");

            db.execSQL("CREATE TABLE records (" +
                    "podcast_id INTEGER NOT NULL," +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "url TEXT NOT NULL," +
                    "description TEXT," +
                    "date TEXT," +
                    "duration TEXT," +
                    "PRIMARY KEY (podcast_id, id))");

            db.execSQL("CREATE TABLE players ("
                    + "podcast_id INTEGER NOT NULL,"
                    + "record_id INTEGER NOT NULL,"
                    + "position INTEGER NOT NULL,"
                    + "PRIMARY KEY (podcast_id, record_id)"
                    + ")");

            db.execSQL(PodcastsReadableDatabase.PodcastsTable.CREATE_RATING_ORD_INDEX_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException();
        }
    }

    public class PodcastsOpenHelperVersion6 extends SQLiteOpenHelper {
        PodcastsOpenHelperVersion6(Context context) {
            super(context, TEST_DATABASE_NAME, null, 6);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE podcasts (" +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "length INTEGER NOT NULL," +
                    "seen INTEGER NOT NULL DEFAULT 0," +
                    "icon_url TEXT," +
                    "icon_rgb INTEGER NOT NULL DEFAULT 0," +
                    "icon_rgb2 INTEGER NOT NULL DEFAULT 0," +
                    "splash_url TEXT," +
                    "splash_rgb INTEGER NOT NULL DEFAULT 0," +
                    "splash_rgb2 INTEGER NOT NULL DEFAULT 0," +
                    "ord INTEGER NOT NULL," +
                    "rating INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (id))");

            db.execSQL("CREATE TABLE records (" +
                    "podcast_id INTEGER NOT NULL," +
                    "id INTEGER NOT NULL," +
                    "name TEXT NOT NULL," +
                    "url TEXT NOT NULL," +
                    "description TEXT," +
                    "date TEXT," +
                    "duration TEXT," +
                    "PRIMARY KEY (podcast_id, id))");

            db.execSQL("CREATE TABLE players ("
                    + "podcast_id INTEGER NOT NULL,"
                    + "record_id INTEGER NOT NULL,"
                    + "position INTEGER NOT NULL,"
                    + "length INTEGER NOT NULL,"
                    + "PRIMARY KEY (podcast_id, record_id)"
                    + ")");

            db.execSQL(PodcastsReadableDatabase.PodcastsTable.CREATE_RATING_ORD_INDEX_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException();
        }
    }
}
