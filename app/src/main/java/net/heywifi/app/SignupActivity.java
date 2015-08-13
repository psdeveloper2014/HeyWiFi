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
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class SignupActivity extends AppCompatActivity {

    SharedPrefSettings pref;

    TextView id_err_tv, pw_err_tv, pw_re_err_tv, email_err_tv;
    EditText id_et, pw_et, pw_re_et, email_et;
    Button signup_btn;

    String id, pw, pw_re, salt, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        id_et = (EditText) findViewById(R.id.id_et);
        id_err_tv = (TextView) findViewById(R.id.id_err_tv);
        pw_et = (EditText) findViewById(R.id.pw_et);
        pw_err_tv = (TextView) findViewById(R.id.pw_err_tv);
        pw_re_et = (EditText) findViewById(R.id.pw_re_et);
        pw_re_err_tv = (TextView) findViewById(R.id.pw_re_err_tv);
        email_et = (EditText) findViewById(R.id.email_et);
        email_err_tv = (TextView) findViewById(R.id.email_err_tv);
        signup_btn = (Button) findViewById(R.id.signup_btn);

        pref = new SharedPrefSettings(this);

        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        if (!checkValid()) return;
        generateSalt();
        hashPassword();
        new SignupPostTask().execute();
    }

    private boolean checkValid() {
        boolean valid = true;
        id = id_et.getText().toString();
        pw = pw_et.getText().toString();
        pw_re = pw_re_et.getText().toString();
        email = email_et.getText().toString();

        // Check id
        if (id.length() < 6) {
            id_err_tv.setVisibility(View.VISIBLE);
            id_err_tv.setText(getResources().getString(R.string.too_short));
            valid = false;
        } else if (id.length() > 20) {
            id_err_tv.setVisibility(View.VISIBLE);
            id_err_tv.setText(getResources().getString(R.string.too_long));
            valid = false;
        } else {
            id_err_tv.setVisibility(View.INVISIBLE);
        }

        // Check password
        if (pw.length() < 6) {
            pw_err_tv.setVisibility(View.VISIBLE);
            pw_err_tv.setText(getResources().getString(R.string.too_short));
            valid = false;
        } else if (id.length() > 30) {
            pw_err_tv.setVisibility(View.VISIBLE);
            pw_err_tv.setText(getResources().getString(R.string.too_long));
            valid = false;
        } else {
            pw_err_tv.setVisibility(View.INVISIBLE);
        }

         // Check re-typed password
        if (!pw.equals(pw_re)) {
            pw_re_err_tv.setVisibility(View.VISIBLE);
            pw_re_err_tv.setText(getResources().getString(R.string.pw_re_incorrect));
            valid = false;
        } else {
            pw_re_err_tv.setVisibility(View.INVISIBLE);
        }

        // Check email
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_err_tv.setVisibility(View.VISIBLE);
            email_err_tv.setText(getResources().getString(R.string.email_incorrect));
            valid = false;
        } else {
            email_err_tv.setVisibility(View.INVISIBLE);
        }

        return valid;
    }

    private void generateSalt() {
        salt = BCrypt.gensalt();
    }

    private void hashPassword() {
        pw = BCrypt.hashpw(pw, salt);
    }

    private class SignupPostTask extends AsyncTask<Void, Void, Integer> {

        String response;
        LoadingDialog dialog;

        protected void onPreExecute() {
            dialog = new LoadingDialog(SignupActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void ... params) {
            int status;

            connectGetResponse();
            status = decodeJson();

            return status;
        }

        protected void onPostExecute(Integer status) {
            dialog.dismiss();

            /*
             * 0:success
             * 1:already registered id
             * 2:already registered email
             */
            switch (status) {
                case 0:
                    pref.putUserInfo(id, pw);
                    setResult(1);
                    finish();
                    break;
                case 1:
                    id_err_tv.setVisibility(View.VISIBLE);
                    email_err_tv.setVisibility(View.INVISIBLE);
                    id_err_tv.setText(getResources().getString(R.string.id_duplicated));
                    break;
                case 2:
                    id_err_tv.setVisibility(View.INVISIBLE);
                    email_err_tv.setVisibility(View.VISIBLE);
                    email_err_tv.setText(getResources().getString(R.string.email_duplicated));
                    break;
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
                nameValuePairs.add(new BasicNameValuePair("salt", salt));
                nameValuePairs.add(new BasicNameValuePair("email", email));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);

                String u = "https://www.heywifi.net/db/login/signup.php";

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