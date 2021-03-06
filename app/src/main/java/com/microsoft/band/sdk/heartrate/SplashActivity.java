package com.microsoft.band.sdk.heartrate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class SplashActivity extends Activity {
    private PendingIntent pi;
    private AlarmManager am;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final Context c = getApplicationContext();
        final AlarmManager m = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

        AlarmManager.AlarmClockInfo alarmClockInfo = m.getNextAlarmClock();
        Log.d(Long.toString(System.currentTimeMillis()), Long.toString(alarmClockInfo.getTriggerTime()));
        setup();
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 9000, pi);
    }

    private void setup() {
        BroadcastReceiver br;
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                Intent intent = new Intent(getApplicationContext(), BandHeartRateAppActivity.class);
                startActivity(intent);
            }
        };
        registerReceiver(br, new IntentFilter("com.microsoft.band.sdk.heartrate.HealthyWake"));
        pi = PendingIntent.getBroadcast( this, 0, new Intent("com.microsoft.band.sdk.heartrate.HealthyWake"),
                0 );
        am = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }
}
