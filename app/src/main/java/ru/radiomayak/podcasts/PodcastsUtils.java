package ru.radiomayak.podcasts;

import java.util.Locale;

public final class PodcastsUtils {
    private static final String PODCAST_ICON_FILE = "icon-%s";
    private static final String PODCAST_SPLASH_FILE = "splash-%s";

    private static final String ZERO_TIME_TEXT = "00:00";

    private PodcastsUtils() {
    }

    public static String formatTime(long msecs) {
        if (msecs <= 0) {
            return ZERO_TIME_TEXT;
        }
        return formatSeconds(msecs / 1000);
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return ZERO_TIME_TEXT;
        }
        long secs = seconds % 60;
        long mins = (seconds / 60) % 60;
        long hours = seconds / 3600;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, mins, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d", mins, secs);
    }

    static String getPodcastIconFilename(Podcast podcast) {
        return String.format(PODCAST_ICON_FILE, String.valueOf(podcast.getId()));
    }

    static String getPodcastSplashFilename(Podcast podcast) {
        return String.format(PODCAST_SPLASH_FILE, String.valueOf(podcast.getId()));
    }

    /*static List<Record> loadRecords(LighthouseApplication application, long podcast, long from, int count) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(application, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getReadableDatabase()) {
            String sql = "SELECT " + StringUtils.join(RECORDS_SELECT_FIELDS, ", ") + " FROM " + PodcastsOpenHelper.RECORDS;
            String args[];
            if (from > 0) {
                sql += " WHERE " + RECORD_PODCAST_ID + " = ? AND " + RECORD_ID + " < ?";
                args = args(podcast, from);
            } else {
                sql += " WHERE " + RECORD_PODCAST_ID + " = ?";
                args = args(podcast);
            }
            sql += " ORDER BY " + RECORD_ID + " DESC LIMIT " + count;

            File cacheDir = application.getCacheDir();
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
//
//                    File cacheFile = CacheUtils.getFile(cacheDir, String.valueOf(podcast), String.valueOf(id));
//                    try (RandomAccessFile file = new RandomAccessFile(cacheFile, "r")) {
//                        ByteMap byteMap = ByteMapUtils.readHeader(file);
//                        if (byteMap != null) {
//                            record.setCacheSize(byteMap.size());
//                        }
//                    } catch (IOException ignored) {
//                    }
                    records.add(record);
                }
                return records;
            }
        }
    }*/

    /*
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
//                if (!record.isPlayed()) {
//                    try (Cursor cursor = database.rawQuery(RECORD_PLAYED_SELECT_SQL, args(podcast, record.getId()))) {
//                        if (cursor.moveToNext()) {
//                            record.setPlayed(cursor.getInt(0) != 0);
//                        }
//                    }
//                }
//                values.put(RECORD_PLAYED, record.isPlayed() ? 1 : 0);
                database.insertWithOnConflict(PodcastsOpenHelper.RECORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }*/
/*
    public static void storeRecordPlayedProperty(Context context, long podcast, Record record) {
        PodcastsOpenHelper helper = new PodcastsOpenHelper(context, PODCASTS_DATABASE_NAME);
        try (SQLiteDatabase database = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
//            values.put(RECORD_PLAYED, record.isPlayed() ? 1 : 0);
            String[] args = args(podcast, record.getId());
            database.update(PodcastsOpenHelper.RECORDS, values, RECORD_PODCAST_ID + " = ? AND " + RECORD_ID + " = ?", args);
        }
    }*/
}
