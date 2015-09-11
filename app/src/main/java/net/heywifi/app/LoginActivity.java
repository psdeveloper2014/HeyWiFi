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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class LoginActivity extends AppCompatActivity {

    Context context;
    SharedPrefSettings pref;

    OAuthLogin mOAuthLogin;

    CallbackManager callbackManager;
    String fid, fname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
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
                }
            }
        };

        OAuthLoginButton mOAuthLoginButton = (OAuthLoginButton) findViewById(R.id.naver_btn);
        mOAuthLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);

        // Facebook Login
        callbackManager = CallbackManager.Factory.create();
        LoginButton facebook_btn = (LoginButton) findViewById(R.id.facebook_btn);
        facebook_btn.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject json, GraphResponse response) {
                        try {
                            fid = (String) response.getJSONObject().get("id");
                            fname = (String) response.getJSONObject().get("name");
                            new SuccessFacebookLoginTask().execute();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {}

            @Override
            public void onError(FacebookException e) {}
        });
    }

    private class SuccessNaverLoginTask extends AsyncTask<Void, Void, Integer> {

        String response;
        String id, name;

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

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://master.heywifi.net/query/login/register.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + pref.NAVER_TYPE));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                nameValuePairs.add(new BasicNameValuePair("name", URLEncoder.encode(name, "utf-8")));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void saveInfo() {
            pref.putUserType(pref.NAVER_TYPE);
            pref.putUserId(id);
            pref.putUserName(name);

            // Don't need to keep login state
            // Also, for FindOthersActivity
            mOAuthLogin.logout(context);
        }

        protected void onPostExecute(Integer result) {
            setResult(1);
            finish();
        }
    }

    private class SuccessFacebookLoginTask extends AsyncTask<Void, Void, Integer> {

        String response;

        protected Integer doInBackground(Void... params) {
            saveInfoOnServer();
            saveInfo();

            return 0;
        }

        private void saveInfoOnServer() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://master.heywifi.net/query/login/register.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + pref.FACEBOOK_TYPE));
                nameValuePairs.add(new BasicNameValuePair("id", fid));
                nameValuePairs.add(new BasicNameValuePair("name", URLEncoder.encode(fname, "utf-8")));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void saveInfo() {
            pref.putUserType(pref.FACEBOOK_TYPE);
            pref.putUserId(fid);
            pref.putUserName(fname);

            // Don't need to keep login state
            // Also, for FindOthersActivity
            LoginManager.getInstance().logOut();
        }

        protected void onPostExecute(Integer result) {
            setResult(1);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
