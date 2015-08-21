package net.heywifi.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class MainAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, MainService.class);
        context.startService(service);
    }
}
