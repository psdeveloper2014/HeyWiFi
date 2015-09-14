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

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class ManagePhoneActivity extends AppCompatActivity {

    TextView guide_tv;
    RelativeLayout[] item_rl = new RelativeLayout[6];
    TextView[] nick_tv = new TextView[6];
    ImageView[] edit_ivbtn = new ImageView[6];
    ImageView[] delete_ivbtn = new ImageView[6];

    SharedPrefSettings pref;

    int type;
    String id;

    int pos;
    String[] nick = new String[6];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managephone);

        guide_tv = (TextView) findViewById(R.id.guide_tv);
        item_rl[1] = (RelativeLayout) findViewById(R.id.item01_rl);
        item_rl[2] = (RelativeLayout) findViewById(R.id.item02_rl);
        item_rl[3] = (RelativeLayout) findViewById(R.id.item03_rl);
        item_rl[4] = (RelativeLayout) findViewById(R.id.item04_rl);
        item_rl[5] = (RelativeLayout) findViewById(R.id.item05_rl);
        nick_tv[1] = (TextView) findViewById(R.id.nick01_tv);
        nick_tv[2] = (TextView) findViewById(R.id.nick02_tv);
        nick_tv[3] = (TextView) findViewById(R.id.nick03_tv);
        nick_tv[4] = (TextView) findViewById(R.id.nick04_tv);
        nick_tv[5] = (TextView) findViewById(R.id.nick05_tv);
        edit_ivbtn[1] = (ImageView) findViewById(R.id.nick01_edit);
        edit_ivbtn[2] = (ImageView) findViewById(R.id.nick02_edit);
        edit_ivbtn[3] = (ImageView) findViewById(R.id.nick03_edit);
        edit_ivbtn[4] = (ImageView) findViewById(R.id.nick04_edit);
        edit_ivbtn[5] = (ImageView) findViewById(R.id.nick05_edit);
        delete_ivbtn[1] = (ImageView) findViewById(R.id.nick01_delete);
        delete_ivbtn[2] = (ImageView) findViewById(R.id.nick02_delete);
        delete_ivbtn[3] = (ImageView) findViewById(R.id.nick03_delete);
        delete_ivbtn[4] = (ImageView) findViewById(R.id.nick04_delete);
        delete_ivbtn[5] = (ImageView) findViewById(R.id.nick05_delete);

        pref = new SharedPrefSettings(this);
        type = pref.getUserType();
        id = pref.getUserId();

        new GetPhoneInfoTask().execute();
        applyItems();
    }

    private class GetPhoneInfoTask extends AsyncTask<Void, Void, Integer> {

        String response;
        LoadingDialog dialog;

        protected void onPreExecute() {
            dialog = new LoadingDialog(ManagePhoneActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
        }

        protected Integer doInBackground(Void... params) {
            connectGetPhoneInfo();
            decodeJson();

            return 0;
        }

        protected void onPostExecute(Integer result) {
            dialog.dismiss();
        }

        private void connectGetPhoneInfo() {
            try {
                response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://slave.heywifi.net/query/phone/getphoneinfo.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + type));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void decodeJson() {
            try {
                JSONObject json = new JSONObject(response);
                int status = json.getInt("status");

                if (status == 1) {
                    pos = json.getInt("pos");
                    for (int i=1; i<=pos; i++) {
                        nick[i] = URLDecoder.decode(json.getString("nick"+i), "utf-8");
                    }
                } else {
                    pos = -1;
                }
            } catch (Exception e) {
                pos = -1;
            }
        }
    }

    private void applyItems() {
        switch (pos) {
            case -1:
                guide_tv.setText(R.string.findphone_guide_nointernet);
                break;
            case 0:
                guide_tv.setText(R.string.findphone_guide_nophone);
                break;
            default:
                guide_tv.setText(R.string.manage_guide);
                break;
        }

        for (int i=1; i<=5; i++) {
            item_rl[i].setVisibility(View.GONE);
        }

        for (int i=1; i<=pos; i++) {
            item_rl[i].setVisibility(View.VISIBLE);
            nick_tv[i].setText(nick[i]);
        }

        edit_ivbtn[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameNickDialog rdialog = new RenameNickDialog(ManagePhoneActivity.this, nick[1], type, id, 1);
                rdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                rdialog.show();
            }
        });
        delete_ivbtn[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePhoneDialog ddialog = new DeletePhoneDialog(ManagePhoneActivity.this, nick[1], type, id, 1);
                ddialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ddialog.show();
            }
        });
        edit_ivbtn[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameNickDialog rdialog = new RenameNickDialog(ManagePhoneActivity.this, nick[2], type, id, 2);
                rdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                rdialog.show();
            }
        });
        delete_ivbtn[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePhoneDialog ddialog = new DeletePhoneDialog(ManagePhoneActivity.this, nick[2], type, id, 2);
                ddialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ddialog.show();
            }
        });
        edit_ivbtn[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameNickDialog rdialog = new RenameNickDialog(ManagePhoneActivity.this, nick[3], type, id, 3);
                rdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                rdialog.show();
            }
        });
        delete_ivbtn[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePhoneDialog ddialog = new DeletePhoneDialog(ManagePhoneActivity.this, nick[3], type, id, 3);
                ddialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ddialog.show();
            }
        });
        edit_ivbtn[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameNickDialog rdialog = new RenameNickDialog(ManagePhoneActivity.this, nick[4], type, id, 4);
                rdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                rdialog.show();
            }
        });
        delete_ivbtn[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePhoneDialog ddialog = new DeletePhoneDialog(ManagePhoneActivity.this, nick[4], type, id, 4);
                ddialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ddialog.show();
            }
        });
        edit_ivbtn[5].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenameNickDialog rdialog = new RenameNickDialog(ManagePhoneActivity.this, nick[5], type, id, 5);
                rdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                rdialog.show();
            }
        });
        delete_ivbtn[5].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeletePhoneDialog ddialog = new DeletePhoneDialog(ManagePhoneActivity.this, nick[5], type, id, 5);
                ddialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ddialog.show();
            }
        });
    }
}

class RenameNickDialog extends Dialog {

    String nick;
    int type;
    String id;
    int renamepos;
    String renamenick;

    LinearLayout main_ly;
    RelativeLayout loading_rl;

    public RenameNickDialog(Context context, String nick, int type, String id, int renamepos) {
        super(context);
        this.nick = nick;
        this.type = type;
        this.id = id;
        this.renamepos = renamepos;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_renamenick);

        main_ly = (LinearLayout) findViewById(R.id.main_ly);
        loading_rl = (RelativeLayout) findViewById(R.id.loading_rl);
        TextView dialog_text = (TextView) findViewById(R.id.dialog_rename_tv);
        EditText dialog_et = (EditText) findViewById(R.id.dialog_rename_et);
        Button save_btn = (Button) findViewById(R.id.dialog_savebtn);
        Button close_btn = (Button) findViewById(R.id.dialog_closebtn);

        dialog_text.setText(nick + getContext().getResources().getString(R.string.dialog_deletetext));
        renamenick = dialog_et.getText().toString();

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendRenameRequestThread renameThread = new SendRenameRequestThread();
                renameThread.start();
            }
        });

        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    class SendRenameRequestThread extends Thread {

        public SendRenameRequestThread() {}

        public void run() {
            // Show loading LinearLayout
            uiHandler.sendEmptyMessage(0);

            try {
                String response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://master.heywifi.net/query/phone/renamenick.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + type));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                nameValuePairs.add(new BasicNameValuePair("renamepos", "" + renamepos));
                nameValuePairs.add(new BasicNameValuePair("renamenick", URLEncoder.encode(renamenick, "utf-8")));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();

                JSONObject json = new JSONObject(response);
                int status = json.getInt("status");

                if (status == 1) {
                    // cancel();
                    uiHandler.sendEmptyMessage(1);
                } else {
                    main_ly.setVisibility(View.VISIBLE);
                    loading_rl.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                main_ly.setVisibility(View.VISIBLE);
                loading_rl.setVisibility(View.GONE);
            }
        }

        // 시간을 화면에 표시하는 핸들러
        private Handler uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == 0) {
                    main_ly.setVisibility(View.GONE);
                    loading_rl.setVisibility(View.VISIBLE);
                } else {
                    cancel();
                }
            }
        };
    }
}

class DeletePhoneDialog extends Dialog {

    String nick;
    int type;
    String id;
    int deletepos;

    LinearLayout main_ly;
    RelativeLayout loading_rl;

    public DeletePhoneDialog(Context context, String nick, int type, String id, int deletepos) {
        super(context);
        this.nick = nick;
        this.type = type;
        this.id = id;
        this.deletepos = deletepos;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_deletephone);

        main_ly = (LinearLayout) findViewById(R.id.main_ly);
        loading_rl = (RelativeLayout) findViewById(R.id.loading_rl);
        TextView dialog_text = (TextView) findViewById(R.id.dialog_text);
        Button delete_btn = (Button) findViewById(R.id.dialog_deletebtn);
        Button close_btn = (Button) findViewById(R.id.dialog_closebtn);

        dialog_text.setText(nick + getContext().getResources().getString(R.string.dialog_deletetext));

        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendDeleteRequestThread deleteThread = new SendDeleteRequestThread();
                deleteThread.start();
            }
        });

        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    class SendDeleteRequestThread extends Thread {

        public SendDeleteRequestThread() {}

        public void run() {
            // Show loading LinearLayout
            uiHandler.sendEmptyMessage(0);

            try {
                String response = "";

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://master.heywifi.net/query/phone/deletephone.php");

                List nameValuePairs = new ArrayList(2);
                nameValuePairs.add(new BasicNameValuePair("type", "" + type));
                nameValuePairs.add(new BasicNameValuePair("id", id));
                nameValuePairs.add(new BasicNameValuePair("deletepos", "" + deletepos));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse httpResponse = httpClient.execute(httpPost);
                response = httpResponse.toString();

                JSONObject json = new JSONObject(response);
                int status = json.getInt("status");

                if (status == 1) {
                    // cancel();
                    uiHandler.sendEmptyMessage(1);
                } else {
                    main_ly.setVisibility(View.VISIBLE);
                    loading_rl.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                main_ly.setVisibility(View.VISIBLE);
                loading_rl.setVisibility(View.GONE);
            }
        }

        // 시간을 화면에 표시하는 핸들러
        private Handler uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == 0) {
                    main_ly.setVisibility(View.GONE);
                    loading_rl.setVisibility(View.VISIBLE);
                } else {
                    cancel();
                }
            }
        };
    }
}