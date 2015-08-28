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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class IntroActivity extends AppCompatActivity {

    public static IntroActivity intro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        intro = IntroActivity.this;
        SharedPrefSettings pref = new SharedPrefSettings(this);

        if (pref.isFirstLaunch()) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivityForResult(intent, 0);
        } else {
            if (!pref.isUserLogined()) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivityForResult(intent, 0);
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultcode, Intent data) {
        switch (resultcode) {
            // Success Login
            case 1:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            // Canceled
            default:
                finish();
                break;
        }
    }
}
