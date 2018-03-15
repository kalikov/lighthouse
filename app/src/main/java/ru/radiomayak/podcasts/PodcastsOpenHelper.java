package ru.radiomayak.podcasts;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PodcastsOpenHelper extends SQLiteOpenHelper {
    public PodcastsOpenHelper(Context context) {
        this(context, PodcastsReadableDatabase.DATABASE_NAME);
    }

    PodcastsOpenHelper(Context context, String name) {
        super(context, name, null, PodcastsReadableDatabase.VERSION);
    }

    PodcastsOpenHelper(Context context, String name, DatabaseErrorHandler errorHandler) {
        super(context, name, null, PodcastsReadableDatabase.VERSION, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        PodcastsWritableDatabase database = new PodcastsWritableDatabase(db);
        database.create();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        PodcastsWritableDatabase database = new PodcastsWritableDatabase(db);
        database.upgrade(oldVersion, newVersion);
    }
}
