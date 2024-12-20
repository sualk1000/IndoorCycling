package de.sualk1000.indoorcycler;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class BLEScanner {

    private final IndoorCyclingService indoorCyclingService;

    public final ArrayList<String> bleItemList = new ArrayList<String>();

    public Dialog_BLEScanner.StableArrayAdapter bleItemAdapter;
    private  SingBroadcastReceiver mReceiver;

    private BluetoothAdapter bluetoothAdapter;
    String bleHeartbeatDeviceName;
    String bikeDeviceName;

    private String use_garmin;
    private String garmin_user;
    private String garmin_pwd;
    private BluetoothDevice bleHeartbeatDevice;
    private BluetoothDevice bleBikeDevice;


    private static BLEScanner instance = null;

    public static BLEScanner getInstance()
    {
        return instance;
    }
    public BLEScanner(BluetoothManager bluetoothManager, IndoorCyclingService indoorCyclingService)
    {
        if(instance != null)
            throw new IllegalArgumentException("BLEScanner already exists");
        this.indoorCyclingService = indoorCyclingService;
        bluetoothAdapter = bluetoothManager.getAdapter();

        instance = this;
        if (checkBluetoothState() == false ) {
            throw new IllegalArgumentException("Invalid BLE State");

        }

    }



    @SuppressLint("MissingPermission")
    private boolean checkBluetoothState() {


        //checks if bluetooth is available and if it´s enabled or not
        if(bluetoothAdapter == null){
            indoorCyclingService.sendTextMessage("Bluetooth not available");
        }else{
            if(bluetoothAdapter.isEnabled()){


                if(bluetoothAdapter.isDiscovering()){
                    indoorCyclingService.sendTextMessage("Device is discovering...");
                }else{
                    //indoorCyclingService.sendTextMessage("Bluetooth is enabled");
                    return true;
                }
            }else{
                indoorCyclingService.sendTextMessage("You need to enabled bluetooth");

            }
        }
        return false;
    }
    @SuppressLint("MissingPermission")
    public boolean isDiscovering()
    {
        return bluetoothAdapter.isDiscovering();

    }
    @SuppressLint("MissingPermission")
    public void startScan()
    {
        Log.e(TAG, "Start Scan.");
        if(mReceiver == null) {
            this.mReceiver = new SingBroadcastReceiver();

            this.indoorCyclingService.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            this.indoorCyclingService.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
            this.indoorCyclingService.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        }
        if(bluetoothAdapter.isDiscovering() )
            bluetoothAdapter.cancelDiscovery() ;

        bluetoothAdapter.startDiscovery() ;
    }
    @SuppressLint("MissingPermission")
    void stopScan(boolean notifyService)
    {
        Log.e(TAG, "Stop Scan.");
        if(bluetoothAdapter.isDiscovering() )
            bluetoothAdapter.cancelDiscovery() ;

        if(mReceiver != null) {
            this.indoorCyclingService.unregisterReceiver(mReceiver);
            mReceiver = null;

        }
        if(notifyService)
            this.indoorCyclingService.onScanFinished();
    }
    private final static String TAG = BLEScanner.class.getSimpleName();

    public boolean checkSettings() {

        SharedPreferences sharedPreferences = indoorCyclingService.getSharedPreferences(indoorCyclingService.getApplicationContext().getPackageName() + ".settings", Context.MODE_PRIVATE);

        bleHeartbeatDeviceName = sharedPreferences.getString("heartbeat", "");
        bikeDeviceName = sharedPreferences.getString("bike", "");

        if(bleHeartbeatDeviceName.equals(""))
            return false;
        if(bikeDeviceName.equals(""))
            return false;
        use_garmin = sharedPreferences.getString("use_garmin", "");
        garmin_user = sharedPreferences.getString("garmin_user", "");

        if(use_garmin.equals("true") && garmin_user.equals(""))
            return false;

        return true;
    }









    private class SingBroadcastReceiver extends BroadcastReceiver {

        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //may need to chain this to a recognizing function
            Log.d(TAG,"onReceive " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getName() != null) {
                    String derp = device.getName() + " - " + device.getAddress();
                    Log.d(TAG, "Device " + derp);
                    if (bleItemList != null && bleItemList.contains(device.getName()) == false){
                        bleItemList.add(device.getName());
                        if(bleItemAdapter != null)
                            bleItemAdapter.notifyDataSetChanged();
                    }
                    if (bleHeartbeatDeviceName != null && device.getName().equals(bleHeartbeatDeviceName)) {
                        bleHeartbeatDevice = device;
                        indoorCyclingService.sendStartHearbeat(bluetoothAdapter,bleHeartbeatDevice);
                    }

                    if (bikeDeviceName != null && device.getName().equals(bikeDeviceName)) {
                        bleBikeDevice = device;
                        indoorCyclingService.sendStartBike(bleBikeDevice);
                    }
                }else
                {
                    String derp = " - " + device.getAddress();
                    Log.d(TAG, "Device " + derp);

                }
                //Toast.makeText(context, derp, Toast.LENGTH_LONG);

                if(bleHeartbeatDevice != null && bleBikeDevice != null)
                {
                    Log.d(TAG, "All Devices found");
                    stopScan(true);
                }

            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //report user
                Log.d(TAG,"Discovery Started");

                bleItemList.clear();
                bleItemList.add("<not set>");
             }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"Discovery Finished");
                stopScan(true);
            }

        }
    }
}