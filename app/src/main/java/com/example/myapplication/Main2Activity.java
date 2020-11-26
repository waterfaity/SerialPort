package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Main2Activity extends Activity {

    private static final String TAG = "main";

    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
        textView = findViewById(R.id.text);
        addText("启动程序");
        init();
    }

    private void init() {

    }

    /**
     * 输入
     *
     * @param view
     */
    public void ensureInput(View view) {
        EditText editText2 = findViewById(R.id.input2);
        ExeCommand exeCommand = new ExeCommand();
        exeCommand.run(editText2.getText().toString(), 2000);
        exeCommand.setOnDataListener(new ExeCommand.OnDataListener() {
            @Override
            public void onGetData(String data) {
                sendMessage(data);
            }
        });

//        EditText editText1 = findViewById(R.id.input1);
//        execu(editText1.getText().toString(), editText2.getText().toString());
    }

    Process exec;

    /**
     * 执行
     *
     * @param command1
     */
    private void execu(String command1, String command2) {
        try {
            exec = Runtime.getRuntime().exec(command1);
            read(exec.getInputStream());
            read(exec.getErrorStream());
            exec.getOutputStream().write((command2 + "\nexit\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void read(InputStream inputStream) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                while (true) {
                    try {
                        String s = bufferedReader.readLine();
                        if (!TextUtils.isEmpty(s)) {
                            sendMessage("执行结果:" + s);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            addText(">>" + msg.obj);
        }
    };


    private void sendMessage(String msg) {
        Message message = new Message();
        message.obj = msg;
        handler.sendMessage(message);
    }

    private void addText(String text) {
        textView.setText(text + "\n" + textView.getText().toString());
        Log.i(TAG, "addText: " + text);
    }
}