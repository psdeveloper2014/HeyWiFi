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
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DBManager extends SQLiteOpenHelper {

    SQLiteDatabase db;
    String query;

    static final String TB_USERINFO = "userinfo";
    static final String TB_PHONEINFO = "phoneinfo";

    public DBManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        query = "CREATE TABLE " + TB_USERINFO + " (" +
                "id TEXT, pw TEXT);";
        db.execSQL(query);

        query = "CREATE TABLE " + TB_PHONEINFO + " (" +
                "mac1 TEXT, nick1 TEXT, mac2 TEXT, nick2 TEXT, mac3 TEXT, nick3 TEXT, " +
                "mac4 TEXT, nick4 TEXT, mac5 TEXT, nick5 TEXT);";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertUserinfo(String id, String pw) {
        query = "INSERT INTO " + TB_USERINFO + " VALUES(" +
                "'" + id + "', '" + pw + "');";
        db.execSQL(query);
    }

    public String[] selectUserinfo() {
        Cursor csr;
        String[] data = new String[2];

        query = "SELECT * FROM " + TB_USERINFO + ";";
        csr = db.rawQuery(query, null);
        csr.moveToFirst();

        try {
            data[0] = csr.getString(0);     // id
            data[1] = csr.getString(1);     // pw
        } catch (CursorIndexOutOfBoundsException e) {
            // If there's nothing in cursor
            for (int i=0; i<2; i++) {
                data[i] = "";
            }
        }

        return data;
    }

    public void updateUserinfo(String id, String pw, String salt, String email) {

    }

    public boolean isUserLogined() {
        Cursor csr;
        boolean logined;

        query = "SELECT id FROM " + TB_USERINFO + ";";
        csr = db.rawQuery(query, null);
        csr.moveToFirst();

        try {
            if (!csr.getString(0).isEmpty()) logined = true;
            else logined = false;
        } catch (CursorIndexOutOfBoundsException e) {
            logined = false;
        }

        return logined;
    }

    public void insertPhoneInfo(String[] mac, String[] nick) {
        query = "INSERT INTO " + TB_PHONEINFO + " VALUES(";
        for (int i=0; i<5; i++) {
            if (i != 4) {
                query += mac[i] + ", " + nick[i] + ", ";
            } else {
                query += mac[i] + ", " + nick[i] + ");";
            }
        }
        db.execSQL(query);
    }

    public String[] selectPhoneInfo() {
        Cursor csr;
        String[] data = new String[10];

        query = "SELECT * FROM " + TB_PHONEINFO + ";";
        csr = db.rawQuery(query, null);
        csr.moveToFirst();

        try {
            for (int i=0; i<10; i++) {
                data[i] = csr.getString(i);
            }
        } catch (CursorIndexOutOfBoundsException e) {
            // If there's nothing in cursor
            for (int i=0; i<10; i++) {
                data[i] = "";
            }
        }

        return data;
    }

    public boolean anythingInPhoneInfo() {
        Cursor csr;
        boolean inthere;

        query = "SELECT mac1 FROM " + TB_PHONEINFO + ";";
        csr = db.rawQuery(query, null);
        csr.moveToFirst();

        try {
            if (!csr.getString(0).isEmpty()) inthere = true;
            else inthere = false;
        } catch (CursorIndexOutOfBoundsException e) {
            inthere = false;
        }

        return inthere;
    }
}
