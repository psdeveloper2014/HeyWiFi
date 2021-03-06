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
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class FindPhoneActivity extends AppCompatActivity {

    TextView guide_tv;
    LinearLayout find_ly;
    CheckBox ring_cb, vibrate_cb;
    Button findphone_btn;
    ListView lv;
    FindPhoneListAdapter adapter;

    int ringChecked = 2, vibrateChecked = 1;

    int type;
    String id;
    String[] mac = new String[5];
    String[] nick = new String[5];
    String[] gcmid = new String[5];

    int selectedpos = -1;
    TextView item_tv;
    ImageView item_iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findphone);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PhoneWarningDialog dialog = new PhoneWarningDialog(FindPhoneActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();

        Intent getIntent = getIntent();
        type = getIntent.getExtras().getInt("type");
        id = getIntent.getExtras().getString("id");

        guide_tv = (TextView) findViewById(R.id.findphone_guide_tv);
        find_ly = (LinearLayout) findViewById(R.id.findphone_find_ly);
        ring_cb = (CheckBox) findViewById(R.id.findphone_ring_cb);
        vibrate_cb = (CheckBox) findViewById(R.id.findphone_vibrate_cb);
        findphone_btn = (Button) findViewById(R.id.findphone_btn);
        lv = (ListView) findViewById(R.id.findphone_lv);
        adapter = new FindPhoneListAdapter(this);

        find_ly.setVisibility(View.GONE);
        if (isConnected()) {
            fillListView();
        } else {
            guide_tv.setText(R.string.findphone_guide_nointernet);
        }

        ring_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ringChecked = 2;
                } else {
                    ringChecked = 0;
                }
            }
        });

        vibrate_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    vibrateChecked = 1;
                } else {
                    vibrateChecked = 0;
                }
            }
        });

        findphone_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FindPhoneActivity.this, FindingPhoneActivityS01.class);
                intent.putExtra("gcmid", gcmid[selectedpos]);
                intent.putExtra("mac", mac[selectedpos]);
                intent.putExtra("message", ringChecked + vibrateChecked);
                startActivityForResult(intent, 0);
            }
        });
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mobile.isConnected() || wifi.isConnected();
    }

    private void fillListView() {
        new GetPhoneListTask().execute();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Make visible bottom find button
                find_ly.setVisibility(View.VISIBLE);

                // If it's first time highlighting,
                if (selectedpos == -1) {
                    highlightItem(view);
                }
                // There's already higlighted item,
                else {
                    disableHighlightedItem();
                    highlightItem(view);
                }

                selectedpos = position;
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

    private class GetPhoneListTask extends AsyncTask<Void, Void, Integer> {

        String response;

        @Override
        protected Integer doInBackground(Void ... params) {
            downloadPhoneList();
            decodePhoneJson();

            return 0;
        }

        protected void onPostExecute(Integer result) {
            adapter.clearItem();
            try {
                for (int i=0; i<5; i++) {
                    if (!nick[i].equals("") && nick[i] != null && !nick[i].equals("null")) {
                        adapter.addItem(new FindPhoneListItem(URLDecoder.decode(nick[i], "utf-8")));
                    } else {
                        break;
                    }
                }
                lv.setAdapter(adapter);
                guide_tv.setText(R.string.findphone_guide);
            } catch (Exception e) {
                guide_tv.setText(R.string.findphone_guide_nophone);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        private void downloadPhoneList() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/phone/getphoneinfo.php");

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

        private void decodePhoneJson() {
            int status;

            try {
                JSONObject json = new JSONObject(response);
                status = json.getInt("status");

                if (status == 1) {
                    mac[0] = json.getString("mac1");
                    nick[0] = json.getString("nick1");
                    gcmid[0] = json.getString("gcmid1");
                    mac[1] = json.getString("mac2");
                    nick[1] = json.getString("nick2");
                    gcmid[1] = json.getString("gcmid2");
                    mac[2] = json.getString("mac3");
                    nick[2] = json.getString("nick3");
                    gcmid[2] = json.getString("gcmid3");
                    mac[3] = json.getString("mac4");
                    nick[3] = json.getString("nick4");
                    gcmid[3] = json.getString("gcmid4");
                    mac[4] = json.getString("mac5");
                    nick[4] = json.getString("nick5");
                    gcmid[4] = json.getString("gcmid5");
                } else {
                    for (int i=0; i<5; i++) {
                        mac[i] = "";
                        nick[i] = "";
                        gcmid[i] = "";
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (resultCode) {
            // Give up
            case -1:
                setResult(-1);
                finish();
                break;
            // Found
            case 1:
                setResult(1);
                finish();
                break;
        }
    }
}

class PhoneWarningDialog extends Dialog {

    public PhoneWarningDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_warning);

        TextView dialog_title = (TextView) findViewById(R.id.dialog_title);
        TextView dialog_text = (TextView) findViewById(R.id.dialog_text);
        Button closebtn = (Button) findViewById(R.id.dialog_closebtn);

        dialog_title.setText(getContext().getResources().getString(R.string.findphone_dialog_title));
        dialog_text.setText(getContext().getResources().getString(R.string.findphone_dialog_text));

        closebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}