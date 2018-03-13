package ru.radiomayak.podcasts;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.radiomayak.StringUtils;

public class PodcastsOpenHelper extends SQLiteOpenHelper {
    private static final int VERSION = 4;

    static final String PODCASTS = "podcasts";
    static final String RECORDS = "records";
    static final String PLAYERS = "players";

    static final String PODCAST_ID = "id";
    static final String PODCAST_NAME = "name";
    static final String PODCAST_DESC = "description";
    static final String PODCAST_LENGTH = "length";
    static final String PODCAST_SEEN = "seen";
    static final String PODCAST_ICON_URL = "icon_url";
    static final String PODCAST_ICON_RGB = "icon_rgb";
    static final String PODCAST_ICON_RGB2 = "icon_rgb2";
    static final String PODCAST_SPLASH_URL = "splash_url";
    static final String PODCAST_SPLASH_RGB = "splash_rgb";
    static final String PODCAST_SPLASH_RGB2 = "splash_rgb2";
    static final String PODCAST_ORD = "ord";

    static final String RECORD_PODCAST_ID = "podcast_id";
    static final String RECORD_ID = "id";
    static final String RECORD_NAME = "name";
    static final String RECORD_URL = "url";
    static final String RECORD_DESC = "description";
    static final String RECORD_DATE = "date";
    static final String RECORD_DURATION = "duration";

    static final String PLAYER_PODCAST_ID = "podcast_id";
    static final String PLAYER_RECORD_ID = "record_id";
    static final String PLAYER_POSITION = "position";

    private static final String CREATE_PODCASTS_SQL = "CREATE TABLE " + PODCASTS + " ("
            + PODCAST_ID + " INTEGER NOT NULL,"
            + PODCAST_NAME + " TEXT NOT NULL,"
            + PODCAST_DESC + " TEXT,"
            + PODCAST_LENGTH + " INTEGER NOT NULL,"
            + PODCAST_SEEN + " INTEGER NOT NULL DEFAULT 0,"
            + PODCAST_ICON_URL + " TEXT,"
            + PODCAST_ICON_RGB + " INTEGER NOT NULL DEFAULT 0,"
            + PODCAST_ICON_RGB2 + " INTEGER NOT NULL DEFAULT 0,"
            + PODCAST_SPLASH_URL + " TEXT,"
            + PODCAST_SPLASH_RGB + " INTEGER NOT NULL DEFAULT 0,"
            + PODCAST_SPLASH_RGB2 + " INTEGER NOT NULL DEFAULT 0,"
            + PODCAST_ORD + " INTEGER NOT NULL,"
            + "PRIMARY KEY (" + PODCAST_ID + ")"
            + ")";

    private static final String CREATE_PODCASTS_ORD_INDEX_SQL = "CREATE INDEX idx_podcasts__ord"
            + " ON " + PODCASTS + " (" + PODCAST_ORD + " DESC)";

    private static final String CREATE_RECORDS_SQL = "CREATE TABLE " + RECORDS + " ("
            + RECORD_PODCAST_ID + " INTEGER NOT NULL,"
            + RECORD_ID + " INTEGER NOT NULL,"
            + RECORD_NAME + " TEXT NOT NULL,"
            + RECORD_URL + " TEXT NOT NULL,"
            + RECORD_DESC + " TEXT,"
            + RECORD_DATE + " TEXT,"
            + RECORD_DURATION + " TEXT,"
            + "PRIMARY KEY (" + RECORD_PODCAST_ID + ", " + RECORD_ID + ")"
            + ")";

    private static final String CREATE_PLAYERS_SQL = "CREATE TABLE " + PLAYERS + " ("
            + PLAYER_PODCAST_ID + " INTEGER NOT NULL,"
            + PLAYER_RECORD_ID + " INTEGER NOT NULL,"
            + PLAYER_POSITION + " INTEGER NOT NULL,"
            + "PRIMARY KEY (" + RECORD_PODCAST_ID + ", " + PLAYER_RECORD_ID + ")"
            + ")";

    private static final String MIGRATE_RECORDS_PLAYED_SQL = "INSERT INTO " + PLAYERS
            + "(" + PLAYER_PODCAST_ID + ", " + PLAYER_RECORD_ID + ", " + PLAYER_POSITION + ")"
            + " SELECT " + RECORD_PODCAST_ID + ", " + RECORD_ID + ", 0 FROM " + RECORDS + " WHERE played = 1";

    public PodcastsOpenHelper(Context context) {
        this(context, PodcastsReadableDatabase.DATABASE_NAME);
    }

    PodcastsOpenHelper(Context context, String name) {
        super(context, name, null, VERSION);
    }

    PodcastsOpenHelper(Context context, String name, DatabaseErrorHandler errorHandler) {
        super(context, name, null, VERSION, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PODCASTS_SQL);
        db.execSQL(CREATE_PODCASTS_ORD_INDEX_SQL);
        db.execSQL(CREATE_RECORDS_SQL);
        db.execSQL(CREATE_PLAYERS_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 1) {
            db.execSQL("ALTER TABLE " + PODCASTS + " ADD COLUMN " + PODCAST_SEEN + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE " + PODCASTS + " SET " + PODCAST_SEEN + " = " + PODCAST_LENGTH);
        }
        if (oldVersion <= 2) {
            db.execSQL("UPDATE " + PODCASTS + " SET " + PODCAST_SEEN + " = " + PODCAST_LENGTH
                    + " WHERE " + PODCAST_SEEN + " > " + PODCAST_LENGTH);
        }
        if (oldVersion <= 3) {
            db.execSQL(CREATE_PLAYERS_SQL);
            db.execSQL(MIGRATE_RECORDS_PLAYED_SQL);
            db.execSQL("DROP TABLE " + RECORDS);
            db.execSQL(CREATE_RECORDS_SQL);
            db.execSQL("ALTER TABLE " + PODCASTS + " RENAME TO old_podcasts");
            db.execSQL(CREATE_PODCASTS_SQL);
            String columns = StringUtils.join(new String[] {PODCAST_ID, PODCAST_NAME, PODCAST_DESC, PODCAST_LENGTH,
                    PODCAST_SEEN, PODCAST_ICON_URL, PODCAST_ICON_RGB, PODCAST_ICON_RGB2,
                    PODCAST_SPLASH_URL, PODCAST_SPLASH_RGB, PODCAST_SPLASH_RGB2, PODCAST_ORD}, ", ");
            db.execSQL("INSERT INTO " + PODCASTS + "(" + columns + ") SELECT " + columns + " FROM old_podcasts");
            db.execSQL("DROP TABLE old_podcasts");
        }
    }
}
