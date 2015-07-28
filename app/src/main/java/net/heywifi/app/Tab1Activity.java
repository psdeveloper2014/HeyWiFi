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


public class Tab1Activity extends Fragment {

    DBManager dm;
    static int DATABASE_VERSION = 1;

    View v;

    Button change_info_btn, find_others_device_btn, find_my_device_btn;
    RelativeLayout on_logined_rl, on_notlogined_rl;
    TextView my_name_tv;
    ImageView n_ripple_iv;
    RippleBackground ripple, n_ripple;

    String[] mac = new String[5];
    String[] nick = new String[5];

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.activity_tab1, container, false);

        change_info_btn = (Button) v.findViewById(R.id.change_info_btn);
        find_others_device_btn = (Button) v.findViewById(R.id.find_others_device_btn);
        find_my_device_btn = (Button) v.findViewById(R.id.find_my_device_btn);
        on_logined_rl = (RelativeLayout) v.findViewById(R.id.on_logined_rl);
        my_name_tv = (TextView) v.findViewById(R.id.my_name_tv);
        ripple = (RippleBackground) v.findViewById(R.id.ripple);
        on_notlogined_rl = (RelativeLayout) v.findViewById(R.id.on_notlogined_rl);
        n_ripple = (RippleBackground) v.findViewById(R.id.n_ripple);
        n_ripple_iv = (ImageView) v.findViewById(R.id.n_ripple_iv);

        dm = new DBManager(v.getContext(), "data", null, DATABASE_VERSION);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (dm.isUserLogined()) {
            on_logined_rl.setVisibility(View.VISIBLE);
            on_notlogined_rl.setVisibility(View.INVISIBLE);
            ripple.startRippleAnimation();
            n_ripple.stopRippleAnimation();
            getDeviceInfo();
            setMyNameTextView();
        } else {
            on_logined_rl.setVisibility(View.INVISIBLE);
            on_notlogined_rl.setVisibility(View.VISIBLE);
            ripple.stopRippleAnimation();
            n_ripple.startRippleAnimation();
            // TODO: n_ripple_iv.setOnClickListener() ...
        }
    }

    private void getDeviceInfo() {
        if (!dm.anythingInPhoneInfo()) {
            new PhoneInfoPostTask().execute();
        }

        String[] data = dm.selectPhoneInfo();
        for (int i=0; i<5; i++) {
            mac[i] = data[i*2];
            nick[i] = data[i*2+1];
        }
    }

    private void setMyNameTextView() {
        WifiManager wm = (WifiManager) v.getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        String gmac = wi.getMacAddress();

        for (int i=0; i<5; i++) {
            if (gmac.equals(mac[i])) {
                String myname = getResources().getString(R.string.my_name_header)
                        + nick[i] + getResources().getString(R.string.my_name_footer);
                my_name_tv.setText(myname);
            }
        }
    }

    private class PhoneInfoPostTask extends AsyncTask<Void, Void, Void> {

        String response;
        LoadingDialog dialog;

        String[] tmac = new String[5];
        String[] tnick = new String[5];

        protected void onPreExecute() {
            dialog = new LoadingDialog(v.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Void doInBackground(Void ... params) {
            connectGetPhoneInfo();
            decodeJson();
            writeOnDB();

            return null;
        }

        protected void onPostExecute(Void result) {
            dialog.dismiss();
        }

        private void connectGetPhoneInfo() {
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

                String[] user = dm.selectUserinfo();
                String id = user[0];
                String pw = user[1];

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

                // Data is on json
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

        private void writeOnDB() {
            dm.insertPhoneInfo(tmac, tnick);
        }
    }
}
