package com.example.bookmem;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class BookMem extends AppCompatActivity
{
    ArrayAdapter<String> adapter;
    private SQLiteDatabase db;
    private DBHelper dbhelper;
    private EditText et;
    private ListView listView;
    boolean narrowDown_flag = false;

    private int tappedPosition = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        dbhelper = new DBHelper(this);
        db = dbhelper.getWritableDatabase();

        listView=(ListView)findViewById(R.id.list_view);

        Button btn1=(Button)findViewById(R.id.btn1);
        Button btn2=(Button)findViewById(R.id.btn2);
        Button btn3=(Button)findViewById(R.id.btn3);

        et = (EditText)findViewById(R.id.edit_text);

        adapterUpdate();

        //登録ボタンが選択されたときのイベント
        btn1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                narrowDown_flag = false;
                checkDuplicate();
                et.setText("");
            }
        });

        //検索ボタンが選択されたときのイベント
        btn2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                narrowDown_flag = true;
                adapterUpdate();
            }
        });

        //戻るボタンが選択されたときのイベント
        btn3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                narrowDown_flag = false;
                adapterUpdate();
                et.setText("");
            }
        });

        //リスト項目が選択された時のイベント
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPosition(position);
                changeBooksNum(1);
            }
        });

        //リスト項目が長押しされた時のイベント
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) listView.getItemAtPosition(position);
                setPosition(position);
                alertCheck(item);
                return true;
            }
        });

        //ListViewの更新
        listView.setAdapter(adapter);
    }

    //adapterを更新
    private void adapterUpdate(){
        adapter.clear();
        Cursor c;
        String item = et.getText().toString();
        if(item.length() == 0){
            narrowDown_flag = false;
        }
        if(narrowDown_flag){
            c = db.query("booktable", new String[]{"item", "num", "date"}, "item like ?", new String[]{"%"+item+"%"}, null, null, null);
        }else{
            c = db.query("booktable", new String[]{"item", "num", "date"}, null, null, null, null, null);
        }
        boolean mov = c.moveToFirst();
        while(mov){
            adapter.add(String.format("%s%10d冊%10s",c.getString(0), c.getInt(1), c.getString(2)));
            mov = c.moveToNext();
        }
    }

    //クリックポジションの取得
    private void setPosition(int position) {
        tappedPosition = position;
    }
    private int getPosition() {
        return tappedPosition;
    }

    //日付の取得
    private String getTime(){
        Time time = new Time("Asia/Tokyo");
        time.setToNow();
        String date = (time.month + 1) + "/" + time.monthDay;
        return date;
    }

    //冊数の増減を更新
    private void changeBooksNum(int num){
        Cursor c;
        int position = getPosition();
        if(narrowDown_flag){
            String item = et.getText().toString();
            c = db.query("booktable", new String[]{"item", "num", "date"}, "item like ?", new String[]{"%"+item+"%"}, null, null, null);
        }else{
            c = db.query("booktable", new String[]{"item", "num", "date"}, null, null, null, null, null);
        }
        c.moveToPosition(position);
        String item = c.getString(0);
        String date = getTime();
        int booknum = c.getInt(1);
        booknum += num;
        ContentValues val = new ContentValues();
        val.put("num", booknum);
        val.put("date", date);
        db.update("booktable", val, "item = ?", new String[]{item});
        adapterUpdate();
    }

    //本の重複を確認
    private void checkDuplicate(){
        String item = et.getText().toString();
        Cursor c = db.query("booktable", new String[]{"item", "num", "date"}, "item = ?", new String[]{item}, null, null, null);
        if(c.getCount() != 0) {
            duplicateBook();
        }else{
            addDB();
        }
    }

    //データベースの更新
    private void addDB(){
        String item = et.getText().toString();
        if(item.length()==0)
            return;
        String date = getTime();

        ContentValues val = new ContentValues();
        val.put("item", item);
        val.put("num", 1);
        val.put("date", date);
        db.insert("booktable", null, val);
        adapterUpdate();
    }

    //ロングクリックで表示されるアラート
    private void alertCheck(String item) {
        String[] alert_menu = {"まとめて追加", "まとめて削除", "項目の削除", "cancel"};

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(item);
        alert.setItems(alert_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int idx) {
                if (idx == 0) {
                    checkInclimentNum();
                }
                else if (idx == 1) {
                    checkDeclimentNum();
                }
                else if (idx == 2) {
                    deleteCheck();
                }
                else {
                }
            }
        });
        alert.show();
    }

    //本の増加数を取得するアラート
    private void checkInclimentNum(){
        final EditText editText = new EditText(this);
        editText.setHint("数字を入力");
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("本を追加")
                .setMessage("追加する冊数を入力してください。")
                .setView(editText)
                .setNeutralButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String num_s = editText.getText().toString();
                        int num = Integer.parseInt(num_s);
                        changeBooksNum(num);
                    }
                })
                .show();
    }

    //本の削除数を取得するアラート
    private void checkDeclimentNum(){
        final EditText editText = new EditText(this);
        editText.setHint("数字を入力");
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("本を削除")
                .setMessage("削除する冊数を入力してください。")
                .setView(editText)
                .setNeutralButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String num_s = editText.getText().toString();
                        int num = Integer.parseInt(num_s);
                        num *= -1;
                        changeBooksNum(num);
                    }
                })
                .show();
    }

    //本が重複していた時のアラート
    private void duplicateBook() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("既存");

        alertDialogBuilder.setMessage("既存のため追加できません");

        alertDialogBuilder.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        alertDialogBuilder.setCancelable(true);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //リスト項目を削除する時の確認アラート
    private void deleteCheck() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("削除");

        alertDialogBuilder.setMessage("本当に削除しますか？");

        alertDialogBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { deleteItem(); }
                });
        alertDialogBuilder.setNeutralButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        alertDialogBuilder.setCancelable(true);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //リスト項目の削除
    private void deleteItem() {
        Cursor c;
        int position = getPosition();
        if(narrowDown_flag){
            String item = et.getText().toString();
            c = db.query("booktable", new String[]{"item", "num", "date"}, "item like ?", new String[]{"%"+item+"%"}, null, null, null);
        }else{
            c = db.query("booktable", new String[]{"item", "num", "date"}, null, null, null, null, null);
        }
        c.moveToPosition(position);
        String item = c.getString(0);
        db.delete("booktable", "item = ?", new String[]{item});
        adapterUpdate();
    }
}
