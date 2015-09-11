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


public class FindingPhoneActivityS01 extends AppCompatActivity {

    String gcmid, mac;
    int message;

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
        mac = getIntent.getStringExtra("mac");
        message = getIntent.getIntExtra("message", 3);

        progress = (CircleProgress) findViewById(R.id.circle_progress);
        text01 = (TextView) findViewById(R.id.finding_text01);
        text02 = (TextView) findViewById(R.id.finding_text02);
        found_btn = (Button) findViewById(R.id.found_btn);
        giveup_btn = (Button) findViewById(R.id.giveup_btn);
        nextstep_btn = (Button) findViewById(R.id.nextstep_btn);

        found_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(1);
                finish();
            }
        });

        giveup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(-1);
                finish();
            }
        });

        nextstep_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FindingPhoneActivityS01.this, FindingPhoneActivityS02.class);
                intent.putExtra("mac", mac);
                intent.putExtra("message", message);
                startActivityForResult(intent, 0);
            }
        });

        new RequestGcmTask().execute();
    }

    protected void onPause() {
        super.onPause();
    }

    private class TimeCountThread extends Thread {

        TimeCountHandler timehandler;

        public TimeCountThread() {
            timehandler = new TimeCountHandler();
        }

        public void run() {
            percent = 1;
            while (percent <= 100) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                }

                timehandler.sendEmptyMessage(percent);
                percent++;
            }
        }
    }

    private class TimeCountHandler extends Handler {

        public TimeCountHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            progress.setProgress(msg.what);

            if (msg.what == 100) {
                text01.setText(getResources().getString(R.string.finding01_found_ask));
                text02.setVisibility(View.INVISIBLE);
            }
        }
    }

    private class RequestGcmTask extends AsyncTask<Void, Void, Integer> {

        String response;

        protected Integer doInBackground(Void... params) {
            int status;

            requestGcm();
            status = decodeJson();

            return status;
        }

        protected void onPostExecute(Integer status) {
            // Success
            if (status == 1) {
                // Start time counting
                TimeCountThread tct = new TimeCountThread();
                tct.start();
                text01.setText(R.string.finding01_sent);
                text02.setText(R.string.finding01_guide);
                text02.setVisibility(View.VISIBLE);
            } else {
                text01.setText(R.string.finding01_failed);
                text02.setText(R.string.finding01_failed_guide);
                text02.setVisibility(View.VISIBLE);
            }
        }

        private void requestGcm() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/phone/requestgcm.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("gcmid", gcmid));
                nameValuePairs.add(new BasicNameValuePair("message", "" + message));
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
                status = json.getInt("success");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return status;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == -1) {
            setResult(-1);
            finish();
        }
    }
}
