package net.heywifi.app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;

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

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


public class GcmIntentService extends IntentService {

    boolean ring, vibrate;
    int reqdate;
    int[] date = new int[2];

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        try {
            if (!extras.isEmpty()) {
                if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    decodeMessage(extras.getString("message"));
                    getDate();

                    if (reqdate == date[0] || reqdate == date[1]) {
                        Intent mIntent = new Intent(GcmIntentService.this, FoundActivity.class);
                        mIntent.putExtra("ring", ring);
                        mIntent.putExtra("vibrate", vibrate);
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mIntent);
                    }
                }
            }
        } catch (Exception e) {}

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void decodeMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            // 2:ring, 1: vibrate / mode = ring + vibrate
            int mode = json.getInt("mode");
            switch (mode) {
                case 3:
                    ring = true;
                    vibrate = true;
                    break;
                case 2:
                    ring = true;
                    vibrate = false;
                    break;
                case 1:
                    ring = false;
                    vibrate = true;
                    break;
                case 0:
                    ring = false;
                    vibrate = false;
                    break;
            }
            // Requested date
            reqdate = json.getInt("date");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getDate() {
        String response = "";
        try {
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

            String u = "https://www.heywifi.net/db/phone/getdate.php";

            URL url = new URL(u);
            HttpsURLConnection request = (HttpsURLConnection) url.openConnection();

            request.setSSLSocketFactory(sslContext.getSocketFactory());
            request.setUseCaches(false);
            request.setDoInput(true);
            request.setDoOutput(true);
            request.setRequestMethod("POST");
            OutputStream post = request.getOutputStream();
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

        decodeJson(response);
    }

    private void decodeJson(String response) {
        try {
            JSONObject json = new JSONObject(response);
            date[0] = json.getInt("date1");
            date[1] = json.getInt("date2");
        } catch (JSONException e) {
            date[0] = 0;
            date[1] = 0;
        }
    }
}
