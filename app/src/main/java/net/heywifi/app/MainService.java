/*
 * Copyright 2015 Park Si Hyeon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.heywifi.app;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainService extends Service {

    SharedPrefSettings pref;
    WifiManager wm;

    Date d;
    String date;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("service", "onStartCommand");
        run();
        return START_NOT_STICKY;
    }

    public void run() {
        Log.i("service", "started");

        pref = new SharedPrefSettings(this);

        // Finding Phone Step 2
        wm = (WifiManager) getSystemService(WIFI_SERVICE);
        wm.startScan();
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {}

        d = new Date();
        date = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(d);

        if (!date.equals(pref.getHashedDate())) {
            hashPasscode();
        }
        String hash = pref.getTodayHash();

        // heywifi_3aaaaaaaaaaaaaaaaaaa (28 character)
        // 0123456789                 27
        List<ScanResult> scanResults = wm.getScanResults();
        for (ScanResult result : scanResults) {
            try {
                String passcode = result.SSID.substring(9, 27);
                if (passcode.equals(hash)) {
                    boolean ring, vibrate;
                    switch (result.SSID.charAt(8)) {
                        case '3':
                            ring = true;
                            vibrate = true;
                            break;
                        case '2':
                            ring = true;
                            vibrate = false;
                            break;
                        case '1':
                            ring = false;
                            vibrate = true;
                            break;
                        case '0':
                            ring = false;
                            vibrate = false;
                            break;
                        default:
                            ring = true;
                            vibrate = true;
                            break;
                    }
                    Intent mIntent = new Intent(this, FoundActivity.class);
                    mIntent.putExtra("ring", ring);
                    mIntent.putExtra("vibrate", vibrate);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mIntent);
                }
            } catch (StringIndexOutOfBoundsException e) {}
        }
    }

    private void hashPasscode() {
        try {
            WifiInfo wi = wm.getConnectionInfo();
            String mac = wi.getMacAddress().toUpperCase();

            String str = mac + date + "hailey";

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i=0; i<byteData.length; i++) {
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            }

            pref.putTodayHash(sb.toString().substring(0, 18));
            pref.putHashedDate(date);
        } catch (NoSuchAlgorithmException e) {}
    }
}
