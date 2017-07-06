package ru.radiomayak.podcasts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.List;

import ru.radiomayak.StringUtils;

public final class PodcastsUtils {
    private static final String PODCASTS_DATABASE_NAME = "podcasts";

    private static final String PODCAST_ICON_FILE = "icon-%s";
    private static final String PODCAST_SPLASH_FILE = "splash-%s";

    private static final String PODCAST_ID = PodcastsOpenHelper.PODCAST_ID;
    private static final String PODCAST_NAME = PodcastsOpenHelper.PODCAST_NAME;
    private static final String PODCAST_DESC = PodcastsOpenHelper.PODCAST_DESC;
    private static final String PODCAST_LENGTH = PodcastsOpenHelper.PODCAST_LENGTH;
    private static final String PODCAST_ICON_URL = PodcastsOpenHelper.PODCAST_ICON_URL;
    private static final String PODCAST_ICON_RGB = PodcastsOpenHelper.PODCAST_ICON_RGB;
    private static final String PODCAST_ICON_RGB2 = PodcastsOpenHelper.PODCAST_ICON_RGB2;
    private static final String PODCAST_SPLASH_URL = PodcastsOpenHelper.PODCAST_SPLASH_URL;
    private static final String PODCAST_SPLASH_RGB = PodcastsOpenHelper.PODCAST_SPLASH_RGB;
    private static final String PODCAST_SPLASH_RGB2 = PodcastsOpenHelper.PODCAST_SPLASH_RGB2;
    private static final String PODCAST_ORD = PodcastsOpenHelper.PODCAST_ORD;

    private static final String RECORD_PODCAST_ID = PodcastsOpenHelper.RECORD_PODCAST_ID;
    private static final String RECORD_ID = PodcastsOpenHelper.RECORD_ID;
    private static final String RECORD_NAME = PodcastsOpenHelper.RECORD_NAME;
    private static final String RECORD_URL = PodcastsOpenHelper.RECORD_URL;
    private static final String RECORD_DESC = PodcastsOpenHelper.RECORD_DESC;
    private static final String RECORD_DATE = PodcastsOpenHelper.RECORD_DATE;
    private static final String RECORD_DURATION = PodcastsOpenHelper.RECORD_DURATION;
    private static final String RECORD_PLAYED = PodcastsOpenHelper.RECORD_PLAYED;

    private static final String[] PODCASTS_SELECT_FIELDS = {PODCAST_ID, PODCAST_NAME, PODCAST_DESC, PODCAST_LENGTH,
            PODCAST_ICON_URL, PODCAST_ICON_RGB, PODCAST_ICON_RGB2, PODCAST_SPLASH_URL, PODCAST_SPLASH_RGB, PODCAST_SPLASH_RGB2};

    private static final String PODCASTS_SELECT_SQL = "SELECT " + StringUtils.join(PODCASTS_SELECT_FIELDS, ", ") +
            " FROM " + PodcastsOpenHelper.PODCASTS + " WHERE " + PODCAST_ORD + " > 0" +
            " ORDER BY " + PODCAST_ORD + " DESC";

    private static final String[] PODCAST_COLORS_SELECT_FIELDS = {PODCAST_ICON_URL, PODCAST_ICON_RGB, PODCAST_ICON_RGB2,
            PODCAST_SPLASH_URL, PODCAST_SPLASH_RGB, PODCAST_SPLASH_RGB2};

    private static final String PODCAST_COLORS_SELECT_SQL = "SELECT " + StringUtils.join(PODCAST_COLORS_SELECT_FIELDS, ", ") +
            " FROM " + PodcastsOpenHelper.PODCASTS + " WHERE " + PODCAST_ID + " = ?";

    private static final String[] RECORDS_SELECT_FIELDS = {RECORD_ID, RECORD_NAME, RECORD_URL, RECORD_DESC,
            RECORD_DATE, RECORD_DURATION, RECORD_PLAYED};

    private static final String RECORD_PLAYED_SELECT_SQL = "SELECT " + RECORD_PLAYED +
            " FROM " + PodcastsOpenHelper.RECORDS + " WHERE " + RECORD_PODCAST_ID + " = ? AND " + RECORD_ID + " = ?";

    private static final ThreadLocal<String[]> IDENTITY_LEN_ARRAY = new ThreadLocal<>();
    private static final ThreadLocal<String[]> DOUBLE_LEN_ARRAY = new ThreadLocal<>();

    private PodcastsUtils() {
    }

    static Podcasts loadPodcasts(Context context) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getReadableDatabase()) {
            try (Cursor cursor = database.rawQuery(PODCASTS_SELECT_SQL, null)) {
                Podcasts podcasts = new Podcasts(cursor.getCount());
                while (cursor.moveToNext()) {
                    int i = 0;
                    Podcast podcast = new Podcast(cursor.getLong(i), cursor.getString(i + 1));
                    i += 2;
                    podcast.setDescription(cursor.getString(i++));
                    podcast.setLength(cursor.getInt(i++));

                    String iconUrl = cursor.getString(i++);
                    if (iconUrl != null) {
                        int iconPrimaryColor = cursor.getInt(i);
                        int iconSecondaryColor = cursor.getInt(i + 1);
                        podcast.setIcon(new Image(iconUrl, iconPrimaryColor, iconSecondaryColor));
                    }
                    i += 2;

                    String splashUrl = cursor.getString(i++);
                    if (splashUrl != null) {
                        int imagePrimaryColor = cursor.getInt(i);
                        int imageSecondaryColor = cursor.getInt(i + 1);
                        podcast.setSplash(new Image(splashUrl, imagePrimaryColor, imageSecondaryColor));
                    }
                    podcasts.add(podcast);
                }
                return podcasts;
            }
        }
    }

    static void storePodcasts(Context context, Podcasts podcasts) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getWritableDatabase()) {
            ContentValues resetValues = new ContentValues();
            resetValues.put(PODCAST_ORD, 0);
            database.update(PodcastsOpenHelper.PODCASTS, resetValues, null, null);
            int index = podcasts.list().size();
            for (Podcast podcast : podcasts.list()) {
                ContentValues values = new ContentValues();
                values.put(PODCAST_ID, podcast.getId());
                values.put(PODCAST_NAME, podcast.getName());
                if (podcast.getDescription() != null) {
                    values.put(PODCAST_DESC, podcast.getDescription());
                }
                values.put(PODCAST_LENGTH, podcast.getLength());
                Cursor cursor = null;
                try {
                    if (podcast.getIcon() != null) {
                        cursor = updateColors(database, null, podcast, podcast.getIcon(), 0);
                    }
                    putColors(values, podcast.getIcon(), PODCAST_ICON_URL, PODCAST_ICON_RGB, PODCAST_ICON_RGB2);
                    if (podcast.getSplash() != null) {
                        cursor = updateColors(database, cursor, podcast, podcast.getSplash(), 3);
                    }
                    putColors(values, podcast.getSplash(), PODCAST_SPLASH_URL, PODCAST_SPLASH_RGB, PODCAST_SPLASH_RGB2);
                } finally {
                    if (cursor != null) {
                        IOUtils.closeQuietly(cursor);
                    }
                }
                values.put(PODCAST_ORD, index);
                database.insertWithOnConflict(PodcastsOpenHelper.PODCASTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                index--;
            }
        }
    }

    private static void putColors(ContentValues values, Image image, String url, String rgb, String rgb2) {
        if (image != null) {
            values.put(url, image.getUrl());
            values.put(rgb, image.getPrimaryColor());
            values.put(rgb2, image.getSecondaryColor());
        } else {
            values.putNull(url);
            values.put(rgb, 0);
            values.put(rgb2, 0);
        }
    }

    private static Cursor updateColors(SQLiteDatabase database, Cursor cursor, Podcast podcast, Image image, int index) {
        if (image.getPrimaryColor() == 0) {
            if (cursor == null) {
                cursor = database.rawQuery(PODCAST_COLORS_SELECT_SQL, args(toString(podcast.getId())));
            }
            if (!cursor.isFirst() && !cursor.moveToFirst()) {
                return cursor;
            }
            if (image.getUrl().equalsIgnoreCase(cursor.getString(index))) {
                image.setColors(cursor.getInt(index + 1), cursor.getInt(index + 2));
            }
        }
        return cursor;
    }

    static void storePodcastIconColors(Context context, long id, int primaryColor, int secondaryColor) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(PODCAST_ICON_RGB, primaryColor);
            values.put(PODCAST_ICON_RGB2, secondaryColor);
            database.update(PodcastsOpenHelper.PODCASTS, values, PODCAST_ID + " = ?", args(toString(id)));
        }
    }

    static void storePodcastSplashColors(Context context, long id, int primaryColor, int secondaryColor) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(PODCAST_SPLASH_RGB, primaryColor);
            values.put(PODCAST_SPLASH_RGB2, secondaryColor);
            database.update(PodcastsOpenHelper.PODCASTS, values, PODCAST_ID + " = ?", args(toString(id)));
        }
    }

    static String getPodcastIconFilename(Podcast podcast) {
        return String.format(PODCAST_ICON_FILE, toString(podcast.getId()));
    }

    static String getPodcastSplashFilename(Podcast podcast) {
        return String.format(PODCAST_SPLASH_FILE, toString(podcast.getId()));
    }

    static List<Record> loadRecords(Context context, long podcast, long from, int count) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getReadableDatabase()) {
            String sql = "SELECT " + StringUtils.join(RECORDS_SELECT_FIELDS, ", ") + " FROM " + PodcastsOpenHelper.RECORDS;
            String args[];
            if (from > 0) {
                sql += " WHERE " + RECORD_PODCAST_ID + " = ? AND " + RECORD_ID + " < ?";
                args = args(toString(podcast), toString(from));
            } else {
                sql += " WHERE " + RECORD_PODCAST_ID + " = ?";
                args = args(toString(podcast));
            }
            sql += " ORDER BY " + RECORD_ID + " DESC LIMIT " + count;

            try (Cursor cursor = database.rawQuery(sql, args)) {
                List<Record> records = new ArrayList<>(count);
                while (cursor.moveToNext()) {
                    int i = 0;
                    long id = cursor.getLong(i++);
                    String name = cursor.getString(i++);
                    String url = cursor.getString(i++);
                    Record record = new Record(id, name, url);
                    record.setDescription(cursor.getString(i++));
                    record.setDate(cursor.getString(i++));
                    record.setDuration(cursor.getString(i++));
                    record.setPlayed(cursor.getInt(i) != 0);
                    records.add(record);
                }
                return records;
            }
        }
    }

    static void storeRecords(Context context, long podcast, List<Record> records) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getWritableDatabase()) {
            for (Record record : records) {
                ContentValues values = new ContentValues();
                values.put(RECORD_PODCAST_ID, podcast);
                values.put(RECORD_ID, record.getId());
                values.put(RECORD_NAME, record.getName());
                values.put(RECORD_URL, record.getUrl());
                values.put(RECORD_DESC, record.getDescription());
                values.put(RECORD_DATE, record.getDate());
                values.put(RECORD_DURATION, record.getDuration());
                if (!record.isPlayed()) {
                    try (Cursor cursor = database.rawQuery(RECORD_PLAYED_SELECT_SQL, args(toString(podcast), toString(record.getId())))) {
                        if (cursor.moveToNext()) {
                            record.setPlayed(cursor.getInt(0) != 0);
                        }
                    }
                }
                values.put(RECORD_PLAYED, record.isPlayed() ? 1 : 0);
                database.insertWithOnConflict(PodcastsOpenHelper.RECORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    public static void storeRecordPlayedProperty(Context context, long podcast, Record record) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(RECORD_PLAYED, record.isPlayed() ? 1 : 0);
            String[] args = args(toString(podcast), toString(record.getId()));
            database.update(PodcastsOpenHelper.RECORDS, values, RECORD_PODCAST_ID + " = ? AND " + RECORD_ID + " = ?", args);
        }
    }

    private static String toString(long arg) {
        return String.valueOf(arg);
    }

    private static String[] args(String arg) {
        String[] array = IDENTITY_LEN_ARRAY.get();
        if (array == null) {
            array = new String[1];
            IDENTITY_LEN_ARRAY.set(array);
        }
        array[0] = arg;
        return array;
    }

    private static String[] args(String arg1, String arg2) {
        String[] array = DOUBLE_LEN_ARRAY.get();
        if (array == null) {
            array = new String[2];
            DOUBLE_LEN_ARRAY.set(array);
        }
        array[0] = arg1;
        array[1] = arg2;
        return array;
    }
}
