package net.heywifi.app;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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


public class RegisterPhoneActivity extends AppCompatActivity {

    DBManager dm;
    static int DATABASE_VERSION = 1;

    TextView phone_name_err_tv, phone_mac_tv;
    EditText phone_name_et;
    Button register_btn;

    LoadingDialog dialog;

    String id, pw, mac, nick;

    int pos;
    String[] tmac, tnick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerphone);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        phone_name_err_tv = (TextView) findViewById(R.id.phone_name_err_tv);
        phone_name_et = (EditText) findViewById(R.id.phone_name_et);
        phone_mac_tv = (TextView) findViewById(R.id.phone_mac_tv);
        register_btn = (Button) findViewById(R.id.register_btn);

        dm = new DBManager(getApplicationContext(), "data", null, DATABASE_VERSION);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        mac = wi.getMacAddress().toUpperCase();
        phone_mac_tv.setText(mac);

        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void register() {
        checkValid();

        getUserInfoFromDB();
        new RegisterPhoneTask().execute();
    }

    private void checkValid() {
        nick = phone_name_et.getText().toString();

        if (nick.length() > 30) {
            phone_name_err_tv.setText(R.string.register_err_toolong);
            phone_name_err_tv.setVisibility(View.VISIBLE);
        } else {
            phone_name_err_tv.setVisibility(View.INVISIBLE);
        }
    }

    private void getUserInfoFromDB() {
        String[] data = dm.selectUserinfo();
        id = data[0];
        pw = data[1];
    }

    private class RegisterPhoneTask extends AsyncTask<Void, Void, Integer> {

        String response;

        protected void onPreExecute() {
            dialog = new LoadingDialog(RegisterPhoneActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void ... params) {
            int status;

            connectGetResponse();
            status = decodeJson();

            if (status == 0) {
                downloadPhoneList();
                decodePhoneJson();
                writeOnDB();
            }

            return status;
        }

        protected void onPostExecute(Integer status) {
            dialog.dismiss();

            /*
             * 0:success
             * 1:wrong id or pw (Check before launch)
             * 2:already registered device (Check before launch)
             * 3:already registered nickname
             */
            switch (status) {
                case 0:
                    finish();
                    break;
                case 3:
                    phone_name_err_tv.setVisibility(View.VISIBLE);
                    phone_name_err_tv.setText(R.string.register_err_already);
                    break;
                default:
                    // TODO: show internal error
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
                nameValuePairs.add(new BasicNameValuePair("mac", mac));
                nameValuePairs.add(new BasicNameValuePair("nick", nick));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);

                String u = "https://www.heywifi.net/db/phone/registerphone.php";

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

        private void downloadPhoneList() {
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

        private void decodePhoneJson() {
            int status = -1;
            tmac = new String[5];
            tnick = new String[5];

            try {
                JSONObject json = new JSONObject(response);
                status = json.getInt("status");

                if (status == 0) {
                    pos = json.getInt("pos");
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
            if (dm.anythingInPhoneInfo()) {
                dm.updatePhoneInfo(id, pos, tmac, tnick);
            } else {
                dm.insertPhoneInfo(id, pos, tmac, tnick);
            }
        }
    }
}
