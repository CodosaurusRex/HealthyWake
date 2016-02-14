package com.microsoft.band.sdk.heartrate;

import android.provider.BaseColumns;

/**
 * Created by williezhu on 2/13/16.
 */
public final class HighscoreContract {

    // give it an empty constructor.
    public HighscoreContract() {}

    /* Inner class that defines the table contents */
    public static abstract class HighscoreEntry implements BaseColumns {
        public static final String TABLE_NAME = "highscore";
        public static final String COLUMN_NAME_ENTRY_INDEX = "entryid";
        public static final String COLUMN_NAME_SCORE = "score";
    }
}
