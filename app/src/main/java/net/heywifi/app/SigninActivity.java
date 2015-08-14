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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class SigninActivity extends AppCompatActivity {

    SharedPrefSettings pref;

    TextView id_err_tv, pw_err_tv;
    EditText id_et, pw_et;
    Button signin_btn;

    String id, pw, salt;

    LoadingDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        id_et = (EditText) findViewById(R.id.id_et);
        id_err_tv = (TextView) findViewById(R.id.id_err_tv);
        pw_et = (EditText) findViewById(R.id.pw_et);
        pw_err_tv = (TextView) findViewById(R.id.pw_err_tv);
        signin_btn = (Button) findViewById(R.id.signin_btn);

        pref = new SharedPrefSettings(this);

        signin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void login() {
        id = id_et.getText().toString();
        pw = pw_et.getText().toString();
        new SigninPostTask().execute();
    }

    private class SigninPostTask extends AsyncTask<Void, Void, Integer> {

        String response;

        protected void onPreExecute() {
            dialog = new LoadingDialog(SigninActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void... params) {
            int status, saltstatus;

            connectGetSalt();
            saltstatus = decodeSaltJson();

            // Success on getting salt
            if (saltstatus == 1) {
                hashPassword();
                connectGetResponse();
                status = decodeJson();
            } else {
                // No(Wrong) ID
                return 1;
            }

            return status;
        }

        protected void onPostExecute(Integer status) {
            dialog.dismiss();

            /*
             * 0:success
             * 1:wrong id
             * 2:wrong pw
             */
            switch (status) {
                case 0:
                    pref.putUserInfo(id, pw);
                    setResult(3);
                    finish();
                    break;
                case 1:
                    id_err_tv.setVisibility(View.VISIBLE);
                    pw_err_tv.setVisibility(View.INVISIBLE);
                    id_err_tv.setText(getResources().getString(R.string.id_notexist));
                    break;
                case 2:
                    id_err_tv.setVisibility(View.INVISIBLE);
                    pw_err_tv.setVisibility(View.VISIBLE);
                    pw_err_tv.setText(getResources().getString(R.string.pw_notexist));
            }
        }

        private void connectGetSalt() {
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
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);

                String u = "https://www.heywifi.net/db/login/getsalt.php";

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

        private int decodeSaltJson() {
            int saltstatus = -1;

            try {
                JSONObject json = new JSONObject(response);
                // 0:wrong id, 1:success
                saltstatus = json.getInt("status");
                salt = json.getString("salt");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return saltstatus;
        }

        private void hashPassword() {
            for (int i=0; i<1000; i++) {
                pw = encrypt(pw + salt);
            }
        }

        private String encrypt(String str) {
            try {
                MessageDigest sh = MessageDigest.getInstance("SHA-512");
                sh.update(str.getBytes());
                StringBuffer sb = new StringBuffer();
                for (byte b : sh.digest()) sb.append(Integer.toHexString(0xff & b));
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private void connectGetResponse() {
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

                String u = "https://www.heywifi.net/db/login/signin.php";

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

        private int decodeJson() {
            int status = -1;

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