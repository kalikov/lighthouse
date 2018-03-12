package ru.radiomayak.podcasts;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Collection;

import ru.radiomayak.StringUtils;

public class PodcastsReadableDatabase implements AutoCloseable {
    private static final ThreadLocal<String[]> IDENTITY_LEN_ARRAY = new ThreadLocal<>();
    private static final ThreadLocal<String[]> DOUBLE_LEN_ARRAY = new ThreadLocal<>();

    static final String DATABASE_NAME = "podcasts";

    private static final String SELECT = "SELECT ";
    private static final String FROM = " FROM ";
    private static final String WHERE = " WHERE ";
    private static final String AND = " AND ";
    private static final String COMMA = ",";

    static class RecordsT {
        static final String NAME = "records";

        enum Fields {
            PODCAST_ID("podcast_id"),
            ID("id"),
            NAME("name"),
            URL("url"),
            DESC("description"),
            DATE("date"),
            DURATION("duration");

            private final String name;

            Fields(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }

    static class Players {
        static final String NAME = "players";

        private static final String SELECT_SQL = SELECT + StringUtils.join(Fields.values(), COMMA) + FROM + NAME;

        enum Fields {
            PODCAST_ID("podcast_id"),
            RECORD_ID("record_id"),
            POSITION("position");

            private final String name;

            Fields(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }

    static class Files {
        static final String NAME = "files";

        private static final String SELECT_SQL = SELECT + StringUtils.join(Fields.values(), COMMA) + FROM + NAME;

        enum Fields {
            PODCAST_ID("podcast_id"),
            RECORD_ID("record_id"),
            SIZE("size"),
            CAPACITY("capacity");

            private final String name;

            Fields(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
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

    public void loadRecordsPosition(long podcast, Records records) {
        if (records.isEmpty()) {
            return;
        }
        StringBuilder queryBuilder = new StringBuilder(Players.SELECT_SQL);
        String[] args = new String[records.list().size() + 1];

        queryBuilder.append(WHERE).append(Players.Fields.PODCAST_ID).append(" = ?");
        args[0] = String.valueOf(podcast);

        queryBuilder.append(AND).append(Players.Fields.RECORD_ID).append(" in (");
        buildArgs(queryBuilder, args, 1, records.list());
        queryBuilder.append(')');

        try (Cursor cursor = db.rawQuery(queryBuilder.toString(), args)) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(Players.Fields.RECORD_ID.ordinal());
                Record record = records.get(id);
                if (record != null) {
                    int position = cursor.getInt(Players.Fields.POSITION.ordinal());
                    record.setPosition(position);
                }
            }
        }
    }

    private static void buildArgs(StringBuilder builder, String[] args, int offset, Collection<Record> records) {
        int index = 0;
        for (Record record : records) {
            if (index > 0) {
                builder.append(COMMA);
            }
            builder.append('?');
            args[index + offset] = String.valueOf(record.getId());
            index++;
        }
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
