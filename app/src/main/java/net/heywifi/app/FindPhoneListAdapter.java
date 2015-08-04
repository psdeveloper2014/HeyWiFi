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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class FindPhoneListAdapter extends BaseAdapter {

    Context context;
    List<FindPhoneListItem> items = new ArrayList<>();

    public FindPhoneListAdapter(Context context) {
        this.context = context;
    }

    public void addItem(FindPhoneListItem it) {
        items.add(it);
    }

    public void clearItem() {
        items.clear();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FindPhoneListView itemView;

        if (convertView == null) {
            itemView = new FindPhoneListView(context, items.get(position));
        } else {
            itemView = (FindPhoneListView) convertView;
        }

        return itemView;
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}

class FindPhoneListItem {

    String nick;
    boolean selected;

    public FindPhoneListItem(String nick, boolean selected) {
        this.nick = nick;
        this.selected = selected;
    }

    public String getNick() {
        return nick;
    }

    public boolean getSelected() {
        return selected;
    }
}

class FindPhoneListView extends RelativeLayout {

    public FindPhoneListView(Context context, FindPhoneListItem item) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listitem_findphone, this, true);

        TextView nick_tv = (TextView) findViewById(R.id.nick_tv);
        nick_tv.setText(item.getNick());
    }
}