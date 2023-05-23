package com.example.anroid_gui;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.Line;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;

    private static final int PERMISSION_REQUEST_CODE = 2;

    private static final int PERMISSION_REQUEST_CODE1 = 3;

    private static final int CHART_TEMP = 0;

    private static final int CHART_SPEED = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private static final UUID JDY31_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice mJDY31Device;

    private static final int REQUEST_BLUETOOTH_PERMISSION = 3;
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

    private Handler LineChartsHandler = null;
    private BluetoothSocket socket = null;


    private String TempStr = null;

    private String SpeedStr = null;

    private String LightStr = null;

    private boolean pause_flag = false;
    private BroadcastReceiver bluetoothReceiver = null;

    private ArrayList<Float> heat_temp_array = null;

    private ArrayList<Integer> speed_array = null;

    private LineChart mLineChart1 = null;

    private LineChart mLineChart2 = null;

    private PdfDocument mPdfDocument = null;

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
        CheckPermissions();
        heat_temp_array = new ArrayList<>();
        speed_array = new ArrayList<>();
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with file operations

            } else {
                // Permission denied, handle the scenario
                Toast.makeText(this, "Permission denied. Cannot create file.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == PERMISSION_REQUEST_CODE1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                // Permission granted, proceed with file operations

            } else {
                // Permission denied, handle the scenario
                Toast.makeText(this, "Permission denied. Cannot create file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isFloat(String value) {
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void CheckPermissions() {

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permissions
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            // Request the MANAGE_EXTERNAL_STORAGE permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        }
    }

    private void BluetoothCheck() {
        while (true) {
            if (socket == null || !socket.isConnected()) {
                if (startSystemThread != null && startSystemThread.isAlive()) {
                    pause_flag = true;
                }
                bluetoothtextHandler.sendEmptyMessage(1);
                bluetoothbuttonHandler.sendEmptyMessage(1);
                startbuttonHandler.sendEmptyMessage(0);
                pausebuttonHandler.sendEmptyMessage(0);
                break;
            } else if (!mBluetoothAdapter.isEnabled()) {
                if (startSystemThread != null && startSystemThread.isAlive()) {
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
                        return;
                    }
                    if (device.getName().equals("HC-05")) {

                        if (startSystemThread != null) {
                            if (startSystemThread.isAlive()) {
                                try {
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    if (socket != null && socket.isConnected()) {
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
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void configureAllHandler() {
        AllToastHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    Toast.makeText(MainActivity.this, "Please switch on the phone's bluetooth", Toast.LENGTH_SHORT).show();
                } else if (msg.what == 1) {
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
                } else if (msg.what == 1) {
                    tempTextView.setText("");
                }
            }
        };

        speedtextHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    speedTextView.setText(SpeedStr);
                } else if (msg.what == 1) {
                    speedTextView.setText("");
                }
            }
        };

        lighttextHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    lightTextView.setText(LightStr);
                } else if (msg.what == 1) {
                    lightTextView.setText("");
                }
            }
        };

        pausebuttonHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    pause_button.setEnabled(false);
                } else if (msg.what == 1) {
                    pause_button.setEnabled(true);
                }
            }
        };

        startbuttonHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    start_button.setEnabled(false);
                } else if (msg.what == 1) {
                    start_button.setEnabled(true);
                }
            }
        };

        LineChartsHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    generateDataFile();
                }
            }
        };

    }


    private void startSystem() {
        pause_flag = false;
        heat_temp_array.clear();
        speed_array.clear();
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
                if (pause_flag) {
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
                        temptextHandler.sendEmptyMessage(1);
                        speedtextHandler.sendEmptyMessage(1);
                        lighttextHandler.sendEmptyMessage(1);
                        LineChartsHandler.sendEmptyMessage(0);
                        break;
                    }
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);
                } else if (res.length == 2) {


                    if (isFloat(res[0])) {
                        TempStr = res[0];
                        heat_temp_array.add(Float.parseFloat(res[0]));
                    }
                    if(isInteger(res[1])){
                        SpeedStr = res[1];
                        if(!res[1].equals("0")){
                            speed_array.add(Integer.parseInt(res[1]));
                        }
                    }
                    if (!LightStr.equals("ON")) {
                        LightStr = "OFF";
                    }
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);
                } else if (res.length == 3) {
                    if (isFloat(res[0])) {
                        TempStr = res[0];
                        heat_temp_array.add(Float.parseFloat(TempStr));
                    }

                    if (!res[1].equals("0")) {
                        if (isInteger(SpeedStr)) {
                            SpeedStr = res[1];
                            speed_array.add(Integer.parseInt(SpeedStr));
                        }
                    }
                    if (res[2].equals("1")) {
                        LightStr = "ON";
                    } else {
                        if (!LightStr.equals("ON")) {
                            LightStr = "OFF";
                        }

                    }
                    temptextHandler.sendEmptyMessage(0);
                    speedtextHandler.sendEmptyMessage(0);
                    lighttextHandler.sendEmptyMessage(0);
                }
                Thread.sleep(1000);
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            return;
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("HC-05")) {
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
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            }
            socket = mJDY31Device.createInsecureRfcommSocketToServiceRecord(JDY31_UUID);
            System.out.println(socket);
            socket.connect();
            Thread.sleep(1000);
            bluetoothTextView.clearComposingText();
            bluetoothtextHandler.sendEmptyMessage(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
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
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }else {
                    if (bluetoothCheckThread != null && bluetoothCheckThread.isAlive()) {
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
            }

//                Toast toast = Toast.makeText(getApplicationContext(), "Link Successfully!", Toast.LENGTH_SHORT);
//                toast.show();
        });
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startbuttonHandler.sendEmptyMessage(0);
                pausebuttonHandler.sendEmptyMessage(1);
                LightStr = "OFF";
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
    private void generateDataFile(){
        ArrayList<Float> floatList = new ArrayList<>();
        for (Integer intValue : speed_array) {
            float floatValue = intValue.floatValue(); // Convert Integer to double
            floatList.add(floatValue);
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_line_charts, null);
        mLineChart1 = createLineChart(floatList, "Motor Speed", 0, dialogView);
        mLineChart2 = createLineChart(heat_temp_array, "Heating Temperature", 1, dialogView);
        drawLineCharts();
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText name_input = dialogView.findViewById(R.id.image_namevalue_id);

        dialogBuilder.setView(dialogView);
        dialogBuilder.setPositiveButton("Save",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(name_input.getText().equals("")){
                    Toast.makeText(MainActivity.this, "Please input the name of the image", Toast.LENGTH_SHORT).show();
                }else{
                    int height = mLineChart1.getHeight() + mLineChart2.getHeight();
                    int width = Math.max(mLineChart1.getWidth(), mLineChart2.getWidth());
                    Bitmap combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(combinedBitmap);
                    // 绘制第一个折线图
                    mLineChart2.draw(canvas);
                    // 将画布平移至第二个折线图的起始位置
                    canvas.translate(0, mLineChart2.getHeight());
                    // 绘制第二个折线图
                    mLineChart1.draw(canvas);

                    // 保存Bitmap为图片文件
                    saveBitmapToImage(combinedBitmap,name_input.getText().toString());
                }

            }
        });

        dialogBuilder.setNegativeButton("Cancel",new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        // 创建并显示弹窗
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

    }
    private LineChart createLineChart(ArrayList<Float> data, String label, int flag, View view) {
        LineChart lineChart = null;
        if(flag == 1){
            lineChart = view.findViewById(R.id.lineChart1);
        }else{
            lineChart = view.findViewById(R.id.lineChart2);
        }
        // 设置 LineChart 的属性
        lineChart.setNoDataText("No data available");
        lineChart.getDescription().setText(label);
        // 创建 Entry 列表
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }
        // 创建 LineDataSet
        LineDataSet dataSet = new LineDataSet(entries, label);
        if(flag==1){
            dataSet.setColor(Color.RED);
        }else{
            dataSet.setColor(Color.GREEN);
        }
        // 设置 LineDataSet 的属性
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        // 创建 LineData
        LineData lineData = new LineData(dataSet);
        // 设置 LineChart 的数据
        lineChart.setData(lineData);
        System.out.println(lineChart);
        return lineChart;
    }
    private void drawLineCharts() {
        // 创建一个临时文件来存储PDF文件
        File file = new File(getCacheDir(), "chart.pdf");
        try {
            // 创建PDF文档对象
            PdfDocument pdfDocument = new PdfDocument();

            // 创建页面配置
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 800, 1).create();

            // 开始新页面
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            // 获取页面的画布
            Canvas canvas = page.getCanvas();

            // 在画布上绘制第一个折线图
            mLineChart1.measure(canvas.getWidth(), canvas.getHeight());
            mLineChart1.layout(0, 0, canvas.getWidth(), canvas.getHeight());
            mLineChart1.draw(canvas);

            // 在画布上绘制第二个折线图
            mLineChart2.measure(canvas.getWidth(), canvas.getHeight());
            mLineChart2.layout(0, mLineChart1.getHeight(), canvas.getWidth(), mLineChart1.getHeight() + canvas.getHeight());
            mLineChart2.draw(canvas);

            // 结束页面
            pdfDocument.finishPage(page);

            // 将PDF文件写入临时文件
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            pdfDocument.writeTo(fileOutputStream);
            fileOutputStream.close();

            // 关闭PDF文档
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void saveBitmapToImage(Bitmap bitmap, String fileName) {
        try {
            // 创建保存图片的文件
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            fileName = fileName + ".png";
            File file = new File(directory, fileName);
            // 将Bitmap保存为图片文件
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            // 将图片添加到相册
            addImageToGallery(file.getAbsolutePath(), this);
            Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void addImageToGallery(String imagePath, Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, imagePath);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }




}