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

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class IntroActivity extends AppCompatActivity {

    public static IntroActivity intro;
    SharedPrefSettings pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        intro = IntroActivity.this;
        pref = new SharedPrefSettings(this);

        if (pref.isFirstLaunch()) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivityForResult(intent, 0);
        } else {
            if (!pref.isUserLogined()) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivityForResult(intent, 0);
            } else if (isConnected()) {
                // Check saved info is correct
                new CheckAccountTask().execute();
                // Starting activity is included in CheckAccountTask()
            } else {
                // Unavailable to check account
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mobile.isConnected() || wifi.isConnected();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultcode, Intent data) {
        switch (resultcode) {
            // Success Login
            case 1:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            // Canceled
            default:
                finish();
                break;
        }
    }

    private class CheckAccountTask extends AsyncTask<Void, Void, Integer> {

        String response;

        protected Integer doInBackground(Void... params) {
            checkAccount();
            int status = decodeJson();

            return status;
        }

        protected void onPostExecute(Integer status) {
            if (status == 1) {
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
                startActivityForResult(intent, 0);
            }
        }

        private void checkAccount() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/login/checkaccount.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + pref.getUserType()));
                nameValuePairs.add(new BasicNameValuePair("id", pref.getUserId()));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int decodeJson() {
            int status;

            try {
                JSONObject json = new JSONObject(response);
                status = json.getInt("status");
            } catch (JSONException e) {
                status = 0;
            }

            return status;
        }
    }
}
