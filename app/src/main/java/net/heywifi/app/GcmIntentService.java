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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;


public class GcmIntentService extends IntentService {

    boolean ring, vibrate;
    int reqdate;
    int[] date = new int[2];

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        try {
            if (!extras.isEmpty()) {
                if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    decodeMessage(extras.getString("message"));
                    getDate();

                    if (reqdate == date[0] || reqdate == date[1]) {
                        Intent mIntent = new Intent(GcmIntentService.this, FoundActivity.class);
                        mIntent.putExtra("ring", ring);
                        mIntent.putExtra("vibrate", vibrate);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mIntent);
                    }
                }
            }
        } catch (Exception e) {}

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void decodeMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            // 2:ring, 1: vibrate / mode = ring + vibrate
            int mode = json.getInt("mode");
            switch (mode) {
                case 3:
                    ring = true;
                    vibrate = true;
                    break;
                case 2:
                    ring = true;
                    vibrate = false;
                    break;
                case 1:
                    ring = false;
                    vibrate = true;
                    break;
                case 0:
                    ring = false;
                    vibrate = false;
                    break;
            }
            // Requested date
            reqdate = json.getInt("date");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getDate() {
        String response = "";
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/phone/getdate.php");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            response = httpResponse.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        decodeJson(response);
    }

    private void decodeJson(String response) {
        try {
            JSONObject json = new JSONObject(response);
            date[0] = json.getInt("date1");
            date[1] = json.getInt("date2");
        } catch (JSONException e) {
            date[0] = 0;
            date[1] = 0;
        }
    }
}
