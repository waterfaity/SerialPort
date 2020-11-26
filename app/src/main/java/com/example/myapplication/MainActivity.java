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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class MainActivity extends Activity {

    private static final String TAG = "main";

    private TextView textView;
    private CheckBox checkBox;
    private CheckBox checkBoxLoop;
    private SerialPortManager serialPortManager;

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0)
                addText(">>" + msg.obj);
            else if (msg.what == 1) {
                send();
                sendEmptyMessageDelayed(1, 1000);
            }
        }
    };

    private String pathName = "/dev/ttyS2";
    private int baudRate = 4800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
        textView = findViewById(R.id.text);
        checkBox = findViewById(R.id.checkbox);
        checkBoxLoop = findViewById(R.id.send_loop);
        File file = new File(pathName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        addText("启动程序");

        serialPortManager = new SerialPortManager();
        SerialPortFinder serialPortFinder = new SerialPortFinder();


        if (file.exists()) {
            serialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
                @Override
                public void onSuccess(File file) {
                    addText("打开端口成功:" + file.getName());
                }

                @Override
                public void onFail(File file, Status status) {
                    addText("打开端口失败:" + file.getName() + " status:" + status.name());
                }
            });
            serialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
                @Override
                public void onDataReceived(byte[] bytes) {
                    handleData("接收:", bytes);
                }

                @Override
                public void onDataSent(byte[] bytes) {

                    handleData("发送:", bytes);
                }
            });
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    sendMessage("启动串口:" + serialPortManager.openSerialPort(file, baudRate));
//                    chmod777(file);
                }
            }.start();
        } else {
            addText("没有发现可用端口");
        }
    }

    private void handleData(String text, byte[] bytes) {
        Message message = new Message();

        StringBuilder num = new StringBuilder();
        StringBuilder num16 = new StringBuilder();
        String string = null;
        try {
            string = new String(bytes, "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            for (byte aByte : bytes) {
                num.append(aByte).append(" ");
                String s = Integer.toHexString(aByte).toUpperCase();
                if (s.length() > 2) s = s.substring(s.length() - 2);
                num16.append(s).append(" ");
            }
        }

        message.obj = text + ":\n" + num + " \n" + num16 + "\n" + string;
        handler.sendMessage(message);
    }


    boolean chmod777(File file) {

        if (null != file && file.exists()) {
            sendMessage("file:" + file.getAbsolutePath() + "\ncanRead:" + file.canRead() + "\ncanWrite:" + file.canWrite() + "\ncanExec:" + file.canExecute());
            try {

                String command = "ls";
                sendMessage("执行:" + command);
                Process su = Runtime.getRuntime().exec(command);
                String cmd = "chmod 777 " + file.getAbsolutePath();
                sendMessage("执行:" + cmd);
                read(su.getInputStream());
                su.getOutputStream().write(cmd.getBytes());
                if (0 == su.waitFor() && file.canRead() && file.canWrite() && file.canExecute()) {
                    sendMessage("执行成功");
                    return true;
                }
            } catch (InterruptedException | IOException var4) {
                var4.printStackTrace();
            }
            return false;
        } else {
            return false;
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


    private void sendMessage(String msg) {
        Message message = new Message();
        message.obj = msg;
        handler.sendMessage(message);
    }

    private void addText(String text) {
        textView.setText(text + "\n" + textView.getText().toString());
        Log.i(TAG, "addText: " + text);
    }


    public void ensureInput(View view) {
        if (checkBoxLoop.isChecked()) {
            handler.sendEmptyMessage(1);
        } else {
            send();
        }
    }

    private void send() {
        EditText editText = findViewById(R.id.input2);
        onInput(editText.getText().toString());
    }

    private void onInput(String text) {

        byte[] bytes;
        if (checkBox.isChecked()) {
            String[] s = text.split(" ");
            bytes = new byte[s.length];
            for (int j = 0; j < s.length; j++) {
                bytes[j] = (byte) Integer.parseInt(s[j], 16);
            }
        } else {
            bytes = text.getBytes();
        }

        boolean b = serialPortManager.sendBytes(bytes);
        if (!b) addText("写入失败:" + text);


        //byte 范围:  -128 127
        //1010 0100   0x00A4

        //bit 位  1
        //byte 一个字节  8bit    -128 127
        //char 两个字节  16bit   2byte=16bit  0 - 65535
        //short 两个字节 16bit   -32768  32767
        //int   4
        //long  8
        //float 4
        //double 8

//        byte ver_check_data[] = {(byte) 0xA4, 0x01, 0x00, 0x03};
    }

    public void stopInput(View view) {
        handler.removeMessages(1);
    }
}