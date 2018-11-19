package com.lapism.searchview.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

// TODO ROOM
class SearchHistoryDatabase extends SQLiteOpenHelper {

    static final String SEARCH_HISTORY_TABLE = "search_history";
    static final String SEARCH_HISTORY_COLUMN_TITLE = "_title";
    static final String SEARCH_HISTORY_COLUMN_SUBTITLE = "_subtitle";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "searchHistoryDatabase.db";
    private static final String CREATE_TABLE_SEARCH_HISTORY = "CREATE TABLE IF NOT EXISTS "
            + SEARCH_HISTORY_TABLE + " ( "
            //+ SEARCH_HISTORY_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + SEARCH_HISTORY_COLUMN_TITLE + " TEXT, "
            + SEARCH_HISTORY_COLUMN_SUBTITLE + " TEXT " + ");";

    SearchHistoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SEARCH_HISTORY);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        dropAllTables(db);
        onCreate(db);
    }

    private void dropAllTables(@NonNull SQLiteDatabase db) {
        dropTableIfExists(db);
    }

    private void dropTableIfExists(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + SEARCH_HISTORY_TABLE);
    }

}


