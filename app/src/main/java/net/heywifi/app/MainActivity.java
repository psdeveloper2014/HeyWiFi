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

import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class MainActivity extends ActionBarActivity {

    SharedPrefSettings pref;

    Toolbar toolbar;
    ViewPager vp;
    ViewPagerAdapter vpadapter;
    SlidingTabLayout tabs;
    CharSequence titles[] = new CharSequence[2];
    int numtab = 2;

    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Close IntroActivity
        IntroActivity.intro.finish();

        AdView adv = (AdView) findViewById(R.id.ad);
        AdRequest adr = new AdRequest.Builder().build();
        adv.loadAd(adr);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        titles[0] = getResources().getString(R.string.tab1_title);
        titles[1] = getResources().getString(R.string.tab2_title);

        vpadapter = new ViewPagerAdapter(getSupportFragmentManager(), titles, numtab);
        vp = (ViewPager) findViewById(R.id.viewpager);
        vp.setAdapter(vpadapter);

        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true);
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });

        tabs.setViewPager(vp);

        pref = new SharedPrefSettings(this);

        MainAlarmManager mam = new MainAlarmManager(this);
        mam.set();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        setUserInfoMenuTitle(menu);
        return true;
    }

    private void setUserInfoMenuTitle(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_user);
        if (pref.isUserLogined()) {
            String name = pref.getUserName();
            item.setTitle(getResources().getText(R.string.action_greet1) + name + getResources().getText(R.string.action_greet2));
        } else {
            item.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_user:
                break;
            case R.id.action_settings:
                break;
            case R.id.action_feedback:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
