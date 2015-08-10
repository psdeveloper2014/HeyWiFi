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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;


public class GcmRegister {

    Context context;
    SharedPrefSettings pref;

    GoogleCloudMessaging gcm;
    String regid;
    String senderid = "648637692734";

    public GcmRegister(Context context) {
        this.context = context;
        pref = new SharedPrefSettings(context);
    }

    public String register() {
        gcm = GoogleCloudMessaging.getInstance(context);
        regid = getRegistrationId();

        if (regid.isEmpty()) {
            try {
                regid = gcm.register(senderid);
                pref.putRegId(regid);
            } catch (IOException e) {}
        }

        return regid;
    }

    private String getRegistrationId() {
        if (pref.getRegId().isEmpty()) {
            return "";
        }

        int regVersion = pref.getRegVersion();
        int currentVersion = getAppVersion();
        if (regVersion != currentVersion) {
            return "";
        }

        return pref.getRegId();
    }

    private int getAppVersion() {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name");
        }
    }
}
