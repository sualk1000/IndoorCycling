package de.sualk1000.indoorcycler;

import android.Manifest;
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
    private  SingBroadcastReceiver mReceiver;
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 1;
    public static final int REQUEST_ENABLE_BLUETOOTH = 11;
    List<BluetoothGattService> services;

    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private int servicesDiscovered = BluetoothGatt.GATT_FAILURE;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public HeartRateMonitor(BluetoothManager bluetoothManager, IndoorCyclingService indoorCyclingService)
    {
       // this.bluetoothManager = bluetoothManager;
        this.indoorCyclingService = indoorCyclingService;
        bluetoothAdapter = bluetoothManager.getAdapter();

        checkBluetoothState();
        if (checkCoarseLocationPermission() == false || bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }else
        {
            startScan();

        }

    }

    public String toString()
    {
        String ret = "";
        if(bluetoothGatt != null)
        {
            ret += "connected=" + this.connectionState;
        }else
        {
            ret += "discovering=" + this.bluetoothAdapter.isDiscovering();
        }
        return ret;
    }
    private boolean checkCoarseLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(indoorCyclingService,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(indoorCyclingService,android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                indoorCyclingService.sendCommand("reques_permission_1");
                return false;
            }
        }

        //checks all needed permissions
        if(ContextCompat.checkSelfPermission(indoorCyclingService, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            indoorCyclingService.sendCommand("reques_permission_2");
            return false;
        }else{
            return true;
        }

    }

    private void checkBluetoothState() {


        //checks if bluetooth is available and if it´s enabled or not
        if(bluetoothAdapter == null){
            indoorCyclingService.sendTextMessage("Bluetooth not available");
        }else{
            if(bluetoothAdapter.isEnabled()){


                if(bluetoothAdapter.isDiscovering()){
                    indoorCyclingService.sendTextMessage("Device is discovering...");
                }else{
                    //indoorCyclingService.sendTextMessage("Bluetooth is enabled");
                }
            }else{
                indoorCyclingService.sendTextMessage("You need to enabled bluetooth");

                indoorCyclingService.sendCommand("request_bluetooth");
            }
        }
    }
    public boolean isDiscovering()
    {
        return bluetoothAdapter.isDiscovering();

    }
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
    void stopScan()
    {
        Log.e(TAG, "Stop Scan.");
        if(bluetoothAdapter.isDiscovering() )
            bluetoothAdapter.cancelDiscovery() ;

        if(mReceiver != null) {
            this.indoorCyclingService.unregisterReceiver(mReceiver);
            mReceiver = null;

        }
    }
    private final static String TAG = HeartRateMonitor.class.getSimpleName();

    // Bluetooth


    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(GattHeartRateAttributes.UUID_HEART_RATE_MEASUREMENT);


    /**
     * Implements callback methods for GATT events that the app cares about.
     * For example, connection change and services discovered.
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                Log.d(TAG, "onConnectionStateChange. Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + bluetoothGatt.discoverServices());

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

                services = getSupportedGattServices();
                for(BluetoothGattService myservice:services)
                {
                    Log.i(TAG, "onServicesDiscovered service: " + myservice.toString());

                    BluetoothGattCharacteristic heartRateCharacteristic = myservice.getCharacteristic(UUID_HEART_RATE_MEASUREMENT);

                    if(heartRateCharacteristic != null) {
                        setCharacteristicNotification(heartRateCharacteristic, true);
                    }
                }

            } else {
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
            Log.v(TAG, String.format("Received heart rate: %d", heartRate));
            // We send the heartRate value to our Server throughout a HTTP post request
        } else {
            // Inform the web server to disconnect
            this.heartRate = 0;
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
            }
        }
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        Log.d(TAG, "GATT client-server connection. Starting connection, connect() called");
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        Log.d(TAG, "GATT client-server connection. Device found, connection ongoing");
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        /**
         * Connect to GATT Server hosted by this device
         * Caller acts as GATT client (BluetoothLeService)
         * The callback is used to deliver results to Caller,
         * such as connection status as well as any further GATT client operations
         * The method returns a BluetoothGatt instance
         * You can use BluetoothGatt to conduct GATT client operations.
         */
        bluetoothGatt = device.connectGatt(indoorCyclingService, false, bluetoothGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.d(TAG, "Releasing Resources. close() called");
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }



    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
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
                    UUID.fromString(GattHeartRateAttributes.UUID_CLIENT_CHARACTERISTIC_CONFIG));
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
    }
}