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


public class T100Monitor {

    private final IndoorCyclingService indoorCyclingService;
    //private  SingBroadcastReceiver mReceiver;
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 1;
    public static final int REQUEST_ENABLE_BLUETOOTH = 11;
    List<BluetoothGattService> services;
    public final static UUID UUID_CYCLING_POWER_MEASUREMENT =
            UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb");
    public Integer cyclingPower = 0;

    private BluetoothAdapter bluetoothAdapter;
    //private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private int servicesDiscovered = BluetoothGatt.GATT_FAILURE;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public T100Monitor(IndoorCyclingService indoorCyclingService)
    {
        // this.bluetoothManager = bluetoothManager;
        this.indoorCyclingService = indoorCyclingService;
        BluetoothManager bluetoothManager = (BluetoothManager)indoorCyclingService.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

    }


    private final static String TAG = T100Monitor.class.getSimpleName();

    // Bluetooth



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
                Log.d(TAG, "onConnectionStateChange. Connected to GATT server.");
                // Attempts to discover services after successful connection.
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
                    BluetoothGattCharacteristic cyclingPowerCharacteristic = myservice.getCharacteristic(UUID_CYCLING_POWER_MEASUREMENT);

                    if(cyclingPowerCharacteristic != null) {
                        Log.i(TAG, "CYCLING_POWER_MEASUREMENT found");
                        setCharacteristicNotification(cyclingPowerCharacteristic, true);

                        chararcteristicFound = true;
                    }
                }

                if(!chararcteristicFound)
                {
                    T100Monitor.this.indoorCyclingService.sendTextMessage("Bike Not found on BLE device");

                }



            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         byte[] value, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.d(TAG, "onCharacteristicRead() called");
                broadcastUpdate( characteristic);
            }
        }


        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             byte[] value)
        {
            //Log.d(TAG, "onCharacteristicChanged() called. value changed");
            broadcastUpdate( characteristic);
        }
    };


    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {

        if (UUID_CYCLING_POWER_MEASUREMENT.equals(characteristic.getUuid())) {
            this.cyclingPower = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2) ;
            //Log.v(TAG, String.format("Received cyclingPower: %d W", cyclingPower));
            indoorCyclingService.sendPower( cyclingPower);

        }
    }


    @SuppressLint("MissingPermission")
    public boolean start(final BluetoothDevice device) {
        Log.d(TAG, "GATT client-server connection. Starting connection, connect() called");

        bluetoothGatt = device.connectGatt(indoorCyclingService, false, bluetoothGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        //bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
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

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
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



}