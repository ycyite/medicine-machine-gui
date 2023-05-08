package com.example.anroid_gui;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private TextView bluetoothTextView = null;
    private TextView tempTextView = null;
    private TextView speedTextView = null;
    private TextView lightTextView = null;

    private OutputStream outputStream = null;
    private InputStream inputStream = null;

    private Thread bluetoothCheckThread = null;


    private Thread startSystemThread = null;



    private Button link_button = null;
    private Button pause_button = null;
    private Button start_button = null;

    private Handler bluetoothtextHandler = null;

    private Handler temptextHandler = null;

    private Handler speedtextHandler = null;

    private Handler lighttextHandler = null;

    private Handler bluetoothbuttonHandler = null;

    private Handler AllToastHandler = null;

    private Handler pausebuttonHandler = null;

    private Handler startbuttonHandler = null;



    private BluetoothSocket socket = null;


    private String TempStr = null;

    private String SpeedStr = null;

    private String LightStr = null;

    private boolean pause_flag = false;
    private BroadcastReceiver bluetoothReceiver = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureReceiver();
        findAllComponents();
        configureAllHandler();
        configureAllText();
        configureAllButtons();
        configureBluetooth();

        pause_flag = false;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver when the Activity is destroyed
        unregisterReceiver(bluetoothReceiver);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, perform Bluetooth connection here
            } else {
                // Permission denied, show a message or disable Bluetooth features
            }
        }
    }

    private void BluetoothCheck(){
        while(true){
            if(socket==null || !socket.isConnected()){
                if(startSystemThread!=null && startSystemThread.isAlive()){
                    pause_flag = true;
                }

                bluetoothtextHandler.sendEmptyMessage(1);
                bluetoothbuttonHandler.sendEmptyMessage(1);
                startbuttonHandler.sendEmptyMessage(0);
                pausebuttonHandler.sendEmptyMessage(0);


                break;
            }else if(!mBluetoothAdapter.isEnabled()){
                if(startSystemThread!=null && startSystemThread.isAlive()){
                    pause_flag = true;
                }

                bluetoothtextHandler.sendEmptyMessage(1);
                bluetoothbuttonHandler.sendEmptyMessage(1);
                AllToastHandler.sendEmptyMessage(0);
                startbuttonHandler.sendEmptyMessage(0);
                pausebuttonHandler.sendEmptyMessage(0);


                break;
            }
        }
    }
    private void configureReceiver() {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
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
                    if (device.getName().equals("JDY31")) {

                        if(startSystemThread!=null){
                            if(startSystemThread.isAlive()){
                                try {
                                    if(inputStream!=null) {
                                        inputStream.close();
                                    }
                                    if(outputStream!=null){
                                        outputStream.close();
                                    }
                                    if(socket!=null && socket.isConnected()){
                                        socket.close();
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                startSystemThread.interrupt();
                            }
                        }
                        bluetoothtextHandler.sendEmptyMessage(1);
                        bluetoothbuttonHandler.sendEmptyMessage(1);
                        AllToastHandler.sendEmptyMessage(1);
                        startbuttonHandler.sendEmptyMessage(0);
                        pausebuttonHandler.sendEmptyMessage(0);
                        lighttextHandler.sendEmptyMessage(1);
                        temptextHandler.sendEmptyMessage(1);
                        speedtextHandler.sendEmptyMessage(1);

                        // JDY31 module is disconnected
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void configureAllHandler() {
        AllToastHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == 0){
                    Toast.makeText(MainActivity.this, "Please switch on the phone's bluetooth", Toast.LENGTH_SHORT).show();
                } else if(msg.what == 1){
                    Toast.makeText(MainActivity.this, " Please switch on the equipment's bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        };
        bluetoothbuttonHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    link_button.setEnabled(false);
                } else if (msg.what == 1) {
                    link_button.setEnabled(true);
                }

            }
        };
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
        temptextHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    tempTextView.setText(TempStr);
                } else if(msg.what == 1){
                    tempTextView.setText("");
                }
            }
        };

        speedtextHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    speedTextView.setText(SpeedStr);
                } else if(msg.what == 1){
                    speedTextView.setText("");
                }
            }
        };

        lighttextHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    lightTextView.setText(LightStr);
                } else if(msg.what == 1){
                    lightTextView.setText("");
                }
            }
        };

        pausebuttonHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == 0){
                    pause_button.setEnabled(false);
                }else if(msg.what == 1){
                    pause_button.setEnabled(true);
                }
            }
        };

        startbuttonHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == 0){
                    start_button.setEnabled(false);
                }else if(msg.what == 1){
                    start_button.setEnabled(true);
                }
            }
        };


    }


    private void startSystem() {
        pause_flag = false;
        String message = "l";
        byte[] messageBytes = message.getBytes(Charset.defaultCharset());
        try {
            outputStream = socket.getOutputStream();
            outputStream.write(messageBytes);
        } catch (IOException e) {
            // Handle the exception
        }
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
                if(pause_flag){
                    String pause_msg = "p";
                    byte[] pause_msg_bytes = pause_msg.getBytes(Charset.defaultCharset());
                    outputStream.write(pause_msg_bytes);
                    lighttextHandler.sendEmptyMessage(1);
                    temptextHandler.sendEmptyMessage(1);
                    speedtextHandler.sendEmptyMessage(1);
                    pausebuttonHandler.sendEmptyMessage(0);
                    startbuttonHandler.sendEmptyMessage(1);

                    pause_flag = false;
                    break;
                }
                outputStream.write(messageBytes);
                Thread.sleep(10);
                bytes = inputStream.read(buffer);
                System.out.println(buffer);
                System.out.println(bytes);
                receivedString = new String(buffer, 0, bytes);
                String res[] = receivedString.split(",");
                System.out.println(receivedString);
                System.out.println(res.length);
                if (res.length == 0) {
                    continue;
                } else if (res.length == 1) {
                    TempStr = res[0];
                    SpeedStr = "0";
                    LightStr = "OFF";
                    if (res[0].equals("111")) {
                        String pause_msg = "p";
                        byte[] pause_msg_bytes = pause_msg.getBytes(Charset.defaultCharset());
                        outputStream.write(pause_msg_bytes);
                        pausebuttonHandler.sendEmptyMessage(0);
                        startbuttonHandler.sendEmptyMessage(1);
                        break;
                    }
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);
                } else if (res.length == 2) {
                    TempStr = res[0];
                    SpeedStr = res[1];
                    LightStr = "OFF";
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);
                } else if (res.length == 3) {
                    TempStr = res[0];
                    SpeedStr = res[1];
                    if(res[2].equals("1")){
                        LightStr = "On";
                    }else{
                        LightStr = "OFF";
                    }
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);
                }
                Thread.sleep(100);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                break;
                // Handle IOException
            } catch (InterruptedException e) {
                throw new RuntimeException(e);

            }
        }


    }




    private void findAllComponents() {
        bluetoothTextView = findViewById(R.id.bluetoothvalue_id);
        tempTextView = findViewById(R.id.tempvalue_id);
        speedTextView = findViewById(R.id.speedvalue_id);
        lightTextView = findViewById(R.id.light_status_id);
        link_button = findViewById(R.id.button_bluetooth);
        start_button = findViewById(R.id.button_start);
        pause_button = findViewById(R.id.button_pause);
    }

    private void configureAllText() {
        bluetoothtextHandler.sendEmptyMessage(1);
    }
    private void configureBluetooth() {
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            return;
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("JDY31")) {
                mJDY31Device = device;
                return;
            }
        }
        mJDY31Device = null;
        socket = null;

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
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            }

            socket = mJDY31Device.createInsecureRfcommSocketToServiceRecord(JDY31_UUID);
            System.out.println(socket);
            socket.connect();

            Thread.sleep(1000);
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public void configureAllButtons() {
        startbuttonHandler.sendEmptyMessage(0);
        pausebuttonHandler.sendEmptyMessage(0);
        link_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(bluetoothCheckThread!=null&&bluetoothCheckThread.isAlive()){
                    bluetoothCheckThread.interrupt();
                }
                try {
                    searchForJDY31();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                connectToJDY31();
                startbuttonHandler.sendEmptyMessage(1);
                bluetoothbuttonHandler.sendEmptyMessage(0);

                bluetoothCheckThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothCheck();
                    }
                });
                bluetoothCheckThread.start();
//                    bluetoothCheckThread.start();
                // You can now send and receive data over the socket
                // ...
            }

//                Toast toast = Toast.makeText(getApplicationContext(), "Link Successfully!", Toast.LENGTH_SHORT);
//                toast.show();
        });
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startbuttonHandler.sendEmptyMessage(0);
                pausebuttonHandler.sendEmptyMessage(1);
                startSystemThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startSystem();
                    }
                });
                startSystemThread.start();
            }
        });
        pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause_flag = true;
            }
        });

    }
}