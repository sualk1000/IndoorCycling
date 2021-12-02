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
import android.net.Uri;
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

import de.sualk1000.indoorcycler.fitnessequipment.Dialog_ConfigSettings;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;

import java.io.File;

/**
 * Dashboard 'menu' of available sampler activities
 */
public class Activity_IndoorCycling extends FragmentActivity implements ServiceConnection {

    private final static String TAG = Activity_IndoorCycling.class.getSimpleName();

    private Context mContext;
    private IndoorCyclingService indoorCyclingService;
//    private com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult bikePower;

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);




    }
        //Initialize the list
    @SuppressWarnings("serial") //Suppress warnings about hash maps not having custom UIDs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



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
                //((Button) findViewById(R.id.buttonStart)).setEnabled(false);
                //((Button) findViewById(R.id.buttonStop)).setEnabled(true);



                //Toast.makeText(Activity_IndoorCycling.this, "Start", Toast.LENGTH_LONG).show();

            }

        });
        ((Button) findViewById(R.id.buttonStop)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                indoorCyclingService.onStopButton();


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

                Dialog_ConfigSettings dialog = new Dialog_ConfigSettings();
                dialog.show(getFragmentManager(), "Configure User Profile");
            }

        });

        //((TextView) findViewById(R.id.textView_Speed)).setText("");
        //((TextView) findViewById(R.id.textView_Power)).setText("");

        //((TextView) findViewById(R.id.textView_Message)).setText("");
        ((Button) findViewById(R.id.buttonStartScan)).setEnabled(false);


    }
    /*

     */

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
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
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "onServiceDisconnected ");
        indoorCyclingService = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume ");
        Intent intent= new Intent(this, IndoorCyclingService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        registerReceiver(receiver, new IntentFilter(
                IndoorCyclingService.NOTIFICATION));
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
                Log.i(TAG, "onReceive " + command );
                switch(command)
                {
                    case "show_install":
                        showInstall();
                        break;
                    case "ask":
                        askSend();
                        break;
                    case "reques_permission_1":
                        requestPermission1();
                        break;
                    case "reques_permission_2":
                        requestPermission2();
                        break;
                    case "request_bluetooth":
                        requestBluetooth();
                        break;

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
            case "power":
                ((TextView) findViewById(R.id.textView_Power)).setText(text);
                break;
            case "scan":
                ((TextView) findViewById(R.id.buttonStartScan)).setText(text);
                break;
            case "message":
            default:
                ((TextView) findViewById(R.id.textView_Message)).setText(text);
                break;

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
                ((TextView) findViewById(R.id.textView_Message)).setEnabled(status);
                break;

        }
    }

    void requestPermission1()
    {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,}, 1);

    }

    void requestPermission2()
    {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

    }


    Dialog_ProgressWaiter progressDialog = null;
    void showWait(String message)
    {
        if(progressDialog == null)
            progressDialog = new Dialog_ProgressWaiter();
        else
            progressDialog.setStatus(message);


        progressDialog.show(getFragmentManager(), "MyProgressDialog");
    }

    void hideWait()
    {
        if(progressDialog == null)
            return;

        progressDialog.dismiss();

        progressDialog = null;


    }


    void requestBluetooth() {
        Intent enabledIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enabledIntent, 11);
    }
    void showInstall()
    {
        AlertDialog.Builder adlgBldr = new AlertDialog.Builder(
                Activity_IndoorCycling.this);
        adlgBldr.setTitle("Missing Dependency");
        adlgBldr.setMessage("The required service\n\""
                + AntPlusBikePowerPcc.getMissingDependencyName()
                + "\"\n was not found. You need to install the ANT+ Plugins service or"
                + "you may need to update your existing version if you already have it"
                + ". Do you want to launch the Play Store to get it?");
        adlgBldr.setCancelable(true);
        adlgBldr.setPositiveButton("Go to Store", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent startStore = null;
                startStore = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id="
                                + AntPlusBikePowerPcc.getMissingDependencyPackageName()));
                startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                Activity_IndoorCycling.this.startActivity(startStore);
            }
        });
        adlgBldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog waitDialog = adlgBldr.create();
        waitDialog.show();


    }
}