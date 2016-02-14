package com.microsoft.band.sdk.heartrate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class HighscoreActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highscore);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        List<DataPoint> dataPoints = new ArrayList<>();

        HighscoreDbHelper mDbHelper = new HighscoreDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                HighscoreContract.HighscoreEntry._ID,
                HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX,
                HighscoreContract.HighscoreEntry.COLUMN_NAME_SCORE
        };

        Cursor cursor = db.query(
                HighscoreContract.HighscoreEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX, // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX + " DESC"
        );

        int columnsToGet = Math.min(cursor.getCount(), 7);

        for (int i = 0; i < columnsToGet; i++) {
            cursor.moveToNext();
            double highscore = cursor.getDouble(cursor.getColumnIndex(
                    HighscoreContract.HighscoreEntry.COLUMN_NAME_SCORE));
            dataPoints.add(new DataPoint(i, highscore));
        }

        cursor.close();

        DataPoint[] points = dataPoints.toArray(new DataPoint[dataPoints.size()]);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points);
        graph.addSeries(series);

        addHighscore(getApplicationContext(), highestColumnId(getApplicationContext()));
        Log.d("hi", Integer.toString(highestColumnId(getApplicationContext())));

    }
    protected int highestColumnId(Context c) {
        HighscoreDbHelper mDbHelper = new HighscoreDbHelper(c);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                HighscoreContract.HighscoreEntry._ID,
                HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX,
        };

        Cursor cursor = db.query(
                HighscoreContract.HighscoreEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX, // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null
        );

        int highestColumnId = cursor.getCount();
        cursor.close();
        Log.d("Database", Integer.toString(highestColumnId));
        return highestColumnId;
    }

    protected void addHighscore(Context c, double highscore) {
        //c.deleteDatabase(HighscoreDbHelper.DATABASE_NAME);

        HighscoreDbHelper mDbHelper = new HighscoreDbHelper(c);

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(HighscoreContract.HighscoreEntry.COLUMN_NAME_ENTRY_INDEX, highestColumnId(c));
        values.put(HighscoreContract.HighscoreEntry.COLUMN_NAME_SCORE, highscore);

        // Insert the new row, returning the primary key value of the new row
        db.insert(
                HighscoreContract.HighscoreEntry.TABLE_NAME,
                null,
                values);
    }

}
