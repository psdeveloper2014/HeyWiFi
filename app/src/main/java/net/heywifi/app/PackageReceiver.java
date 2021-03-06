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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class PackageReceiver extends BroadcastReceiver {

    SharedPrefSettings pref;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        pref = new SharedPrefSettings(context);

        MainAlarmManager mam = new MainAlarmManager(context);

        switch (action) {
            case Intent.ACTION_PACKAGE_ADDED:
                mam.set();
                break;
            case Intent.ACTION_PACKAGE_REPLACED:
                mam.set();
                break;
        }
    }
}
