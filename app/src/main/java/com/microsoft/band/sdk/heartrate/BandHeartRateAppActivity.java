//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.heartrate;

import java.lang.ref.WeakReference;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.Window;

public class BandHeartRateAppActivity extends Activity {

	private BandClient client = null;
	private Button btnConsent;
	private TextView txtStatus;
    private ImageView heart;
    private TextView alarmStatus;
	private boolean alarmOn = true;
    private MediaPlayer mp;

    private TextView timerValue;

    private long startTime = 0L;

    private Handler customHandler = new Handler();

    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    int currRate;
    int init = 75;
	boolean active = false;
    boolean initialized = false;

	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
            	appendToUI(String.format("Heart Rate = %d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality()));

                if (event.getQuality().toString().equals("LOCKED") && initialized != true){
                    active = true;
                    initialized = true;
                    init = event.getHeartRate();
                }
                currRate = init - event.getHeartRate();
            	if(currRate > 10 && active){
					alarmOn = false;
				}


                dispHeart(currRate);
				if (alarmOn){

				}
				else {
                    mp.stop();
				}

			}
        }
    };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        heart= (ImageView)findViewById(R.id.heart);
        timerValue = (TextView) findViewById(R.id.timer);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        //alarmStatus = (TextView) findViewById(R.id.alarmStatus);
        txtStatus.setText("");
        new HeartRateSubscriptionTask().execute();
        
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        Typeface tf = Typeface.createFromAsset(getAssets(),
                "fonts/Reg.ttf");
        txtStatus.setTypeface(tf);
        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnConsent.setOnClickListener(new OnClickListener() {
			@SuppressWarnings("unchecked")
            @Override
			public void onClick(View v) {
				new HeartRateConsentTask().execute(reference);
			}
		});


        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);

        addHighscore(getApplicationContext(), 10);

        play(this, getAlarmSound());
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
        c.deleteDatabase(HighscoreDbHelper.DATABASE_NAME);

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
	
	@Override
	protected void onResume() {
		super.onResume();
		txtStatus.setText("");
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		if (client != null) {
			try {
				client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
			} catch (BandIOException e) {
				appendToUI(e.getMessage());
			}
		}
	}
	
    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }
    
	private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
						client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
					} else {
						appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}
	
	private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
		@Override
		protected Void doInBackground(WeakReference<Activity>... params) {
			try {
				if (getConnectedBandClient()) {
					
					if (params[0].get() != null) {
						client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
							@Override
							public void userAccepted(boolean consentGiven) {
							}
					    });
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}
	
	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtStatus.setText(string);
            }
        });
	}
    
	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}


    private void play(Context context, Uri alert) {
        mp = MediaPlayer.create(getApplicationContext(), R.raw.tonedef);

        mp.setLooping(true);
        mp.start();
    }

    private Uri getAlarmSound() {
        Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alertSound == null) {
            alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alertSound == null) {
                alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        return alertSound;
    }



    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timerValue.setText("" + mins + ":"
                    + String.format("%02d", secs) + ":"
                    + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }

    };



    private void dispHeart(final int hrt) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //replace
                // extension removed from the String
                String uri = "@drawable/heart_0";
                if (hrt == 0){
                    uri = "@drawable/heart_0";  // where myresource.png is the file

                }
                else if (hrt < 3){
                    uri = "@drawable/heart_1";  // where myresource.png is the file

                }
                else if (hrt < 4) {
                    uri = "@drawable/heart_2";  // where myresource.png is the file
                }
                else if (hrt < 5){
                    uri = "@drawable/heart_3";  // where myresource.png is the file

                }
                else if (hrt < 7){
                    uri = "@drawable/heart_4";

                }
                else if (hrt < 9){
                    uri = "@drawable/heart_5";
                }
                else{
                    uri = "@drawable/heart_full";
                }
                int imageResource = getResources().getIdentifier(uri, null, getPackageName());
                Drawable res = getResources().getDrawable(imageResource);
                heart.setImageDrawable(res);
            }
        });
    }

}

