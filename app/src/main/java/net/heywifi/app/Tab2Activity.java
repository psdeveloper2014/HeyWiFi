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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;


public class Tab2Activity extends Fragment {

    Context context;
    SharedPrefSettings pref;

    Button regist_btn;
    TextView text_tv;

    GoogleCloudMessaging gcm;
    String regid;
    String senderid = "648637692734";

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_tab2, container, false);
        context = v.getContext();
        pref = new SharedPrefSettings(context);

        regist_btn = (Button) v.findViewById(R.id.regist_btn);
        text_tv = (TextView) v.findViewById(R.id.text_tv);

        regist_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gcm = GoogleCloudMessaging.getInstance(context);
                regid = getRegistrationId();

                if (regid.isEmpty()) {
                    registerInBackground();
                }
            }
        });

        return v;
    }

    private String getRegistrationId() {
        if (pref.getRegId().isEmpty()) {
            return "";
        }

        int regVersion = pref.getRegVersion();
        int currentVersion = getAppVersion();
        if (regVersion != currentVersion) {
            return "";
        }

        return pref.getRegId();
    }

    private int getAppVersion() {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name");
        }
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void ... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(senderid);
                    msg = "Device registered, registration ID = '" + regid + "'";

                    sendRegistrationIdToServer();
                    pref.putRegId(regid);
                } catch (IOException e) {
                    msg = "Error : " + e.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                text_tv.setText(msg);
            }

            private void sendRegistrationIdToServer() {
                // TODO : register regid in server
            }
        }.execute();
    }
}

