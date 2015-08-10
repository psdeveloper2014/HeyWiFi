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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skyfishjy.library.RippleBackground;


public class Tab1Activity extends Fragment {

    SharedPrefSettings pref;

    View v;

    Button change_info_btn, find_others_device_btn, find_my_device_btn, login_btn;
    RelativeLayout registered_rl, not_registered_rl, not_logined_rl;
    TextView my_name_tv;
    ImageView n_ripple_iv;
    RippleBackground ripple, n_ripple;

    String id, pw;
    String mac, nick;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.activity_tab1, container, false);

        change_info_btn = (Button) v.findViewById(R.id.change_info_btn);
        find_others_device_btn = (Button) v.findViewById(R.id.find_others_device_btn);
        find_my_device_btn = (Button) v.findViewById(R.id.find_my_device_btn);
        login_btn = (Button) v.findViewById(R.id.login_btn);
        registered_rl = (RelativeLayout) v.findViewById(R.id.registered_rl);
        my_name_tv = (TextView) v.findViewById(R.id.my_name_tv);
        ripple = (RippleBackground) v.findViewById(R.id.ripple);
        not_registered_rl = (RelativeLayout) v.findViewById(R.id.not_registered_rl);
        n_ripple = (RippleBackground) v.findViewById(R.id.n_ripple);
        n_ripple_iv = (ImageView) v.findViewById(R.id.n_ripple_iv);
        not_logined_rl = (RelativeLayout) v.findViewById(R.id.not_logined_rl);

        pref = new SharedPrefSettings(v.getContext());

        // Change my phone information button
        change_info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Find other's device button
        find_others_device_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Find my device button
        find_my_device_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserInfo();
                Intent intent = new Intent(v.getContext(), FindPhoneActivity.class);
                intent.putExtra("id", id);
                intent.putExtra("pw", pw);
                startActivity(intent);
            }
        });

        // Sign in, Sign up button
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), LoginActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        // Register phone image button
        n_ripple_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), RegisterPhoneActivity.class);
                startActivity(intent);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUI();
    }

    private void loadUI() {
        if (pref.isUserLogined()) {
            loadUserInfo();
            if (isMyPhoneRegistered()) {
                showButtons();
                setMyNameTextView();
                registered_rl.setVisibility(View.VISIBLE);
                not_registered_rl.setVisibility(View.INVISIBLE);
                not_logined_rl.setVisibility(View.INVISIBLE);
                ripple.startRippleAnimation();
                n_ripple.stopRippleAnimation();
            } else {
                showButtons();
                registered_rl.setVisibility(View.INVISIBLE);
                not_registered_rl.setVisibility(View.VISIBLE);
                not_logined_rl.setVisibility(View.INVISIBLE);
                ripple.stopRippleAnimation();
                n_ripple.startRippleAnimation();
            }
        } else {
            goneButtons();
            registered_rl.setVisibility(View.INVISIBLE);
            not_registered_rl.setVisibility(View.INVISIBLE);
            not_logined_rl.setVisibility(View.VISIBLE);
        }
    }

    private boolean isMyPhoneRegistered() {
        String[] data = pref.getPhoneInfo();
        mac = data[0];
        nick = data[1];

        if (!mac.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private void showButtons() {
        change_info_btn.setVisibility(View.VISIBLE);
        find_others_device_btn.setVisibility(View.VISIBLE);
        find_my_device_btn.setVisibility(View.VISIBLE);
        login_btn.setVisibility(View.GONE);
    }

    private void goneButtons() {
        change_info_btn.setVisibility(View.GONE);
        find_others_device_btn.setVisibility(View.VISIBLE);
        find_my_device_btn.setVisibility(View.GONE);
        login_btn.setVisibility(View.VISIBLE);
    }

    private void setMyNameTextView() {
        String myname = getResources().getString(R.string.my_name_header)
                + nick + getResources().getString(R.string.my_name_footer);
        my_name_tv.setText(myname);
    }

    private void loadUserInfo() {
        String data[] = pref.getUserInfo();
        id = data[0];
        pw = data[1];
    }
}
