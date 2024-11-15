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
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;


import androidx.core.content.ContextCompat;

import java.util.List;
        import java.util.UUID;


public class HeartRateMonitor {

    private final IndoorCyclingService indoorCyclingService;
    //private  SingBroadcastReceiver mReceiver;
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 1;
    public static final int REQUEST_ENABLE_BLUETOOTH = 11;
    List<BluetoothGattService> services;

    private BluetoothAdapter bluetoothAdapter;
    //private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private int servicesDiscovered = BluetoothGatt.GATT_FAILURE;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public HeartRateMonitor(IndoorCyclingService indoorCyclingService)
    {
       // this.bluetoothManager = bluetoothManager;
        this.indoorCyclingService = indoorCyclingService;
        BluetoothManager bluetoothManager = (BluetoothManager)indoorCyclingService.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

    }


    private final static String TAG = HeartRateMonitor.class.getSimpleName();

    // Bluetooth


    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");


    /**
     * Implements callback methods for GATT events that the app cares about.
     * For example, connection change and services discovered.
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                Log.d(TAG, "Attempting to start service discovery:" + gatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered received: " + status);
            servicesDiscovered = status;
            if (status == BluetoothGatt.GATT_SUCCESS) {

                boolean chararcteristicFound = false;
                services = getSupportedGattServices();
                for(BluetoothGattService myservice:services)
                {
                    Log.i(TAG, "onServicesDiscovered service: " + myservice.toString());

                    BluetoothGattCharacteristic heartRateCharacteristic = myservice.getCharacteristic(UUID_HEART_RATE_MEASUREMENT);

                    if(heartRateCharacteristic != null) {
                        setCharacteristicNotification(heartRateCharacteristic, true);
                        chararcteristicFound = true;
                        Log.i(TAG, "HEART_RATE_MEASUREMENT found");
                    }
                }

                if(!chararcteristicFound)
                {
                    HeartRateMonitor.this.indoorCyclingService.sendTextMessage("Heartrate Not found on BLE device");

                }



            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead() called");
                broadcastUpdate( characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //Log.d(TAG, "onCharacteristicChanged() called. Heart rate value changed");
            broadcastUpdate( characteristic);
        }
    };
    public Integer heartRate = 0;



    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                //Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                //Log.d(TAG, "Heart rate format UINT8.");
            }
            this.heartRate = characteristic.getIntValue(format, 1);
            //Log.v(TAG, String.format("Received heart rate: %d", heartRate));

            indoorCyclingService.sendHeartRate(heartRate);
            // We send the heartRate value to our Server throughout a HTTP post request
        }
    }


    @SuppressLint("MissingPermission")
    public boolean start(final BluetoothDevice device) {
        Log.d(TAG, "start GATT");

        bluetoothGatt = device.connectGatt(indoorCyclingService, false, bluetoothGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        //bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        Log.w(TAG, "disconnect");
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        Log.d(TAG, "Releasing Resources. close() called");
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }



    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }


    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        // We access the descriptor 'Client Characteristic Configuration' to set the notification flag to 'enabled'
        // Thereby, HEART_RATE_MEASUREMENT characteristic is able to send notifications, whenever the data underlying changes
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    public boolean isConnected() {
        return connectionState == STATE_CONNECTED;
    }



    /*
    private class SingBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //may need to chain this to a recognizing function
            Log.d(TAG,"onReceive " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a Toast
                String derp = device.getName() + " - " + device.getAddress();

                Log.d(TAG,"Device " + derp);
                //Toast.makeText(context, derp, Toast.LENGTH_LONG);

                if(device.getName() != null && device.getName().startsWith("Polar H7"))
                {
                    stopScan();
                    connect(device.getAddress());
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //report user
                Log.d(TAG,"Started");
             }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"Finished");
                stopScan();
            }

        }
    }*/
}