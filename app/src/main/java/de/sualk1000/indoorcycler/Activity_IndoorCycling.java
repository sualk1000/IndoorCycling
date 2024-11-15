/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
*/

package de.sualk1000.indoorcycler;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class Activity_IndoorCycling extends FragmentActivity implements ServiceConnection {

    private final static String TAG = Activity_IndoorCycling.class.getSimpleName();

    private Context mContext;
    public IndoorCyclingService indoorCyclingService;
//    private com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult bikePower;

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);




    }
        //Initialize the list
    @SuppressWarnings("serial") //Suppress warnings about hash maps not having custom UIDs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);


        UberManager.getInstance().setMainActivity(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContext = getApplicationContext();
        setContentView(R.layout.activity_indoorcycling);

        ((Button) findViewById(R.id.buttonStart)).setEnabled(false);
        ((Button) findViewById(R.id.buttonStop)).setEnabled(false);


        ((Button) findViewById(R.id.buttonStart)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                indoorCyclingService.onStartButton();

            }

        });
        ((Button) findViewById(R.id.buttonStop)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                indoorCyclingService.onStopButton();
                //LineChart  chart = (LineChart) findViewById(R.id.my_chart);

                //chart.getData().notifyDataChanged();
                //chart.notifyDataSetChanged();
                //chart.invalidate();


            }

        });
        ((Button) findViewById(R.id.buttonStartScan)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                indoorCyclingService.onScanButton();

            }

        });

        ((Button) findViewById(R.id.buttonSettings)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showSettings();
            }

        });

        //((TextView) findViewById(R.id.textView_Speed)).setText("");
        //((TextView) findViewById(R.id.textView_Power)).setText("");

        //((TextView) findViewById(R.id.textView_Message)).setText("");
        ((Button) findViewById(R.id.buttonStartScan)).setEnabled(false);

        com.github.mikephil.charting.utils.Utils.init(this);

    }
    /*

     */

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        UberManager.getInstance().cleanup();


        /*
        heartRateMonitor.stopScan();

        if (releaseHandleEquipment != null) {
            releaseHandleEquipment.close();
            releaseHandleEquipment = null;
        }

        stopScan();

        if(timer != null) {
            timer.cancel();
        }*/
    }


//            this.basicResistance += 10;
//            boolean submitted = fePcc.getTrainerMethods().requestSetBasicResistance(new BigDecimal(basicResistance), requestFinishedReceiver);






    DialogFragment dialog;

    void askSend()
    {
        File file = new File(this.getCacheDir(), "MyCache.json");
        if(file.exists() == false)
            return;

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("Title");
        builder1.setMessage("Old Activity found.");
        builder1.setCancelable(true);

        builder1.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                indoorCyclingService.sendPrev(file);

            }
        });
        builder1.setNegativeButton("Delete", new DialogInterface.OnClickListener()     {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();

                file.delete();

            }
        });
        AlertDialog alert11 = builder1.create();
        alert11.show();


    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {

        Log.i(TAG, "onServiceConnected ");
        IndoorCyclingService.MyBinder b = (IndoorCyclingService.MyBinder) binder;
        indoorCyclingService = b.getService();

        if(!indoorCyclingService.serviceStarted ) {
            Intent intent = new Intent(this, IndoorCyclingService.class);
            startService(intent);
            askSend();

        }else
        {
            boolean isSportActivityStarted = indoorCyclingService.isSportActivityStarted();

            ((Button) findViewById(R.id.buttonStart)).setEnabled(!isSportActivityStarted);
            ((Button) findViewById(R.id.buttonStop)).setEnabled(isSportActivityStarted);

        }

        LineChart  hearRateChart = (LineChart) findViewById(R.id.my_chart);
        hearRateChart.getDescription().setEnabled(false);
        hearRateChart.setDefaultFocusHighlightEnabled(false);

        Legend l = hearRateChart.getLegend();
        l.setEnabled(true);


        XAxis xAxis = hearRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("mm:ss");

            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                long millis = TimeUnit.SECONDS.toMillis((long) value);
                return mFormat.format(new Date(millis));
            }
        });

        YAxis leftAxis = hearRateChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(30f);
        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.GREEN);

        YAxis rightAxis = hearRateChart.getAxisRight();
        rightAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        rightAxis.setDrawGridLines(false);
        rightAxis.setGranularityEnabled(true);
        rightAxis.setAxisMinimum(40f);
        rightAxis.setAxisMaximum(50f);
        rightAxis.setYOffset(-9f);
        rightAxis.setTextColor(Color.RED);
        /*
        ArrayList<ILineDataSet> dataSets2 = new ArrayList<>();
        dataSets2.add(indoorCyclingService.powerDataSet);
        dataSets2.add(indoorCyclingService.heartRateDataSet);
        hearRateChart.setData(new LineData(dataSets2));

         */
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(120f);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "onServiceDisconnected ");
        indoorCyclingService = null;
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume ");
        super.onResume();
        Intent intent= new Intent(this, IndoorCyclingService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        registerReceiver(receiver, new IntentFilter(
                IndoorCyclingService.NOTIFICATION));


        LineChart  chart = (LineChart) findViewById(R.id.my_chart);
        if(chart.getData() != null) {

            Log.i(TAG, "onResume chart has data: " + chart.getData().getDataSetCount());
            chart.getData().notifyDataChanged();
        }else {
            Log.i(TAG, "onResume chart has NO data");

        }

        /*
        chart.notifyDataSetChanged();
        chart.invalidate();

         */

        /*
        if(indoorCyclingService != null && indoorCyclingService.powerDataSet != null) {
            ArrayList<ILineDataSet> dataSets2 = new ArrayList<>();
            dataSets2.add(indoorCyclingService.powerDataSet); // add the dat
            dataSets2.add(indoorCyclingService.heartRateDataSet); // add the dat
            chart.setData(new LineData(dataSets2));
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause ");
        unbindService(this);

        unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {


            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String command = bundle.getString("command");
                //Log.i(TAG, "onReceive " + command );
                switch(command)
                {
                    case "ask":
                        askSend();
                        break;
                    case "reques_permission_1":
                        requestPermission1();
                        break;
                    case "reques_permission_2":
                        requestPermission2();
                        break;
                    case "reques_permission_3":
                        requestPermission3();
                        break;
                    /*case "request_bluetooth":
                        requestBluetooth();
                        break;
                    */
                    case "control_status":
                        String control = bundle.getString("control");
                        boolean status = bundle.getBoolean("status");
                        setControlStatus(control,status);
                        break;
                    case "show_wait":
                        String text2 = bundle.getString("text");
                        showWait(text2);
                        break;
                    case "hide_wait":
                        hideWait();
                        break;
                    case "show_settings":
                        showSettings();
                        break;
                    case "control_text":
                        String control2 = bundle.getString("control");
                        String text = bundle.getString("text");
                        setControlText(control2,text);
                        break;
                    case "message":

                        String message = bundle.getString("message");
                        Toast.makeText(Activity_IndoorCycling.this, message, Toast.LENGTH_LONG).show();

                        break;
                }

            }
        }
    };

    private void setControlText(String control2, String text) {

        switch(control2)
        {
            case "speed":
            ((TextView) findViewById(R.id.textView_Speed)).setText(text);
            break;
            case "distance":
                ((TextView) findViewById(R.id.textView_Distance)).setText(text);
                break;
            case "time":
                ((TextView) findViewById(R.id.textView_Time)).setText(text);

                onTimer();


                break;
            case "heartrate":
                ((TextView) findViewById(R.id.textView_HeartRate)).setText(text);
                break;
            case "power":
                ((TextView) findViewById(R.id.textView_Power)).setText(text);
                break;
            case "scan":
                ((TextView) findViewById(R.id.buttonStartScan)).setText(text);
                break;

        }

    }

    void onTimer()
    {
        if(indoorCyclingService.powerDataSet.getValues().size() > 0)
        {


            LineChart  chart = (LineChart) findViewById(R.id.my_chart);
            if(chart.getData() == null) {

                ArrayList<ILineDataSet> dataSets2 = new ArrayList<>();
                dataSets2.add(indoorCyclingService.powerDataSet);
                dataSets2.add(indoorCyclingService.heartRateDataSet);
                chart.setData(new LineData(dataSets2));
            }
            XAxis xAxis = chart.getXAxis();

            int last = indoorCyclingService.heartRateDataSet.getValues().size();
            if(last % 10 == 0 && last >= 120) {
                xAxis.setAxisMinimum(1f * (last-100));
                xAxis.setAxisMaximum(1f * last + 20);

            }

            LineDataSet setHeartRate = (LineDataSet) chart.getData().getDataSetByIndex(1);
            LineDataSet setPower = (LineDataSet) chart.getData().getDataSetByIndex(0);

            Entry power = setPower.getValues().get(last-1);
            YAxis yAxisPower = chart.getAxisLeft();
            if(yAxisPower.getAxisMaximum() <= power.getY())
            {
                float newMax = power.getY() - power.getY() % 10  +10;
                yAxisPower.setAxisMaximum(newMax);
            }
            if(yAxisPower.getAxisMinimum() >= power.getY())
            {
                float newMin = power.getY() - power.getY() % 10  - 10;
                //if(newMin < 0) newMin = 0;
                yAxisPower.setAxisMinimum(newMin);
            }
            Entry heartRate = setHeartRate.getValues().get(last-1);
            YAxis yAxisHeartRate = chart.getAxisRight();
            if(yAxisHeartRate.getAxisMaximum() <= heartRate.getY())
            {
                float newMax = heartRate.getY() - heartRate.getY() % 10 + 10;
                yAxisHeartRate.setAxisMaximum(newMax);
            }
            //set1.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }

    }

    private void setControlStatus(String control, boolean status) {
        switch(control)
        {
            case "start":
                ((TextView) findViewById(R.id.buttonStart)).setEnabled(status);
                break;
            case "stop":
                ((TextView) findViewById(R.id.buttonStop)).setEnabled(status);
                break;
            case "scan":
                ((TextView) findViewById(R.id.buttonStartScan)).setEnabled(status);
                break;
            case "message":
            default:
                ((TextView) findViewById(R.id.textView_Time)).setEnabled(status);
                break;

        }
    }
    void requestPermission3()
    {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}, 1);

    }

    void requestPermission1()
    {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}, 1);

    }

    void requestPermission2()
    {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

    }


    Dialog_ProgressWaiter progressDialog = null;
    void showWait(String message)
    {
        if(progressDialog == null) {
            progressDialog = new Dialog_ProgressWaiter();
            progressDialog.show(getFragmentManager(), "MyProgressDialog");

        }
        progressDialog.setStatus(message);



    }

    void showSettings()
    {

        Dialog_ConfigSettings dialog = new Dialog_ConfigSettings();
        dialog.show(getFragmentManager(), "Configure User Profile");

    }
    void hideWait()
    {
        if(progressDialog == null)
            return;

        progressDialog.dismiss();

        progressDialog = null;


    }



}