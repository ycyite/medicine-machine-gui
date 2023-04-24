package com.example.anroid_gui;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private static final UUID JDY31_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice mJDY31Device;

    private TextView bluetoothTextView = null;
    private TextView tempTextView = null;
    private TextView speedTextView = null;
    private TextView lightTextView = null;

    private Thread bluetoothCheckThread = null;

    private Thread startSystemThread = null;

    private Button link_button = null;
    private Button detect_button = null;
    private Button start_button = null;

    private Handler bluetoothtextHandler = null;

    private Handler temptextHandler = null;

    private Handler speedtextHandler = null;

    private Handler lighttextHandler = null;

    private BluetoothSocket socket = null;

    private String TempStr = null;

    private String SpeedStr = null;

    private String LightStr = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findAllComponents();
        configureAllHandler();
        configureAllText();
        configureAllButtons();
        configureBluetooth();

        createAllThread();
        startAllThread();
    }
    private void configureAllHandler() {
        bluetoothtextHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    bluetoothTextView.setText("Connected");
                } else if (msg.what == 1) {
                    bluetoothTextView.setText("Disconnected");
                }
            }
        };
        temptextHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    tempTextView.setText(TempStr);
                }
            }
        };

        speedtextHandler =  new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    speedTextView.setText(SpeedStr);
                }
            }
        };

        lighttextHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0){
                    lightTextView.setText(LightStr);
                }
            }
        };
    }
    private void startSystem(){
        String message = "0";
        OutputStream outputStream = null;
        byte[] messageBytes = message.getBytes(Charset.defaultCharset());
        try {
            outputStream = socket.getOutputStream();
            outputStream.write(messageBytes);

        } catch (IOException e) {
            // Handle the exception
        }
        InputStream inputStream = null;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        byte[] buffer = new byte[1024];
        int bytes;
        String receivedString = "";
        while (true) {
            try {
                outputStream.write(messageBytes);
                Thread.sleep(10);
                bytes = inputStream.read(buffer);
                System.out.println(buffer);
                System.out.println(bytes);
                receivedString = new String(buffer, 0, bytes);
                String res[] = receivedString.split(" ");
                System.out.println(receivedString);
                if(res.length == 0){
                    continue;
                }
                else if(res.length == 1){
                    TempStr = res[0];
                    if(res[0].equals("111")){
                        break;
                    }
                    temptextHandler.sendEmptyMessage(0);
                }
                else if(res.length == 2){
                    TempStr = res[0];
                    SpeedStr = res[1];
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);

                }else if(res.length == 3){
                    TempStr = res[0];
                    SpeedStr = res[1];
                    LightStr = "On";
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);

                }
                Thread.sleep(900);

            } catch (IOException e) {
                System.out.println(e.getMessage());
                // Handle IOException
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void createAllThread() {
        if (bluetoothCheckThread == null) {
            bluetoothCheckThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mJDY31Device == null) {

                    }
                    while (true) {
                        if (socket == null || !socket.isConnected()) {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            try {
                                socket = mJDY31Device.createInsecureRfcommSocketToServiceRecord(JDY31_UUID);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            bluetoothtextHandler.sendEmptyMessage(1);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            connectToJDY31();
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                }
            });
        }
    }
    private void findAllComponents(){
        bluetoothTextView = findViewById(R.id.bluetoothvalue_id);
        tempTextView = findViewById(R.id.tempvalue_id);
        speedTextView = findViewById(R.id.speedvalue_id);
        lightTextView = findViewById(R.id.light_status_id);
        link_button = findViewById(R.id.button_bluetooth);
        detect_button = findViewById(R.id.button_selfdetect);
        start_button = findViewById(R.id.button_start);
    }

    private void configureAllText(){
        bluetoothtextHandler.sendEmptyMessage(1);
    }
    private void startAllThread(){
        bluetoothCheckThread.start();
    }
    private void configureBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast toast = Toast.makeText(getApplicationContext(), "This device doesn't support the bluetooth!", Toast.LENGTH_SHORT);
            // Show the Toast
            toast.show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    private void searchForJDY31() throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("JDY31")) {
                mJDY31Device = device;

                break;
            }
        }

    }

    private void connectToJDY31() {

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            socket.connect();
            bluetoothTextView.clearComposingText();
            bluetoothtextHandler.sendEmptyMessage(0);
//            Toast toast = Toast.makeText(getApplicationContext(), "The JDY31 Module is connected successfully!", Toast.LENGTH_SHORT);
//            // Show the Toast
//            toast.setDuration(Toast.LENGTH_SHORT);
//            toast.show();
            // Connection successful
        } catch (IOException e) {
            System.out.println(e.getMessage());
            // Connection failed
        }
    }
    public void configureAllButtons() {

        link_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mJDY31Device==null) {
                    try {
                        searchForJDY31();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
//                    bluetoothCheckThread.start();
                // You can now send and receive data over the socket
                // ...
            }
//                Toast toast = Toast.makeText(getApplicationContext(), "Link Successfully!", Toast.LENGTH_SHORT);
//                toast.show();
        });
        detect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSystemThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startSystem();
                    }
                });
                startSystemThread.start();
            }
        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }
}