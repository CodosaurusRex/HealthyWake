package com.microsoft.band.sdk.heartrate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by williezhu on 2/13/16.
 */


public class HighscoreDbHelper extends SQLiteOpenHelper {
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + HighscoreContract.HighscoreEntry.TABLE_NAME + " (" +
                    HighscoreContract.HighscoreEntry._ID + " INTEGER PRIMARY KEY," +
                    HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX + INTEGER_TYPE + COMMA_SEP +
                    HighscoreContract.HighscoreEntry.COLUMN_NAME_SCORE + REAL_TYPE + ")";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + HighscoreContract.HighscoreEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Highscore.db";

    public HighscoreDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}


