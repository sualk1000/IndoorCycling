package de.sualk1000.indoorcycler;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;


import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class IndoorCyclingService extends Service{
    private final static String TAG = IndoorCyclingService.class.getSimpleName();
    public boolean serviceStarted = false;
    private long cumulativeDistance = 0;
    private long lastPowerTimestamp = 0;
    private BikeActivity bikeActivity;
    private HeartRateMonitor heartRateMonitor;
    private T100Monitor t100Monitor;
    private BLEScanner bleScanner = null;
    public static final String NOTIFICATION = IndoorCyclingService.class.getName();
    private int fitness = -1;
    private Timer timer = null;
    long startScan = 0;

    int mStartMode;

    private final IBinder mBinder= new MyBinder();
    boolean mAllowRebind = true;
    LineDataSet heartRateDataSet;
    LineDataSet powerDataSet;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate ");
        bikeActivity = new BikeActivity(this);

        heartRateDataSet = new LineDataSet(new ArrayList<Entry>(), "HeartRate");
        heartRateDataSet.setLineWidth(2.0f);
        heartRateDataSet.setDrawCircles(false);
        heartRateDataSet.setDrawValues(false);
        heartRateDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        heartRateDataSet.setColor(Color.RED);

        powerDataSet = new LineDataSet(new ArrayList<Entry>(), "Power");
        powerDataSet.setLineWidth(2.0f);
        powerDataSet.setDrawCircles(false);
        powerDataSet.setDrawValues(false);
        powerDataSet.setColor(Color.GREEN);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand ");

        serviceStarted = true;
        sendControlStatus("start",false);
        sendControlStatus("stop",false);

        checkPermissions();
        return mStartMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind ");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind ");
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind ");

        if(checkPermissions()== false)
        {
            return;
        }
        if(heartRateMonitor != null && heartRateMonitor.isConnected())
            heartRateMonitor.close();

        heartRateMonitor = null;
        if(t100Monitor != null && t100Monitor.isConnected())
            t100Monitor.close();

        t100Monitor = null;


    }

    boolean checkPermissions()
    {
        if (checkBluetoothScanPermission() == false) {
            sendTextMessage("Need Bluetooth Scan Permission");
            return false;
        }
        if (checkBluetoothConnectPermission() == false) {
            sendTextMessage("Need Bluetooth Scan Permission");
            return false;
        }
        if (checkCoarseLocationPermission() == false) {
            sendTextMessage("Need Coarse Location Permission");
            return false;
        }

        bleScanner = new BLEScanner((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE),this);
        if(bleScanner.checkSettings() == false)
        {
            sendSettings();
        }else {

            if((this.bleScanner.bleHeartbeatDeviceName == null
                    || this.bleScanner.bleHeartbeatDeviceName.equals("")
                    || this.bleScanner.bleHeartbeatDeviceName.equals("<not set>"))
                    && (this.bleScanner.bikeDeviceName == null
                    || this.bleScanner.bikeDeviceName.equals("")
                    || this.bleScanner.bikeDeviceName.equals("<not set>"))
            )
                // No BLE devices set
                sendControlStatus("start",true);
            else
                bleScanner.startScan();
        }

        return true;

    }
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy ");


    }

    public void onStartButton() {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            @SuppressLint("MissingPermission") Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null){
                bikeActivity.lat = (int) (11930465 * locationGPS.getLatitude());
                bikeActivity.lon = (int) (11930465 * locationGPS.getLongitude());
                bikeActivity.altitude = locationGPS.getAltitude();


            }

        }

        if(timer != null)
        {
            sendTextMessage("Already started.");
            return;
        }
        bikeActivity.start();

        timer = new Timer();
        final TimerTask timerTask = new TimerTask() {
            float count = 0;

            @Override
            public void run() {

                count = count + 1f;

                if(IndoorCyclingService.this.bikeActivity.start != null)
                {

                    long time = (new Date().getTime() - bikeActivity.start.getTime()) / 1000;

                    powerDataSet.getValues().add(new Entry(time, (float) (1f * IndoorCyclingService.this.bikeActivity.power)));

                    heartRateDataSet.getValues().add(new Entry(time, 1f * IndoorCyclingService.this.bikeActivity.heartRate));

                    bikeActivity.addMessage();

                }


                sendControlText("time", IndoorCyclingService.this.bikeActivity.getDurationString());
            }
        };

        timer.scheduleAtFixedRate(timerTask, 1000, 1000);

        lastPowerTimestamp = System.currentTimeMillis();
        cumulativeDistance = 0;


    }

    public void onStopButton() {

        if(bikeActivity.isStarted() == false)
        {
            sendTextMessage("Not started.");
            return;
        }
        timer.cancel();
        timer = null;
        bikeActivity.stop();

        cumulativeDistance = 0;
        powerDataSet.getValues().clear();
        powerDataSet.notifyDataSetChanged();
        heartRateDataSet.getValues().clear();
        heartRateDataSet.notifyDataSetChanged();




    }

    public void onScanButton() {
        sendControlStatus("scan",false);
        bleScanner.startScan();

    }

    public boolean isSportActivityStarted() {
        return bikeActivity.isStarted();
    }

    public void sendPrev(File file) {
        bikeActivity.loadMessages(file);
        bikeActivity.send(file);

    }

    public void sendStartHearbeat(BluetoothAdapter bluetoothAdapter, BluetoothDevice bleHeartbeatDevice) {

        if(heartRateMonitor == null)
            heartRateMonitor = new HeartRateMonitor(this);
        heartRateMonitor.start(bleHeartbeatDevice);
        updateScanButtonText();
    }

    public void sendStartBike( BluetoothDevice bikeDevice) {
        if(t100Monitor == null)
            t100Monitor = new T100Monitor(this);
        t100Monitor.start(bikeDevice);

        updateScanButtonText();
    }

   void updateScanButtonText()
   {
       String txt = "Scan";
       if(heartRateMonitor == null)
           txt += " 🔴";
        else
           txt += " 🟢";
        if(t100Monitor == null)
            txt += " 🔴";
        else
            txt += " 🟢";

       sendControlText("scan", txt);

       if(heartRateMonitor != null || t100Monitor != null)
           sendControlStatus("start",true);


   }

   int scan_cycle_counter = -1;
    public void onScanFinished() {


        if(scan_cycle_counter > 1 || scan_cycle_counter < 0)
        {
            scan_cycle_counter = 0;

            if((this.bleScanner.bleHeartbeatDeviceName != null
                    && this.bleScanner.bleHeartbeatDeviceName.equals("") == false
                    && heartRateMonitor == null) || (this.bleScanner.bikeDeviceName != null
                    && this.bleScanner.bikeDeviceName.equals("") == false
                    && this.t100Monitor == null)
            ) {
                sendControlStatus("scan", true);
                sendControlStatus("start", true);
            }
            return;
        }
        scan_cycle_counter ++;
        if(this.bleScanner.bleHeartbeatDeviceName != null
                && this.bleScanner.bleHeartbeatDeviceName.equals("") == false
                && heartRateMonitor == null)
        {
            bleScanner.startScan();
            return;
        }
        if(this.bleScanner.bikeDeviceName != null
                && this.bleScanner.bikeDeviceName.equals("") == false
                && this.t100Monitor == null)
        {
            bleScanner.startScan();
            return;
        }

    }


    public class MyBinder extends Binder {
        IndoorCyclingService getService() {
            return IndoorCyclingService.this;
        }
    }


    void sendCommand(String command)
    {
        sendCommand(command,null);
    }
    void sendCommand(String command,String text)
    {
        Log.i(TAG, "sendCommand " + command);

        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", command);
        if(text != null)
            intent.putExtra("text", command);
        this.sendBroadcast(intent);
    }

    public void sendTextMessage(String message)
    {
        Log.i(TAG, "sendTextMessage " + message);
        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "message");
        intent.putExtra("message", message);
        this.sendBroadcast(intent);
    }
    void sendPower(int cyclingPower)
    {

        BigDecimal bdCyclingPower = new BigDecimal(cyclingPower);

        double luftdichte = 1.2;
        double cw = 0.39;
        //double speed = Math.sqrt( bdCyclingPower.doubleValue() / ( luftdichte * cw )) ;
        double speed = Math.pow( bdCyclingPower.doubleValue() * 0.9 /* factor for roll force */ * 2.0 / ( luftdichte * cw ) , 1.0 / 3.0) ;


        BigDecimal bdSpeed = new BigDecimal(speed);
        bikeActivity.setSpeed(bdSpeed);
        long duration = (System.currentTimeMillis() - lastPowerTimestamp) / 1000;
        if(lastPowerTimestamp == 0)
        {
            lastPowerTimestamp = System.currentTimeMillis();

        }else if(duration > 0 && bdSpeed.intValue() > 0) {
            double distance = speed * duration;

            cumulativeDistance += (long) distance;
            bikeActivity.setDistance(cumulativeDistance);

            Log.i(TAG, "sendPower duration:" + duration + " distance:" + distance + " speed:" + speed + " power:" + cyclingPower);
            lastPowerTimestamp = System.currentTimeMillis();


        }
        bikeActivity.setPower(bdCyclingPower);


        sendControlText("speed", IndoorCyclingService.this.bikeActivity.getSpeedString());
        sendControlText("power", IndoorCyclingService.this.bikeActivity.getPowerString() );
        sendControlText("distance", IndoorCyclingService.this.bikeActivity.getDistanceString());

    }
    void sendHeartRate(int heartRate)
    {
        bikeActivity.setHeartRate(heartRate);
        sendControlText("heartrate", IndoorCyclingService.this.bikeActivity.getHeartRateString());


    }

    void sendControlText(String control,String text)
    {
        //Log.i(TAG, "sendControlText " + control + " " + text);

        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "control_text");
        intent.putExtra("control", control);
        intent.putExtra("text", text);
        this.sendBroadcast(intent);
    }
    void sendControlStatus(String control,boolean status)
    {
        Log.i(TAG, "sendControlStatus " + control + " " + status);
        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "control_status");
        intent.putExtra("control", control);
        intent.putExtra("status", status);
        this.sendBroadcast(intent);
    }


    void sendShowWait(String text)
    {

        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "show_wait");
        intent.putExtra("text", text);
        this.sendBroadcast(intent);
    }
    void sendHideWait()
    {

        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "hide_wait");
        this.sendBroadcast(intent);
    }

    void sendSettings()
    {

        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "show_settings");
        this.sendBroadcast(intent);
    }
    private boolean checkBluetoothScanPermission() {


        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED){
            sendCommand("reques_permission_1");
            return false;
        }else {
                return true;
        }


    }
    private boolean checkBluetoothConnectPermission() {


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            sendCommand("reques_permission_3");
            return false;
        }else {
            return true;
        }


    }
    private boolean checkCoarseLocationPermission() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            sendCommand("reques_permission_2");
            return false;
        }else{
            return true;
        }

    }

}
