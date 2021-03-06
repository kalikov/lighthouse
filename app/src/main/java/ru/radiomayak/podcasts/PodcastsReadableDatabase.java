package ru.radiomayak.podcasts;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.radiomayak.StringUtils;

public class PodcastsReadableDatabase implements AutoCloseable {
    private static final ThreadLocal<String[]> IDENTITY_LEN_ARRAY = new ThreadLocal<>();
    private static final ThreadLocal<String[]> DOUBLE_LEN_ARRAY = new ThreadLocal<>();

    static final String DATABASE_NAME = "podcasts";
    static final int VERSION = 8;

    protected static final String SELECT = "SELECT ";
    protected static final String FROM = " FROM ";
    protected static final String WHERE = " WHERE ";
    protected static final String AND = " AND ";
    protected static final String JOIN = " JOIN ";
    protected static final String ON = " ON ";
    protected static final String ORDER_BY = " ORDER BY ";
    protected static final String DESC = " DESC";
    protected static final String LIMIT = " LIMIT ";
    protected static final String COMMA = ",";
    protected static final String DOT = ".";

    interface TableField {
        String key();

        String type();
    }

    static class PodcastsTable {
        static final String NAME = "podcasts";

        static final String CREATE_SQL = "CREATE TABLE " + NAME + " (" + fields(Field.values()) + COMMA
                + "PRIMARY KEY (" + Field.ID.key() + ")"
                + ")";

        static final String CREATE_RATING_ORD_INDEX_SQL = "CREATE INDEX idx_podcasts__rating_ord"
                + ON + NAME + " (" + Field.RATING.key() + DESC + COMMA + Field.ORD.key() + DESC + ")";

        static final String SELECT_SQL = SELECT + join(Field.values()) + FROM + NAME;

        enum Field implements TableField {
            ID("id", "INTEGER NOT NULL"),
            NAME("name", "TEXT NOT NULL"),
            DESC("description", "TEXT"),
            LENGTH("length", "INTEGER NOT NULL"),
            SEEN("seen", "INTEGER NOT NULL DEFAULT 0"),
            ICON_URL("icon_url", "TEXT"),
            ICON_RGB("icon_rgb", "INTEGER NOT NULL DEFAULT 0"),
            ICON_RGB2("icon_rgb2", "INTEGER NOT NULL DEFAULT 0"),
            SPLASH_URL("splash_url", "TEXT"),
            SPLASH_RGB("splash_rgb", "INTEGER NOT NULL DEFAULT 0"),
            SPLASH_RGB2("splash_rgb2", "INTEGER NOT NULL DEFAULT 0"),
            ORD("ord", "INTEGER NOT NULL"),
            RATING("rating", "INTEGER NOT NULL DEFAULT 0");

            private final String key;
            private final String type;

            Field(String key, String type) {
                this.key = key;
                this.type = type;
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public String type() {
                return type;
            }
        }
    }

    static class RecordsTable {
        static final String NAME = "records";

        static final String CREATE_SQL = "CREATE TABLE " + NAME + " (" + fields(Field.values()) + COMMA
                + "PRIMARY KEY (" + Field.PODCAST_ID.key() + ", " + Field.ID.key() + ")"
                + ")";

        enum Field implements TableField {
            PODCAST_ID("podcast_id", "INTEGER NOT NULL"),
            ID("id", "INTEGER NOT NULL"),
            NAME("name", "TEXT NOT NULL"),
            URL("url", "TEXT NOT NULL"),
            DESC("description", "TEXT"),
            DATE("date", "TEXT"),
            DURATION("duration", "TEXT");

            private final String key;
            private final String type;

            Field(String key, String type) {
                this.key = key;
                this.type = type;
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public String type() {
                return type;
            }
        }
    }

    static class PlayersTable {
        static final String NAME = "players";

        static final String CREATE_SQL = "CREATE TABLE " + NAME + " (" + fields(Field.values()) + COMMA
                + "PRIMARY KEY (" + Field.PODCAST_ID.key() + ", " + Field.RECORD_ID.key() + ")"
                + ")";

        static final String SELECT_SQL = SELECT + join(Field.values()) + FROM + NAME;

        static final String CREATE_PLAYED_ORD_INDEX_SQL = "CREATE INDEX idx_players__played_ord"
                + ON + NAME + " (" + Field.PLAYED.key() + DESC + COMMA + Field.PODCAST_ID.key() + COMMA + Field.RECORD_ID + ")";

        static final String CREATE_NUM_INDEX_SQL = "CREATE UNIQUE INDEX idx_players__num"
                + ON + NAME + " (" + Field.NUM.key() + DESC + ")";

        enum Field implements TableField {
            PODCAST_ID("podcast_id", "INTEGER NOT NULL"),
            RECORD_ID("record_id", "INTEGER NOT NULL"),
            POSITION("position", "INTEGER NOT NULL"),
            LENGTH("length", "INTEGER NOT NULL"),
            PLAYED("played", "INTEGER NOT NULL"),
            NUM("num", "INTEGER NOT NULL");

            private final String key;
            private final String type;

            Field(String key, String type) {
                this.key = key;
                this.type = type;
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public String type() {
                return type;
            }
        }
    }

    protected final SQLiteDatabase db;

    public static PodcastsReadableDatabase get(PodcastsOpenHelper helper) {
        return new PodcastsReadableDatabase(helper.getReadableDatabase());
    }

    public PodcastsReadableDatabase(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public void close() {
        db.close();
    }

    public void beginExclusiveTransaction() {
        db.beginTransaction();
    }

    public void beginTransaction() {
        db.beginTransactionNonExclusive();
    }

    public void endTransaction() {
        db.endTransaction();
    }

    public void commit() {
        db.setTransactionSuccessful();
    }

    public Podcasts loadPodcasts() {
        return loadPodcasts(PodcastsTable.Field.ORD.key() + " > 0", null);
    }

    public Podcasts loadArchivePodcasts() {
        return loadPodcasts(PodcastsTable.Field.ORD.key() + " < 0", null);
    }

    private Podcasts loadPodcasts(String whereClause, String[] args) {
        try (Cursor cursor = db.rawQuery(PodcastsTable.SELECT_SQL + WHERE + whereClause +
                ORDER_BY + PodcastsTable.Field.RATING.key() + DESC + COMMA + PodcastsTable.Field.ORD.key() + DESC, args)) {
            Podcasts podcasts = new Podcasts(cursor.getCount());
            while (cursor.moveToNext()) {
                long id = cursor.getLong(PodcastsTable.Field.ID.ordinal());
                String name = cursor.getString(PodcastsTable.Field.NAME.ordinal());
                Podcast podcast = new Podcast(id, name);
                podcast.setDescription(cursor.getString(PodcastsTable.Field.DESC.ordinal()));
                podcast.setLength(cursor.getInt(PodcastsTable.Field.LENGTH.ordinal()));
                podcast.setSeen(cursor.getInt(PodcastsTable.Field.SEEN.ordinal()));
                podcast.setFavorite(cursor.getInt(PodcastsTable.Field.RATING.ordinal()));

                String iconUrl = cursor.getString(PodcastsTable.Field.ICON_URL.ordinal());
                if (iconUrl != null) {
                    int iconPrimaryColor = cursor.getInt(PodcastsTable.Field.ICON_RGB.ordinal());
                    int iconSecondaryColor = cursor.getInt(PodcastsTable.Field.ICON_RGB2.ordinal());
                    podcast.setIcon(new Image(iconUrl, iconPrimaryColor, iconSecondaryColor));
                }

                String splashUrl = cursor.getString(PodcastsTable.Field.SPLASH_URL.ordinal());
                if (splashUrl != null) {
                    int imagePrimaryColor = cursor.getInt(PodcastsTable.Field.SPLASH_RGB.ordinal());
                    int imageSecondaryColor = cursor.getInt(PodcastsTable.Field.SPLASH_RGB2.ordinal());
                    podcast.setSplash(new Image(splashUrl, imagePrimaryColor, imageSecondaryColor));
                }
                podcasts.add(podcast);
            }
            return podcasts;
        }
    }

    @Nullable
    public Image loadPodcastIcon(long podcast) {
        try (Cursor cursor = db.rawQuery(PodcastsTable.SELECT_SQL + WHERE + PodcastsTable.Field.ID + " = ?", args(podcast))) {
            if (cursor.moveToNext()) {
                return new Image(cursor.getString(PodcastsTable.Field.ICON_URL.ordinal()),
                        cursor.getInt(PodcastsTable.Field.ICON_RGB.ordinal()), cursor.getInt(PodcastsTable.Field.ICON_RGB2.ordinal()));
            }
        }
        return null;
    }

    public Image loadPodcastSplash(long podcast) {
        try (Cursor cursor = db.rawQuery(PodcastsTable.SELECT_SQL + WHERE + PodcastsTable.Field.ID + " = ?", args(podcast))) {
            if (cursor.moveToNext()) {
                String url = cursor.getString(PodcastsTable.Field.SPLASH_URL.ordinal());
                if (!StringUtils.isEmpty(url)) {
                    return new Image(url, cursor.getInt(PodcastsTable.Field.SPLASH_RGB.ordinal()), cursor.getInt(PodcastsTable.Field.SPLASH_RGB2.ordinal()));
                }
            }
        }
        return null;
    }

    public void loadRecordsPositionAndLength(long podcast, Records records) {
        if (records.isEmpty()) {
            return;
        }
        StringBuilder queryBuilder = new StringBuilder(PlayersTable.SELECT_SQL);
        String[] args = new String[records.list().size() + 1];

        queryBuilder.append(WHERE).append(PlayersTable.Field.PODCAST_ID.key()).append(" = ?");
        args[0] = String.valueOf(podcast);

        queryBuilder.append(AND).append(PlayersTable.Field.RECORD_ID.key()).append(" IN (");
        buildArgs(queryBuilder, args, 1, records.list());
        queryBuilder.append(')');

        try (Cursor cursor = db.rawQuery(queryBuilder.toString(), args)) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(PlayersTable.Field.RECORD_ID.ordinal());
                Record record = records.get(id);
                if (record != null) {
                    int position = cursor.getInt(PlayersTable.Field.POSITION.ordinal());
                    int length = cursor.getInt(PlayersTable.Field.LENGTH.ordinal());
                    record.setPosition(position);
                    record.setLength(length);
                }
            }
        }
    }

    public HistoryPage loadHistory(int start, int limit) {
        HistoryTracks tracks;
        int next;
        Map<Long, Podcast> podcastsMap = new LinkedHashMap<>();
        try (Cursor cursor = db.rawQuery(SELECT + join(RecordsTable.NAME, RecordsTable.Field.values()) +
                COMMA + PlayersTable.Field.POSITION.key() + COMMA + PlayersTable.Field.LENGTH.key() +
                COMMA + PlayersTable.Field.NUM.key() + COMMA + PlayersTable.Field.PLAYED.key() +
                FROM + RecordsTable.NAME +
                JOIN + PlayersTable.NAME + ON + RecordsTable.NAME + DOT + RecordsTable.Field.PODCAST_ID.key() + " = " + PlayersTable.NAME + DOT + PlayersTable.Field.PODCAST_ID.key() +
                AND + RecordsTable.Field.ID.key() + " = " + PlayersTable.NAME + DOT + PlayersTable.Field.RECORD_ID.key() +
                WHERE + PlayersTable.Field.NUM.key() + " < " + start +
                ORDER_BY + PlayersTable.Field.NUM.key() + DESC + LIMIT + (limit + 1), null)) {
            tracks = new HistoryTracks(cursor.getCount());
            int index = 0;
            while (index < limit && cursor.moveToNext()) {
                long podcastId = cursor.getLong(RecordsTable.Field.PODCAST_ID.ordinal());
                long recordId = cursor.getLong(RecordsTable.Field.ID.ordinal());
                String name = cursor.getString(RecordsTable.Field.NAME.ordinal());
                String url = cursor.getString(RecordsTable.Field.URL.ordinal());
                Record record = new Record(recordId, name, url);
                record.setDescription(cursor.getString(RecordsTable.Field.DESC.ordinal()));
                record.setDate(cursor.getString(RecordsTable.Field.DATE.ordinal()));
                record.setDuration(cursor.getString(RecordsTable.Field.DURATION.ordinal()));
                record.setPosition(cursor.getInt(RecordsTable.Field.values().length));
                record.setLength(cursor.getInt(RecordsTable.Field.values().length + 1));
                long playTime = cursor.getLong(RecordsTable.Field.values().length + 3);

                Podcast podcast = podcastsMap.get(podcastId);
                if (podcast == null) {
                    podcast = new Podcast(podcastId, String.valueOf(podcastId));
                    podcastsMap.put(podcastId, podcast);
                }
                tracks.add(new HistoryTrack(podcast, record, playTime));
                index++;
            }
            next = cursor.moveToNext() ? cursor.getInt(RecordsTable.Field.values().length + 2) : 0;
        }

        if (!podcastsMap.isEmpty()) {
            String[] args = new String[podcastsMap.size()];

            StringBuilder whereBuilder = new StringBuilder(PodcastsTable.Field.ID.key());
            whereBuilder.append(" IN (");
            buildArgs(whereBuilder, args, 0, podcastsMap.values());
            whereBuilder.append(")");
            Podcasts podcasts = loadPodcasts(whereBuilder.toString(), args);
            for (Podcast podcast : podcasts.list()) {
                podcastsMap.get(podcast.getId()).update(podcast);
            }
        }
        return new HistoryPage(tracks, next);
    }

    private static <T extends Identifiable> void buildArgs(StringBuilder builder, String[] args, int offset, Collection<T> items) {
        int index = 0;
        for (Identifiable item : items) {
            if (index > 0) {
                builder.append(COMMA);
            }
            builder.append('?');
            args[index + offset] = String.valueOf(item.getId());
            index++;
        }
    }

    protected static String fields(TableField[] fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(COMMA);
            }
            builder.append(fields[i].key()).append(' ').append(fields[i].type());
        }
        return builder.toString();
    }

    protected static String join(TableField[] fields) {
        return join("", fields);
    }

    protected static String join(String prefix, TableField[] fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(COMMA);
            }
            if (!StringUtils.isEmpty(prefix)) {
                builder.append(prefix).append('.');
            }
            builder.append(fields[i].key());
        }
        return builder.toString();
    }

    protected static String[] args(long arg) {
        return args(String.valueOf(arg));
    }

    protected static String[] args(String arg) {
        String[] array = IDENTITY_LEN_ARRAY.get();
        if (array == null) {
            array = new String[1];
            IDENTITY_LEN_ARRAY.set(array);
        }
        array[0] = arg;
        return array;
    }

    protected static String[] args(long arg1, long arg2) {
        return args(String.valueOf(arg1), String.valueOf(arg2));
    }

    protected static String[] args(String arg1, String arg2) {
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
