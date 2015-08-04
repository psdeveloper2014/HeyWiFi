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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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


public class FindPhoneActivity extends AppCompatActivity {

    DBManager dm;
    static int DATABASE_VERSION = 1;

    TextView ring_tv, vibrate_tv;
    CheckBox ring_cb, vibrate_cb;
    Button findphone_btn;
    ListView lv;
    FindPhoneListAdapter adapter;

    boolean ringChecked, vibrateChecked;

    String id, pw;
    String[] mac = new String[5];
    String[] nick = new String[5];

    int selectedpos = -1;
    TextView item_tv;
    ImageView item_iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findphone);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent getIntent = getIntent();
        id = getIntent.getExtras().getString("id");
        pw = getIntent.getExtras().getString("pw");

        ring_tv = (TextView) findViewById(R.id.findphone_ring_tv);
        vibrate_tv = (TextView) findViewById(R.id.findphone_vibrate_tv);
        ring_cb = (CheckBox) findViewById(R.id.findphone_ring_cb);
        vibrate_cb = (CheckBox) findViewById(R.id.findphone_vibrate_cb);
        findphone_btn = (Button) findViewById(R.id.findphone_btn);
        lv = (ListView) findViewById(R.id.findphone_lv);
        adapter = new FindPhoneListAdapter(this);

        dm = new DBManager(getApplicationContext(), "data", null, DATABASE_VERSION);

        disableActions();
        fillListView();
    }

    private void disableActions() {
        ring_tv.setTextColor(getResources().getColor(R.color.gray));
        vibrate_tv.setTextColor(getResources().getColor(R.color.gray));
        ring_cb.setChecked(false);
        vibrate_cb.setChecked(false);
        ringChecked = false;
        vibrateChecked = false;
        ring_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ring_cb.setChecked(false);
                Toast.makeText(getApplicationContext(), "휴대폰을 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });
        vibrate_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                vibrate_cb.setChecked(false);
                Toast.makeText(getApplicationContext(), "휴대폰을 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });
        findphone_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "휴대폰을 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableActions() {
        ring_tv.setTextColor(getResources().getColor(R.color.black));
        vibrate_tv.setTextColor(getResources().getColor(R.color.black));
        ring_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ringChecked = isChecked;
            }
        });
        vibrate_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                vibrateChecked = isChecked;
            }
        });
        findphone_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: give data to FindingPhoneActivity
            }
        });
    }

    private void fillListView() {
        new GetPhoneListTask().execute();

        adapter.clearItem();
        for (int i=0; i<5; i++) {
            if (!nick[i].equals("")) {
                adapter.addItem(new FindPhoneListItem(nick[i], false));
            } else {
                break;
            }
        }
        lv.setAdapter(adapter);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If it's first time highlighting,
                if (selectedpos == -1) {
                    // Just highlight
                    highlightItem(view);
                }
                // But there's higlighted item,
                else {
                    disableHighlightedItem();
                    highlightItem(view);
                }

                // Bottom buttons
                enableActions();
            }
        });
    }

    private void highlightItem(View v) {
        item_tv = (TextView) v.findViewById(R.id.nick_tv);
        item_iv = (ImageView) v.findViewById(R.id.checked_iv);

        item_tv.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        item_iv.setVisibility(View.VISIBLE);
    }

    private void disableHighlightedItem() {
        item_tv.setTextColor(getResources().getColor(R.color.black));
        item_iv.setVisibility(View.INVISIBLE);
    }

    private class GetPhoneListTask extends AsyncTask<Void, Void, Void> {

        String response;

        @Override
        protected Void doInBackground(Void ... params) {
            downloadPhoneList();
            decodePhoneJson();

            return null;
        }

        private void downloadPhoneList() {
            try {
                response = "";

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
                nameValuePairs.add(new BasicNameValuePair("id", id));
                nameValuePairs.add(new BasicNameValuePair("pw", pw));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);

                String u = "https://www.heywifi.net/db/phone/getphoneinfo.php";

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

                String input;
                BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
                while ((input = in.readLine()) != null) {
                    response += input;
                }

                post.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void decodePhoneJson() {
            int status = -1;
            mac = new String[5];
            nick = new String[5];

            try {
                JSONObject json = new JSONObject(response);
                status = json.getInt("status");

                if (status == 0) {
                    mac[0] = json.getString("mac1");
                    nick[0] = json.getString("nick1");
                    mac[1] = json.getString("mac2");
                    nick[1] = json.getString("nick2");
                    mac[2] = json.getString("mac3");
                    nick[2] = json.getString("nick3");
                    mac[3] = json.getString("mac4");
                    nick[3] = json.getString("nick4");
                    mac[4] = json.getString("mac5");
                    nick[4] = json.getString("nick5");
                } else {
                    for (int i=0; i<5; i++) {
                        mac[i] = "";
                        nick[i] = "";
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
