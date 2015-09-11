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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skyfishjy.library.RippleBackground;

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


public class Tab1Activity extends Fragment {

    SharedPrefSettings pref;
    Context context;

    Button change_info_btn, find_others_device_btn, find_my_device_btn;
    RelativeLayout registered_rl, not_registered_rl;
    TextView my_name_tv, name_below_tv;
    ImageView n_ripple_iv;
    RippleBackground n_ripple;

    String mac;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_tab1, container, false);
        context = v.getContext();

        change_info_btn = (Button) v.findViewById(R.id.change_info_btn);
        find_others_device_btn = (Button) v.findViewById(R.id.find_others_device_btn);
        find_my_device_btn = (Button) v.findViewById(R.id.find_my_device_btn);
        registered_rl = (RelativeLayout) v.findViewById(R.id.registered_rl);
        my_name_tv = (TextView) v.findViewById(R.id.my_name_tv);
        name_below_tv = (TextView) v.findViewById(R.id.name_below_tv);
        not_registered_rl = (RelativeLayout) v.findViewById(R.id.not_registered_rl);
        n_ripple = (RippleBackground) v.findViewById(R.id.n_ripple);
        n_ripple_iv = (ImageView) v.findViewById(R.id.n_ripple_iv);

        pref = new SharedPrefSettings(context);

        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        mac = wi.getMacAddress().toUpperCase();

        if (isConnected()) {
            new GetPhoneInfoTask().execute();
            // GetPhoneInfoTask calls loadUI()
        } else {
            loadUI();
        }

        // Change my phone information button
        change_info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Find other's device button
        find_others_device_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, FindOthersActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        // Find my device button
        find_my_device_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, FindPhoneActivity.class);
                intent.putExtra("type", pref.getUserType());
                intent.putExtra("id", pref.getUserId());
                startActivityForResult(intent, 0);
            }
        });

        // Register phone image button
        n_ripple_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()) {
                    Intent intent = new Intent(context, RegisterPhoneActivity.class);
                    startActivityForResult(intent, 0);
                } else {
                    Toast.makeText(context, R.string.register_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return v;
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mobile.isConnected() || wifi.isConnected();
    }

    private class GetPhoneInfoTask extends AsyncTask<Void, Void, Integer> {

        String response;
        LoadingDialog dialog;

        protected void onPreExecute() {
            dialog = new LoadingDialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void... params) {
            connectGetPhoneInfo();
            decodeJson();

            return 0;
        }

        protected void onPostExecute(Integer result) {
            dialog.dismiss();
            loadUI();
        }

        private void connectGetPhoneInfo() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/phone/checkregistered.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + pref.getUserType()));
                nameValuePairs.add(new BasicNameValuePair("id", pref.getUserId()));
                nameValuePairs.add(new BasicNameValuePair("mac", mac));
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
                int status = json.getInt("status");

                if (status == 1) {
                    String nick = json.getString("nick");
                    pref.putPhoneInfo(mac, URLDecoder.decode(nick, "utf-8"));
                } else {
                    pref.putPhoneInfo("", "");
                }
            } catch (Exception e) {}
        }
    }

    private void loadUI() {
        if (isMyPhoneRegistered()) {
            String[] data = pref.getPhoneInfo();
            my_name_tv.setText(data[1]);
            registered_rl.setVisibility(View.VISIBLE);
            not_registered_rl.setVisibility(View.INVISIBLE);
            n_ripple.stopRippleAnimation();

            if (isConnected()) {
                name_below_tv.setText(R.string.namebelow_registered);
            } else {
                name_below_tv.setText(R.string.namebelow_failed);
            }
        } else {
            registered_rl.setVisibility(View.INVISIBLE);
            not_registered_rl.setVisibility(View.VISIBLE);
            n_ripple.startRippleAnimation();
        }
    }

    private boolean isMyPhoneRegistered() {
        String[] data = pref.getPhoneInfo();
        mac = data[0];

        return !mac.isEmpty();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        switch (resultCode) {
            // Found phone
            case 1:
                CongratulationDialog cdialog = new CongratulationDialog(context);
                cdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                cdialog.show();
                break;
            // Successfully registered phone
            case 2:
                loadUI();
                break;
            // Successfully login
            case 3:
                // Moved to IntroActivity
                break;
            // Five Device Registered
            case 4:
                FiveDevicesDialog fdialog = new FiveDevicesDialog(context);
                fdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                fdialog.show();
                break;
        }

    }
}

class CongratulationDialog extends Dialog {

    public CongratulationDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_warning);

        TextView dialog_title = (TextView) findViewById(R.id.dialog_title);
        TextView dialog_text = (TextView) findViewById(R.id.dialog_text);
        Button closebtn = (Button) findViewById(R.id.dialog_closebtn);

        dialog_title.setText(R.string.congratulation_dialog_title);
        dialog_text.setText(R.string.congratulation_dialog_text);

        closebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}

class FiveDevicesDialog extends Dialog {

    public FiveDevicesDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_warning);

        TextView dialog_title = (TextView) findViewById(R.id.dialog_title);
        TextView dialog_text = (TextView) findViewById(R.id.dialog_text);
        Button closebtn = (Button) findViewById(R.id.dialog_closebtn);

        dialog_title.setText(R.string.fivedevices_dialog_title);
        dialog_text.setText(R.string.fivedevices_dialog_text);

        closebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}