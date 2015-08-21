package net.heywifi.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;


public class MainAlarmManager {

    Context context;
    static int REQUEST_CODE = 12345;

    public MainAlarmManager(Context context) {
        this.context = context;
    }

    public void set() {
        Log.i("service", "alarm registered");
        Intent intent = new Intent(context, MainAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+10000, 300000, pi);
    }
}
