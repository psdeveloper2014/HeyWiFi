package net.heywifi.app;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Andy on 2015-08-01.
 */
public class MyGcmListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String TAG = "MyGcmListenerService";

        String title = data.getString("title");
        String text = data.getString("text");

        Log.d(TAG, "From: " + from);    // Sender ID
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Text: " + text);
    }

}
