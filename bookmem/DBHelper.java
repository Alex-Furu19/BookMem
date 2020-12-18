package com.example.bookmem;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {  //データベースオブジェクト生成時の処理
        super(context, "booktable", null , 1);
    }

    public void onCreate(SQLiteDatabase db) {  //データベース作成時の処理
        db.execSQL("CREATE TABLE booktable (item TEXT, num INTEGER, date TEXT )");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}  //データベースアップデート時の処理
}
