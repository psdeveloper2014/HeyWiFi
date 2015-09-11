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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class FindingPhoneActivityS02 extends AppCompatActivity {

    String mac;
    int message;

    SharedPrefSettings pref;

    EditText ssid_et;
    Button close_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findingphones02);

        Intent getIntent = getIntent();
        mac = getIntent.getStringExtra("mac");
        message = getIntent.getIntExtra("message", 3);

        ssid_et = (EditText) findViewById(R.id.finding_ssid_et);
        close_btn = (Button) findViewById(R.id.close_btn);

        pref = new SharedPrefSettings(this);

        // heywifi_3aaaaaaaaaaaaaaaaaaa (28 character)
        // 0123456789                 27
        String ssid = "heywifi_" + message + hashPasscode();

        if (!ssid.isEmpty()) {
            ssid_et.setText(ssid);
        }

        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(-1);
                finish();
            }
        });
    }

    private String hashPasscode() {
        String passcode;

        try {
            Date d = new Date();
            String date = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(d);
            String str = mac + date + "hailey";

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i=0; i<byteData.length; i++) {
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            }

            passcode = sb.toString().substring(0, 18);
        } catch (NoSuchAlgorithmException e) {
            passcode = "";
        }

        return passcode;
    }
}
