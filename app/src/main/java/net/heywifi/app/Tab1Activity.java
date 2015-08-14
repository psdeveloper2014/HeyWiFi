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

import com.skyfishjy.library.RippleBackground;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
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


public class Tab1Activity extends Fragment {

    SharedPrefSettings pref;
    Context context;

    Button change_info_btn, find_others_device_btn, find_my_device_btn, login_btn;
    RelativeLayout parent_rl, registered_rl, not_registered_rl, not_logined_rl;
    TextView my_name_tv;
    ImageView n_ripple_iv;
    RippleBackground ripple, n_ripple;

    String id, pw;
    String mac = "", nick;
    String[] tmac = new String[5];
    String[] tnick = new String[5];

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_tab1, container, false);
        context = v.getContext();

        parent_rl = (RelativeLayout) v.findViewById(R.id.parent_rl);
        change_info_btn = (Button) v.findViewById(R.id.change_info_btn);
        find_others_device_btn = (Button) v.findViewById(R.id.find_others_device_btn);
        find_my_device_btn = (Button) v.findViewById(R.id.find_my_device_btn);
        login_btn = (Button) v.findViewById(R.id.login_btn);
        registered_rl = (RelativeLayout) v.findViewById(R.id.registered_rl);
        my_name_tv = (TextView) v.findViewById(R.id.my_name_tv);
        ripple = (RippleBackground) v.findViewById(R.id.ripple);
        not_registered_rl = (RelativeLayout) v.findViewById(R.id.not_registered_rl);
        n_ripple = (RippleBackground) v.findViewById(R.id.n_ripple);
        n_ripple_iv = (ImageView) v.findViewById(R.id.n_ripple_iv);
        not_logined_rl = (RelativeLayout) v.findViewById(R.id.not_logined_rl);

        pref = new SharedPrefSettings(context);

        new GetPhoneInfoTask().execute();

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

            }
        });

        // Find my device button
        find_my_device_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserInfo();
                Intent intent = new Intent(context, FindPhoneActivity.class);
                intent.putExtra("id", id);
                intent.putExtra("pw", pw);
                startActivity(intent);
            }
        });

        // Sign in, Sign up button
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, LoginActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        // Register phone image button
        n_ripple_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, RegisterPhoneActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
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
            if (isConnected()) {
                getUserInfo();
                connectGetPhoneInfo();
                decodeJson();
                checkThisPhoneRegistered();
            }

            return 0;
        }

        protected void onPostExecute(Integer result) {
            dialog.dismiss();
            loadUI();
        }

        private boolean isConnected() {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            return mobile.isConnected() || wifi.isConnected();
        }

        private void connectGetPhoneInfo() {
            try {
                response = "";

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(context.getResources().openRawResource(R.raw.comodo_rsaca));
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

        private void decodeJson() {
            try {
                JSONObject json = new JSONObject(response);
                int status = json.getInt("status");

                if (status == 0) {
                    tmac[0] = json.getString("mac1");
                    tnick[0] = json.getString("nick1");
                    tmac[1] = json.getString("mac2");
                    tnick[1] = json.getString("nick2");
                    tmac[2] = json.getString("mac3");
                    tnick[2] = json.getString("nick3");
                    tmac[3] = json.getString("mac4");
                    tnick[3] = json.getString("nick4");
                    tmac[4] = json.getString("mac5");
                    tnick[4] = json.getString("nick5");
                } else {
                    for (int i=0; i<5; i++) {
                        tmac[i] = "";
                        tnick[i] = "";
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void checkThisPhoneRegistered() {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.getConnectionInfo();
            String devicemac = wi.getMacAddress().toUpperCase();

            for (int i=0; i<5; i++) {
                if (devicemac.equals(tmac[i])) {
                    mac = tmac[i];
                    try {
                        nick = URLDecoder.decode(tnick[i], "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    pref.putPhoneInfo(mac, nick);
                }
            }

            // This phone was deleted on MySQL DB
            if (mac.isEmpty()) {
                pref.putPhoneInfo("", "");
            }
        }
    }

    private void getUserInfo() {
        String data[] = pref.getUserInfo();
        id = data[0];
        pw = data[1];
    }

    private void loadUI() {
        parent_rl.setVisibility(View.VISIBLE);

        if (pref.isUserLogined()) {
            getUserInfo();

            if (isMyPhoneRegistered()) {
                showButtons();
                setMyNameTextView();
                registered_rl.setVisibility(View.VISIBLE);
                not_registered_rl.setVisibility(View.INVISIBLE);
                not_logined_rl.setVisibility(View.INVISIBLE);
                ripple.startRippleAnimation();
                n_ripple.stopRippleAnimation();
            } else {
                showButtons();
                registered_rl.setVisibility(View.INVISIBLE);
                not_registered_rl.setVisibility(View.VISIBLE);
                not_logined_rl.setVisibility(View.INVISIBLE);
                ripple.stopRippleAnimation();
                n_ripple.startRippleAnimation();
            }
        } else {
            goneButtons();
            registered_rl.setVisibility(View.INVISIBLE);
            not_registered_rl.setVisibility(View.INVISIBLE);
            not_logined_rl.setVisibility(View.VISIBLE);
        }
    }

    private boolean isMyPhoneRegistered() {
        String[] data = pref.getPhoneInfo();
        mac = data[0];
        nick = data[1];

        if (!mac.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private void showButtons() {
        change_info_btn.setVisibility(View.VISIBLE);
        find_others_device_btn.setVisibility(View.VISIBLE);
        find_my_device_btn.setVisibility(View.VISIBLE);
        login_btn.setVisibility(View.GONE);
    }

    private void goneButtons() {
        change_info_btn.setVisibility(View.GONE);
        find_others_device_btn.setVisibility(View.VISIBLE);
        find_my_device_btn.setVisibility(View.GONE);
        login_btn.setVisibility(View.VISIBLE);
    }

    private void setMyNameTextView() {
        String myname = getResources().getString(R.string.my_name_header)
                + nick + getResources().getString(R.string.my_name_footer);
        my_name_tv.setText(myname);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        switch (resultCode) {
            // Found phone
            case 1:
                // TODO: give rate dialog
                break;
            // Successfully registered phone
            case 2:
                loadUI();
                break;
            // Successfully login
            case 3:
                new GetPhoneInfoTask().execute();
                break;
        }

    }
}
