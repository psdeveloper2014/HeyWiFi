package net.heywifi.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class PackageReceiver extends BroadcastReceiver {

    SharedPrefSettings pref;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        pref = new SharedPrefSettings(context);

        Intent gcm = new Intent(context, RegisterGcmService.class);

        switch (action) {
            case Intent.ACTION_PACKAGE_ADDED:
                context.startService(gcm);
                break;
            case Intent.ACTION_PACKAGE_REPLACED:
                context.startService(gcm);
                break;
        }
    }
}
