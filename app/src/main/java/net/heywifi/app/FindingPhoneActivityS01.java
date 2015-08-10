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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.CircleProgress;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class FindingPhoneActivityS01 extends AppCompatActivity {

    String gcmid;
    // 0:false, 1:true
    int ring, vibrate;

    CircleProgress progress;
    int percent;

    TextView text01, text02;
    Button found_btn, giveup_btn, nextstep_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findingphones01);

        Intent getIntent = getIntent();
        gcmid = getIntent.getStringExtra("gcmid");
        ring = getIntent.getIntExtra("ring", 1);
        vibrate = getIntent.getIntExtra("vibrate", 1);

        progress = (CircleProgress) findViewById(R.id.circle_progress);
        text01 = (TextView) findViewById(R.id.finding_text01);
        text02 = (TextView) findViewById(R.id.finding_text02);
        found_btn = (Button) findViewById(R.id.found_btn);
        giveup_btn = (Button) findViewById(R.id.giveup_btn);
        nextstep_btn = (Button) findViewById(R.id.nextstep_btn);

        found_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: finish activity
                // result = 1 (show success dialog on MainActivity)
            }
        });

        giveup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: finish activity
                // result = 0 (just close all windows)
            }
        });

        nextstep_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(FindingPhoneActivityS01.this, FindingPhoneActivityS02.class);
//                intent.putExtra("gcmid", gcmid);
//                startActivityForResult(intent);
            }
        });

        new RequestGcmTask().execute();

        TimeCountThread tct = new TimeCountThread();
        tct.start();
    }

    private class TimeCountThread extends Thread {

        Handler timehandler;

        public TimeCountThread() {
            timehandler = new TimeCountHandler();
        }

        public void run() {
            percent = 1;
            while (percent <= 100) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {}

                timehandler.sendEmptyMessage(percent);
                percent++;
            }
        }
    }

    private class TimeCountHandler extends Handler {

        public TimeCountHandler() {}

        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            progress.setProgress(msg.what);

            if (msg.what == 100) {
                giveup_btn.setEnabled(true);
                nextstep_btn.setEnabled(true);
                text01.setText(getResources().getString(R.string.finding01_found_ask));
                text02.setVisibility(View.INVISIBLE);
            }
        }
    }

    private class RequestGcmTask extends AsyncTask<Void, Void, Integer> {

        protected Integer doInBackground(Void ... params) {
            requestGcm();
            return 0;
        }

        private void requestGcm() {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(getResources().openRawResource(R.raw.comodo_rsaca));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }

                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);

                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("gcmid", gcmid));
                nameValuePairs.add(new BasicNameValuePair("ring", ""+ring));
                nameValuePairs.add(new BasicNameValuePair("vibrate", ""+vibrate));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);

                String u = "https://www.heywifi.net/db/phone/requestgcm.php";

                URL url = new URL(u);
                HttpsURLConnection request = (HttpsURLConnection) url.openConnection();

                request.setSSLSocketFactory(sslContext.getSocketFactory());
                request.setUseCaches(false);
                request.setDoInput(true);
                request.setDoOutput(true);
                request.setRequestMethod("POST");
                OutputStream post = request.getOutputStream();
                entity.writeTo(post);
                post.flush();
                post.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
