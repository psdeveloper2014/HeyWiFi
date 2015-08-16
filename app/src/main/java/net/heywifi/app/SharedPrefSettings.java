/*
 * Copyright 2015 Park Si Hyeon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.heywifi.app;

import android.content.Context;
import android.content.SharedPreferences;


public class SharedPrefSettings {

    Context c;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    public int NAVER_TYPE = 1;
    public int FACEBOOK_TYPE = 2;

    public SharedPrefSettings(Context context) {
        c = context;
        pref = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // GCM Registration Id
    public String getRegId() {
        return pref.getString("regid", "");
    }

    public void putRegId(String regid) {
        editor.putString("regid", regid);
        editor.commit();
    }

    // GCM Registered Version
    public int getRegVersion() {
        return pref.getInt("regversion", Integer.MIN_VALUE);
    }

    public void putRegVersion(int regversion) {
        editor.putInt("regversion", regversion);
        editor.commit();
    }

    public String[] getPhoneInfo() {
        String[] data = new String[2];
        data[0] = pref.getString("mac", "");
        data[1] = pref.getString("nick", "");

        return data;
    }

    public void putPhoneInfo(String mac, String nick) {
        editor.putString("mac", mac);
        editor.putString("nick", nick);
        editor.commit();
    }

    public boolean anythingPhoneInfo() {
        String mac = pref.getString("mac", "");

        if (mac.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public int getUserType() {
        return pref.getInt("type", 0);
    }

    public String getUserId() {
        return pref.getString("id", "");
    }

    public String getUserName() {
        return pref.getString("name", "");
    }

    public void putUserType(int type) {
        editor.putInt("type", type);
        editor.commit();
    }

    public void putUserId(String id) {
        editor.putString("id", id);
        editor.commit();
    }

    public void putUserName(String name) {
        editor.putString("name", name);
        editor.commit();
    }

    public boolean isUserLogined() {
        return pref.getInt("type", 0) != 0;
    }

    public boolean isFirstLaunch() {
        boolean boo = pref.getBoolean("firstlaunch", true);

        if (boo) {
            editor.putBoolean("firstlaunch", false);
            return true;
        } else {
            return false;
        }
    }
}