package ru.radiomayak.podcasts;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class PodcastsWritableDatabase extends PodcastsReadableDatabase {
    public static PodcastsWritableDatabase get(PodcastsOpenHelper helper) {
        return new PodcastsWritableDatabase(helper.getWritableDatabase());
    }

    public PodcastsWritableDatabase(SQLiteDatabase db) {
        super(db);
    }

    public void storeRecordPosition(long podcast, long record, int position) {
        ContentValues values = new ContentValues();
        values.put(Players.Fields.PODCAST_ID.toString(), podcast);
        values.put(Players.Fields.RECORD_ID.toString(), record);
        values.put(Players.Fields.POSITION.toString(), position);
        db.insertWithOnConflict(Players.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void storeRecordFile(long podcast, long record, int size, int capacity) {
        ContentValues values = new ContentValues();
        values.put(Files.Fields.PODCAST_ID.toString(), podcast);
        values.put(Files.Fields.RECORD_ID.toString(), record);
        values.put(Files.Fields.SIZE.toString(), size);
        values.put(Files.Fields.CAPACITY.toString(), capacity);
        db.insertWithOnConflict(Files.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void storeRecord(long podcast, Record record) {
        ContentValues values = new ContentValues();
        values.put(RecordsT.Fields.PODCAST_ID.toString(), podcast);
        values.put(RecordsT.Fields.ID.toString(), record.getId());
        values.put(RecordsT.Fields.NAME.toString(), record.getName());
        values.put(RecordsT.Fields.URL.toString(), record.getUrl());
        values.put(RecordsT.Fields.DESC.toString(), record.getDescription());
        values.put(RecordsT.Fields.DATE.toString(), record.getDate());
        values.put(RecordsT.Fields.DURATION.toString(), record.getDuration());
        db.insertWithOnConflict(RecordsT.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
