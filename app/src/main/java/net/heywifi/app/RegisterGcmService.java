package net.heywifi.app;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;


public class RegisterGcmService extends Service implements Runnable {

    SharedPrefSettings pref;

    GoogleCloudMessaging gcm;
    String senderId = "648637692734";
    String regid;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        pref = new SharedPrefSettings(this);
        gcm = GoogleCloudMessaging.getInstance(this);

        return START_STICKY;
    }

    @Override
    public void run() {
        while (true) {
            if (isConnected()) {
                regid = getRegistrationId();

                if (regid.isEmpty()) {
                    register();
                } else {
                    break;
                }
            }

            // Try again after 3 min
            try {
                Thread.sleep(180000);
            } catch (InterruptedException e) {}
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mobile.isConnected() || wifi.isConnected();
    }

    private String getRegistrationId() {
        String registrationId = pref.getRegId();
        if (registrationId.isEmpty()) {
            return "";
        }

        int registeredVersion = pref.getRegVersion();
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            return "";
        }

        return registrationId;
    }

    private int getAppVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void register() {
        try {
            regid = gcm.register(senderId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pref.putRegId(regid);
        pref.putRegVersion(getAppVersion());
    }
}
