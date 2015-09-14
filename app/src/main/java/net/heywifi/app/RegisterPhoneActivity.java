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

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class RegisterPhoneActivity extends AppCompatActivity {

    SharedPrefSettings pref;

    TextView phone_name_err_tv, phone_mac_tv;
    EditText phone_name_et;
    Button register_btn;
    LinearLayout wait_ly;

    LoadingDialog dialog;

    int type;
    String id, mac, nick, gcmid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerphone);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        phone_name_err_tv = (TextView) findViewById(R.id.phone_name_err_tv);
        phone_name_et = (EditText) findViewById(R.id.phone_name_et);
        phone_mac_tv = (TextView) findViewById(R.id.phone_mac_tv);
        register_btn = (Button) findViewById(R.id.register_btn);
        wait_ly = (LinearLayout) findViewById(R.id.wait_ly);

        pref = new SharedPrefSettings(this);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        mac = wi.getMacAddress().toUpperCase();
        phone_mac_tv.setText(mac);

        getUserInfo();
        new CheckRegisteredPhoneNumberTask().execute();

        new RegisterGcmTask().execute();

        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void getUserInfo() {
        type = pref.getUserType();
        id = pref.getUserId();
    }

    private class CheckRegisteredPhoneNumberTask extends AsyncTask<Void, Void, Integer> {

        int pos;
        String response;

        protected void onPreExecute() {
            dialog = new LoadingDialog(RegisterPhoneActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void... params) {
            getPos();
            decodeJson();

            return 0;
        }

        protected void onPostExecute(Integer result) {
            dialog.dismiss();

            if (pos >= 5) {
                setResult(4);
                finish();
            }
        }

        private void getPos() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/phone/getpos.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + type));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void decodeJson() {
            try {
                JSONObject json = new JSONObject(response);
                pos = json.getInt("pos");
            } catch (JSONException e) {
                pos = 5;
            }
        }
    }

    private void register() {
        if (isConnected()) {
            if (checkValid()) {
                new RegisterPhoneTask().execute();
            }
        } else {
            Toast.makeText(this, R.string.register_internet, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mobile.isConnected() || wifi.isConnected();
    }

    private boolean checkValid() {
        nick = phone_name_et.getText().toString();

        if (nick.isEmpty()) {
            phone_name_err_tv.setText(getResources().getString(R.string.register_err_empty));
            phone_name_err_tv.setVisibility(View.VISIBLE);
            return false;
        } else if (nick.length() > 30) {
            phone_name_err_tv.setText(R.string.register_err_toolong);
            phone_name_err_tv.setVisibility(View.VISIBLE);
            return false;
        } else {
            phone_name_err_tv.setVisibility(View.INVISIBLE);
            return true;
        }
    }

    private class RegisterGcmTask extends AsyncTask<Void, Void, Integer> {

        String senderId = "648637692734";
        GoogleCloudMessaging gcm;

        protected Integer doInBackground(Void ... params) {
            int result = 1;
            Context context = getApplicationContext();

            if (isConnected()) {
                gcm = GoogleCloudMessaging.getInstance(context);
                gcmid = getRegistrationId(context);

                if (gcmid.isEmpty()) {
                    result = register();
                }
            }

            return result;
        }

        protected void onPostExecute(Integer result) {
            if (result == 0) {
                CantRegisterDialog cdialog = new CantRegisterDialog(RegisterPhoneActivity.this, dialog);
                cdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                cdialog.show();
                wait_ly.setVisibility(View.INVISIBLE);
                register_btn.setEnabled(false);
            } else {
                wait_ly.setVisibility(View.INVISIBLE);
                register_btn.setEnabled(true);
            }
        }

        private boolean isConnected() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            return mobile.isConnected() || wifi.isConnected();
        }

        private String getRegistrationId(Context context) {
            String registrationId = pref.getRegId();
            if (registrationId.isEmpty()) {
                return "";
            }

            int registeredVersion = pref.getRegVersion();
            int currentVersion = getAppVersion(context);
            if (registeredVersion != currentVersion) {
                return "";
            }

            return registrationId;
        }

        private int getAppVersion(Context context) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return pi.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private int register() {
            int result;

            try {
                gcmid = gcm.register(senderId);
                pref.putRegId(gcmid);
                pref.putRegVersion(getAppVersion(getApplicationContext()));
                result = 1;
            } catch (IOException e) {
                result = 0;
            } catch (NullPointerException e) {
                // Throws Exception because of not installed Google Play Store
                result = 0;
            }

            return result;
        }
    }

    private class RegisterPhoneTask extends AsyncTask<Void, Void, Integer> {

        String response;

        protected void onPreExecute() {
            dialog = new LoadingDialog(RegisterPhoneActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void ... params) {
            int status;

            connectGetResponse();
            status = decodeJson();

            if (status == 1) {
                pref.putPhoneInfo(mac, nick);
            }

            return status;
        }

        protected void onPostExecute(Integer status) {
            dialog.dismiss();

            switch (status) {
                case 1:
                    setResult(2);
                    finish();
                    break;
                case 11:
                    phone_name_err_tv.setVisibility(View.VISIBLE);
                    phone_name_err_tv.setText(R.string.register_err_already);
                    break;
                case 12:
                    setResult(4);
                    finish();
                    break;
                case 13:
                    phone_name_err_tv.setVisibility(View.VISIBLE);
                    phone_name_err_tv.setText(R.string.register_err_byother);
                    register_btn.setEnabled(false);
                    break;
            }
        }

        private void connectGetResponse() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://master.heywifi.net/query/phone/registerphone.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + type));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                nameValuePairs.add(new BasicNameValuePair("mac", mac));
                nameValuePairs.add(new BasicNameValuePair("nick", URLEncoder.encode(nick, "utf-8")));
                nameValuePairs.add(new BasicNameValuePair("gcmid", gcmid));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int decodeJson() {
            int status = 0;

            try {
                JSONObject json = new JSONObject(response);
                status = json.getInt("status");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return status;
        }
    }
}

class CantRegisterDialog extends Dialog {

    public CantRegisterDialog(Context context, LoadingDialog dialog) {
        super(context);
        // Not to duplicate dialogs
        dialog.dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_warning);

        TextView dialog_title = (TextView) findViewById(R.id.dialog_title);
        TextView dialog_text = (TextView) findViewById(R.id.dialog_text);
        Button closebtn = (Button) findViewById(R.id.dialog_closebtn);

        dialog_title.setText(R.string.register_dialog_title);
        dialog_text.setText(R.string.register_dialog_text);

        closebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}