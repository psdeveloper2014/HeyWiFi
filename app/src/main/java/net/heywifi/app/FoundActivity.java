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

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.skyfishjy.library.RippleBackground;


public class FoundActivity extends AppCompatActivity {

    boolean ring, vibrate;

    MediaPlayer mp;
    AudioManager am;
    int volume;
    Vibrator vb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found);

        Intent intent = getIntent();
        ring = intent.getBooleanExtra("ring", true);
        vibrate = intent.getBooleanExtra("vibrate", true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        RippleBackground ripple = (RippleBackground) findViewById(R.id.found_ripple);
        ripple.startRippleAnimation();

        Button found_btn = (Button) findViewById(R.id.found_btn);
        found_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        FinishActivityThread fat = new FinishActivityThread();
        fat.start();

        if (ring) {
            am = (AudioManager) getSystemService(AUDIO_SERVICE);
            volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 15, AudioManager.FLAG_PLAY_SOUND);

            mp = MediaPlayer.create(this, R.raw.payday);
            mp.start();
        }

        if (vibrate) {
            vb = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vb.vibrate(new long[] {1000, 1000, 1000, 1000}, 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.release();
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
        }
        if (vb != null) {
            vb.cancel();
        }
    }

    private class FinishActivityThread extends Thread {

        FinishActivityHandler handler;

        public FinishActivityThread() {
            handler = new FinishActivityHandler();
        }

        public void run() {
            try {
                // 2 min
                Thread.sleep(120000);
            } catch (InterruptedException e) {}

            handler.sendEmptyMessage(0);
        }
    }

    private class FinishActivityHandler extends Handler {

        public FinishActivityHandler() {}

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            finish();
        }
    }
}
