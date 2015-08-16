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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
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


public class LoginActivity extends AppCompatActivity {

    Context context;
    SharedPrefSettings pref;

    OAuthLogin mOAuthLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        context = LoginActivity.this;
        pref = new SharedPrefSettings(context);

        // Naver Login
        mOAuthLogin = OAuthLogin.getInstance();
        mOAuthLogin.init(this, "HpmkeVhxXvFf0TjDKJxu", "5Jl6ZbPtSM", "HeyWiFi");

        OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
            @Override
            public void run(boolean success) {
                if (success) {
                    new SuccessNaverLoginTask().execute();
                } else {
                    // TODO: Oops Naver Auth Server Error! Dialog
                }
            }
        };

        OAuthLoginButton mOAuthLoginButton = (OAuthLoginButton) findViewById(R.id.naver_btn);
        mOAuthLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);

    }

    private class SuccessNaverLoginTask extends AsyncTask<Void, Void, Integer> {

        String response;
        String email, id, name;

        protected Integer doInBackground(Void... params) {
            requestApi();
            decodeXml();
            saveInfoOnServer();
            saveInfo();

            return 0;
        }

        private void requestApi() {
            String accessToken = mOAuthLogin.getAccessToken(context);
            response = mOAuthLogin.requestApi(context, accessToken, "https://apis.naver.com/nidlogin/nid/getUserProfile.xml");
        }

        private void decodeXml() {
            try {
                XmlPullParser xpp;
                XmlPullParserFactory factory;
                int eventType;
                String tagName = null;

                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                xpp = factory.newPullParser();
                xpp.setInput(new StringReader(response));
                eventType = xpp.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("response")) {
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                tagName = xpp.getName();
                            } else if (eventType == XmlPullParser.TEXT && tagName != null) {
                                switch (tagName) {
                                    case "email":
                                        email = xpp.getText().trim();
                                        break;
                                    case "id":
                                        id = xpp.getText().trim();
                                        break;
                                    case "name":
                                        name = xpp.getText().trim();
                                        break;
                                }
                            }
                            eventType = xpp.next();
                        }
                    } else {
                        eventType = xpp.next();
                    }
                }
            } catch (Exception e) {}
        }

        private void saveInfoOnServer() {
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
                nameValuePairs.add(new BasicNameValuePair("type", "" + pref.NAVER_TYPE));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                nameValuePairs.add(new BasicNameValuePair("name", URLEncoder.encode(name, "utf-8")));
                nameValuePairs.add(new BasicNameValuePair("email", email));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);

                String u = "https://www.heywifi.net/query/login/register.php";

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

                Log.e("Register", response);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void saveInfo() {
            pref.putUserType(pref.NAVER_TYPE);
            pref.putUserId(id);
            pref.putUserName(name);
        }

        protected void onPostExecute(Integer result) {
            setResult(3);
            finish();
        }
    }
}
