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


public class FindOthersActivity extends AppCompatActivity {

    Context context;
    SharedPrefSettings pref;

    OAuthLogin mOAuthLogin;

    CallbackManager callbackManager;
    String fid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_findothers);

        context = FindOthersActivity.this;
        pref = new SharedPrefSettings(context);

        // Naver Login
        mOAuthLogin = OAuthLogin.getInstance();
        mOAuthLogin.init(context, "HpmkeVhxXvFf0TjDKJxu", "5Jl6ZbPtSM", "HeyWiFi");

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
                            startFacebookIntent();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id");
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
        String id;

        protected Integer doInBackground(Void... params) {
            requestApi();
            decodeXml();

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

        protected void onPostExecute(Integer result) {
            // Don't need to keep login state
            mOAuthLogin.logout(context);

            Intent nIntent = new Intent(context, FindPhoneActivity.class);
            nIntent.putExtra("type", pref.NAVER_TYPE);
            nIntent.putExtra("id", id);
            startActivityForResult(nIntent, 0);
        }
    }

    private void startFacebookIntent() {
        // Don't need to keep login state
        LoginManager.getInstance().logOut();

        Intent fIntent = new Intent(context, FindPhoneActivity.class);
        fIntent.putExtra("type", pref.FACEBOOK_TYPE);
        fIntent.putExtra("id", fid);
        startActivityForResult(fIntent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            // Give up
            case -1:
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
