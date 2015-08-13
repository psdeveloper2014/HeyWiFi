package net.heywifi.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class IntroActivity extends AppCompatActivity {

    String senderId = "648637692734";

    public static IntroActivity aIntro;
    SharedPrefSettings pref;

    CountDownLatch latch;

    String id, pw;

    String mac = "", nick;
    String[] tmac = new String[5];
    String[] tnick = new String[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // To close IntroActivity when MainActivity launches
        aIntro = IntroActivity.this;
        pref = new SharedPrefSettings(this);
        latch = new CountDownLatch(2);

        if (pref.isFirstLaunch()) {
            // Launch welcome activity when first launch
            // TODO: make welcome activity

            // Launch login activity when no account registered
            if (!pref.isUserLogined()) {
                Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
                startActivityForResult(intent, 0);
            }
            // If user successfully login, do two tasks at below on onActivityResult()
        } else {
            new GetPhoneInfoTask().execute();
            new RegisterGcmTask().execute();
        }
    }

    private class GetPhoneInfoTask extends AsyncTask<Void, Void, Integer> {

        String response;

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
            // Wait until other task is finish
            latch.countDown();
            try {
                latch.await(7, TimeUnit.SECONDS);
            } catch (InterruptedException e) {}

            // Starting MainActivity
            Intent intent = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(intent);
        }

        private boolean isConnected() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            return mobile.isConnected() || wifi.isConnected();
        }

        private void getUserInfo() {
            String[] data = pref.getUserInfo();
            id = data[0];
            pw = data[1];
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
    }

    private void checkThisPhoneRegistered() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
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

    private class RegisterGcmTask extends AsyncTask<Void, Void, Integer> {

        GoogleCloudMessaging gcm;

        String regid;

        protected Integer doInBackground(Void ... params) {
            Context context = getApplicationContext();

            if (isConnected()) {
                gcm = GoogleCloudMessaging.getInstance(context);
                regid = getRegistrationId(context);

                if (regid.isEmpty()) {
                    register();
                }
            }

            return 0;
        }

        protected void onPostExecute(Integer result) {
            // Wait until other task is finish
            latch.countDown();
            try {
                latch.await(7, TimeUnit.SECONDS);
            } catch (InterruptedException e) {}
        }

        private boolean isConnected() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            return mobile.isConnected() || wifi.isConnected();
        }

        private String getRegistrationId(Context context) {
            String registrationId = pref.getRegId();
            if (registrationId.isEmpty()) {
                return "";
            }

            int registeredVersion = pref.getRegVersion();
            int currentVersion = getAppVersion(context);
            if (registeredVersion != currentVersion) {
                return "";
            }

            return registrationId;
        }

        private int getAppVersion(Context context) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return pi.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private void register() {
            try {
                regid = gcm.register(senderId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pref.putRegId(regid);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == 1) {
            new GetPhoneInfoTask().execute();
            new RegisterGcmTask().execute();
        }
    }
}
