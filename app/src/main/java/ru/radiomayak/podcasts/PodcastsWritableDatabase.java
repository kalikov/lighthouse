package ru.radiomayak.podcasts;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PodcastsWritableDatabase extends PodcastsReadableDatabase {
    private static final PodcastsTable.Field[] PODCASTS_FIELD_V1_4 = {PodcastsTable.Field.ID, PodcastsTable.Field.NAME,
            PodcastsTable.Field.DESC, PodcastsTable.Field.LENGTH, PodcastsTable.Field.SEEN, PodcastsTable.Field.ORD,
            PodcastsTable.Field.ICON_URL, PodcastsTable.Field.ICON_RGB, PodcastsTable.Field.ICON_RGB2,
            PodcastsTable.Field.SPLASH_URL, PodcastsTable.Field.SPLASH_RGB, PodcastsTable.Field.SPLASH_RGB2};

    public static PodcastsWritableDatabase get(PodcastsOpenHelper helper) {
        return new PodcastsWritableDatabase(helper.getWritableDatabase());
    }

    public PodcastsWritableDatabase(SQLiteDatabase db) {
        super(db);
    }

    public void storePodcasts(Podcasts podcasts) {
        beginTransaction();
        try {
            ContentValues resetValues = new ContentValues();
            resetValues.put(PodcastsTable.Field.ORD.key(), 0);
            int updated = db.update(PodcastsTable.NAME, resetValues, null, null);
            int index = podcasts.list().size();
            for (Podcast podcast : podcasts.list()) {
                storePodcast(podcast, index, updated == 0);
                index--;
            }
            commit();
        } finally {
            endTransaction();
        }
    }

    public void storePodcast(Podcast podcast, int ord, boolean seenIfNew) {
        ContentValues values = new ContentValues();
        long id = podcast.getId();
        values.put(PodcastsTable.Field.NAME.key(), podcast.getName());
        if (podcast.getDescription() != null) {
            values.put(PodcastsTable.Field.DESC.key(), podcast.getDescription());
        }
        values.put(PodcastsTable.Field.LENGTH.key(), podcast.getLength());
        values.put(PodcastsTable.Field.RATING.key(), podcast.getFavorite());
        values.put(PodcastsTable.Field.ORD.key(), ord);

        try (Cursor cursor = db.rawQuery(PodcastsTable.SELECT_SQL + WHERE + PodcastsTable.Field.ID.key() + " = ?", args(id))) {
            if (!cursor.moveToFirst()) {
                if (seenIfNew || podcast.getSeen() > 0) {
                    values.put(PodcastsTable.Field.SEEN.key(), seenIfNew ? podcast.getLength() : podcast.getSeen());
                }
                if (podcast.getIcon() != null) {
                    values.put(PodcastsTable.Field.ICON_URL.key(), podcast.getIcon().getUrl());
                    values.put(PodcastsTable.Field.ICON_RGB.key(), podcast.getIcon().getPrimaryColor());
                    values.put(PodcastsTable.Field.ICON_RGB2.key(), podcast.getIcon().getSecondaryColor());
                }
                if (podcast.getSplash() != null) {
                    values.put(PodcastsTable.Field.SPLASH_URL.key(), podcast.getSplash().getUrl());
                    values.put(PodcastsTable.Field.SPLASH_RGB.key(), podcast.getSplash().getPrimaryColor());
                    values.put(PodcastsTable.Field.SPLASH_RGB2.key(), podcast.getSplash().getSecondaryColor());
                }
                values.put(PodcastsTable.Field.ID.key(), id);
                db.insertWithOnConflict(PodcastsTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            } else {
                if (podcast.getSeen() > cursor.getLong(PodcastsTable.Field.SEEN.ordinal())) {
                    values.put(PodcastsTable.Field.SEEN.key(), podcast.getSeen());
                }
                if (podcast.getIcon() != null) {
                    if (!podcast.getIcon().getUrl().equalsIgnoreCase(cursor.getString(PodcastsTable.Field.ICON_URL.ordinal()))) {
                        values.put(PodcastsTable.Field.ICON_URL.key(), podcast.getIcon().getUrl());
                        values.put(PodcastsTable.Field.ICON_RGB.key(), podcast.getIcon().getPrimaryColor());
                        values.put(PodcastsTable.Field.ICON_RGB2.key(), podcast.getIcon().getSecondaryColor());
                    } else if (podcast.getIcon().hasColor()) {
                        values.put(PodcastsTable.Field.ICON_RGB.key(), podcast.getIcon().getPrimaryColor());
                        values.put(PodcastsTable.Field.ICON_RGB2.key(), podcast.getIcon().getSecondaryColor());
                    }
                }
                if (podcast.getSplash() != null) {
                    if (!podcast.getSplash().getUrl().equalsIgnoreCase(cursor.getString(PodcastsTable.Field.SPLASH_URL.ordinal()))) {
                        values.put(PodcastsTable.Field.SPLASH_URL.key(), podcast.getSplash().getUrl());
                        values.put(PodcastsTable.Field.SPLASH_RGB.key(), podcast.getSplash().getPrimaryColor());
                        values.put(PodcastsTable.Field.SPLASH_RGB2.key(), podcast.getSplash().getSecondaryColor());
                    } else if (podcast.getSplash().hasColor()) {
                        values.put(PodcastsTable.Field.ICON_RGB.key(), podcast.getSplash().getPrimaryColor());
                        values.put(PodcastsTable.Field.ICON_RGB2.key(), podcast.getSplash().getSecondaryColor());
                    }
                }
                db.update(PodcastsTable.NAME, values, PodcastsTable.Field.ID.key() + " = ?", args(id));
            }
        }
    }

    public void storePodcastSeen(long podcast, int seen) {
        ContentValues values = new ContentValues();
        values.put(PodcastsTable.Field.SEEN.key(), seen);
        db.update(PodcastsTable.NAME, values, PodcastsTable.Field.ID + " = ?", args(podcast));
    }

    public void storePodcastIcon(long podcast, String url, int primaryColor, int secondaryColor) {
        ContentValues values = new ContentValues();
        values.put(PodcastsTable.Field.ICON_URL.key(), url);
        values.put(PodcastsTable.Field.ICON_RGB.key(), primaryColor);
        values.put(PodcastsTable.Field.ICON_RGB2.key(), secondaryColor);
        db.update(PodcastsTable.NAME, values, PodcastsTable.Field.ID + " = ?", args(podcast));
    }

    public void storePodcastSplash(long podcast, String url, int primaryColor, int secondaryColor) {
        ContentValues values = new ContentValues();
        values.put(PodcastsTable.Field.SPLASH_URL.key(), url);
        values.put(PodcastsTable.Field.SPLASH_RGB.key(), primaryColor);
        values.put(PodcastsTable.Field.SPLASH_RGB2.key(), secondaryColor);
        db.update(PodcastsTable.NAME, values, PodcastsTable.Field.ID + " = ?", args(podcast));
    }

    public void storePodcastRating(long podcast, int rating) {
        ContentValues values = new ContentValues();
        values.put(PodcastsTable.Field.RATING.key(), rating);
        db.update(PodcastsTable.NAME, values, PodcastsTable.Field.ID + " = ?", args(podcast));
    }

    public void storeRecordPosition(long podcast, long record, int position) {
        ContentValues values = new ContentValues();
        values.put(PlayersTable.Field.PODCAST_ID.toString(), podcast);
        values.put(PlayersTable.Field.RECORD_ID.toString(), record);
        values.put(PlayersTable.Field.POSITION.toString(), position);
        db.insertWithOnConflict(PlayersTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void storeRecord(long podcast, Record record) {
        ContentValues values = new ContentValues();
        values.put(RecordsTable.Field.PODCAST_ID.toString(), podcast);
        values.put(RecordsTable.Field.ID.toString(), record.getId());
        values.put(RecordsTable.Field.NAME.toString(), record.getName());
        values.put(RecordsTable.Field.URL.toString(), record.getUrl());
        values.put(RecordsTable.Field.DESC.toString(), record.getDescription());
        values.put(RecordsTable.Field.DATE.toString(), record.getDate());
        values.put(RecordsTable.Field.DURATION.toString(), record.getDuration());
        db.insertWithOnConflict(RecordsTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void create() {
        db.execSQL(PodcastsTable.CREATE_SQL);
        db.execSQL(PodcastsTable.CREATE_RATING_ORD_INDEX_SQL);
        db.execSQL(RecordsTable.CREATE_SQL);
        db.execSQL(PlayersTable.CREATE_SQL);
    }

    public void upgrade(int oldVersion, int newVersion) {
        if (oldVersion <= 1) {
            db.execSQL("ALTER TABLE " + PodcastsTable.NAME + " ADD COLUMN " + PodcastsTable.Field.SEEN.key() + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE " + PodcastsTable.NAME + " SET " + PodcastsTable.Field.SEEN.key() + " = " + PodcastsTable.Field.LENGTH.key());
        }
        if (oldVersion <= 2) {
            db.execSQL("UPDATE " + PodcastsTable.NAME + " SET " + PodcastsTable.Field.SEEN.key() + " = " + PodcastsTable.Field.LENGTH.key()
                    + " WHERE " + PodcastsTable.Field.SEEN.key() + " > " + PodcastsTable.Field.LENGTH.key());
        }
        if (oldVersion <= 3) {
            db.execSQL(PlayersTable.CREATE_SQL);
            db.execSQL("INSERT INTO " + PlayersTable.NAME
                    + "(" + PlayersTable.Field.PODCAST_ID + ", " + PlayersTable.Field.RECORD_ID + ", " + PlayersTable.Field.POSITION + ")"
                    + " SELECT " + RecordsTable.Field.PODCAST_ID + ", " + RecordsTable.Field.ID + ", 0 FROM " + RecordsTable.NAME + " WHERE played = 1");
            db.execSQL("DROP TABLE " + RecordsTable.NAME);
            db.execSQL(RecordsTable.CREATE_SQL);
            db.execSQL("ALTER TABLE " + PodcastsTable.NAME + " RENAME TO old_podcasts");
            db.execSQL("CREATE TABLE " + PodcastsTable.NAME + " (" + fields(PODCASTS_FIELD_V1_4) + COMMA
                    + "PRIMARY KEY (" +  PodcastsTable.Field.ID.key() + "))");
            String columns = join(PODCASTS_FIELD_V1_4);
            db.execSQL("INSERT INTO " + PodcastsTable.NAME + "(" + columns + ") SELECT " + columns + " FROM old_podcasts");
            db.execSQL("DROP TABLE old_podcasts");
        }
        if (oldVersion <= 4) {
            db.execSQL("ALTER TABLE " + PodcastsTable.NAME + " ADD COLUMN " + PodcastsTable.Field.RATING.key() + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("DROP INDEX IF EXISTS idx_podcasts__ord");
            db.execSQL(PodcastsTable.CREATE_RATING_ORD_INDEX_SQL);
        }
    }
}
