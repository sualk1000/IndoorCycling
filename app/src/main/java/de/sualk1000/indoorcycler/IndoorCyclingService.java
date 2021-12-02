package de.sualk1000.indoorcycler;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc;
import com.dsi.ant.plugins.antplus.pcc.MultiDeviceSearch;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestStatus;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.io.File;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

public class IndoorCyclingService extends Service{
    private final static String TAG = IndoorCyclingService.class.getSimpleName();
    public boolean serviceStarted = false;
    private long cumulativeDistance;
    private BikeActivity bikeActivity;
    private HeartRateMonitor heartRateMonitor;
    private PccReleaseHandle<AntPlusFitnessEquipmentPcc> releaseHandleEquipment;
    AntPlusFitnessEquipmentPcc fePcc = null;

    public static final String NOTIFICATION = IndoorCyclingService.class.getName();
    private int fitness = -1;
    private Timer timer = null;
    long startScan = 0;
    MultiDeviceSearch mSearch;

    int mStartMode;

    private final IBinder mBinder= new MyBinder();
    boolean mAllowRebind = true;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate ");

        bikeActivity = new BikeActivity(this);

    }
    private TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {


            boolean enableScan = false;
            if(fePcc == null && mSearch == null)
            {
                enableScan = true;
            }

            if(heartRateMonitor.isConnected() == false && heartRateMonitor.isDiscovering() == false) {
                enableScan = true;
            }
            if(mSearch != null  && (System.currentTimeMillis() - startScan) > 30 * 1000)
            {
                stopScan();
            }

            if(mSearch != null)
                enableScan = false;


            Log.i(TAG, "timerTask run mSearch=" + mSearch
                    + " heartRateMonitor=" + heartRateMonitor
                    + " fePcc=" +fePcc
                    + " enableScan=" + enableScan);

            IndoorCyclingService.this.sendControlStatus("scan",enableScan);



        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand ");

        serviceStarted = true;
        startScan();
        this.heartRateMonitor = new HeartRateMonitor((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE),this);

        if(releaseHandleEquipment == null) {
            startScan();
        }
        //    askSend();


        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1000, 2000);

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

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy ");
        if (releaseHandleEquipment != null) {
            releaseHandleEquipment.close();
            releaseHandleEquipment = null;
        }

        stopScan();

    }

    public void onStartButton() {
        stopScan();
        bikeActivity.start();

    }

    public void onStopButton() {

        bikeActivity.stop();



    }

    public void onScanButton() {
        if(heartRateMonitor.isConnected() == false && heartRateMonitor.isDiscovering() == false)
            heartRateMonitor.startScan();

        if(mSearch == null )
        {
            startScan();
        }

    }

    public boolean isSportActivityStarted() {
        return bikeActivity.isStarted();
    }

    public void sendPrev(File file) {
        bikeActivity.loadMessages(file);
        bikeActivity.send(file);

    }

    public class MyBinder extends Binder {
        IndoorCyclingService getService() {
            return IndoorCyclingService.this;
        }
    }

    void stopScan() {
        Log.i(TAG, "stopScan ");
        if (mSearch != null)
            mSearch.close();
        mSearch = null;

    }
    void startScan() {
        EnumSet<DeviceType> devices = EnumSet.noneOf(DeviceType.class);
        //devices.add(DeviceType.BIKE_POWER);
        //devices.add(DeviceType.ENVIRONMENT);
        //devices.add(DeviceType.HEARTRATE);
        //devices.add(DeviceType.BIKE_SPDCAD);
        devices.add(DeviceType.BIKE_CADENCE);
        devices.add(DeviceType.FITNESS_EQUIPMENT);


        mSearch = new MultiDeviceSearch(this, devices, mCallback, mRssiCallback);

        startScan = System.currentTimeMillis();
    }
    AntPlusCommonPcc.IRequestFinishedReceiver requestFinishedReceiver = new AntPlusCommonPcc.IRequestFinishedReceiver(){
        @Override
        public void onNewRequestFinished(RequestStatus requestStatus) {
            sendTextMessage("onNewRequestFinished: " + requestStatus.name());
        }
    };
    private MultiDeviceSearch.SearchCallbacks mCallback = new MultiDeviceSearch.SearchCallbacks() {
        /**
         * Called when a device is found. Display found devices in connected and
         * found lists
         */
        public void onDeviceFound(final com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult deviceFound) {
            Log.i(TAG, "onDeviceFound " + deviceFound.getDeviceDisplayName() + " " + deviceFound.getAntDeviceType().name() + " " + deviceFound.getAntDeviceNumber());
            switch (deviceFound.getAntDeviceType()) {
                case BIKE_POWER:
                    // bikePower = deviceFound;
                    break;
                case FITNESS_EQUIPMENT:
                    fitness = deviceFound.getAntDeviceNumber();
                    releaseHandleEquipment = AntPlusFitnessEquipmentPcc.requestNewOpenAccess(IndoorCyclingService.this,
                            fitness, 0, mFitnessEquipmentResultReceiver,
                            mDeviceStateChangeReceiver, mFitnessEquipmentStateReceiver);

                    break;
                case BIKE_SPD:
                case BIKE_SPDCAD:
                case ENVIRONMENT:
                case HEARTRATE:
                case BIKE_CADENCE:
                case UNKNOWN:
                    break;
                default:
                    break;
            }



        }

        /**
         * The search has been stopped unexpectedly
         */
        public void onSearchStopped(RequestAccessResult reason) {
            Log.i(TAG, "onSearchStopped " + reason.name());
            sendControlText("message", "Search stopped");


        }

        @Override
        public void onSearchStarted(MultiDeviceSearch.RssiSupport supportsRssi) {
            Log.i(TAG, "onSearchStarted ");
            sendControlText("message", "Search started");

            if (supportsRssi == MultiDeviceSearch.RssiSupport.UNAVAILABLE) {

                sendTextMessage("Rssi information not available.");
            } else if (supportsRssi == MultiDeviceSearch.RssiSupport.UNKNOWN_OLDSERVICE) {
                sendTextMessage("Rssi might be supported. Please upgrade the plugin service.");
            }
        }
    };

    /**
     * Callback for RSSI data of previously found devices
     */
    private MultiDeviceSearch.RssiCallback mRssiCallback = new MultiDeviceSearch.RssiCallback() {
        /**
         * Receive an RSSI data update from a specific found device
         */
        @Override
        public void onRssiUpdate(final int resultId, final int rssi) {

            Log.i(TAG, "onRssiUpdate " + resultId + " " + rssi);
        }
    };

    void onMyResultReceived(AntPlusCommonPcc result,
                          RequestAccessResult resultCode, DeviceState initialDeviceState) {
        Log.i(TAG, "onResultReceived " + resultCode);
        switch (resultCode) {
            case SUCCESS:
                Log.i(TAG, "onResultReceived " + result.getDeviceName() + " " + resultCode);
                fePcc = (AntPlusFitnessEquipmentPcc) result;
                subscribeToEvents();
                stopScan();
                sendControlStatus("start",true);
                sendControlStatus("stop",false);
                break;
            case CHANNEL_NOT_AVAILABLE:

                sendTextMessage("Channel Not Available");

                break;
            case ADAPTER_NOT_DETECTED:
                sendTextMessage("ANT Adapter Not Available. Built-in ANT hardware or external adapter required.");

                break;
            case BAD_PARAMS:
                sendTextMessage("Bad request parameters.");
                break;
            case OTHER_FAILURE:
                sendTextMessage("RequestAccess failed. See logcat for details.");
                break;
            case DEPENDENCY_NOT_INSTALLED:
                break;
            case USER_CANCELLED:
                break;
            case UNRECOGNIZED:
                sendTextMessage("PluginLib Upgrade Required?" + resultCode);
                break;
            default:
                sendTextMessage("Unrecognized result: " + resultCode);
                break;
        }

    }


    AntPluginPcc.IPluginAccessResultReceiver<AntPlusFitnessEquipmentPcc> mFitnessEquipmentResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusFitnessEquipmentPcc>() {
        @Override
        public void onResultReceived(AntPlusFitnessEquipmentPcc result,
                                     RequestAccessResult resultCode, DeviceState initialDeviceState) {

            onMyResultReceived(result,
                    resultCode, initialDeviceState);
        }
    };
    AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> mBikePowerResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
        @Override
        public void onResultReceived(AntPlusBikePowerPcc result,
                                     RequestAccessResult resultCode, DeviceState initialDeviceState) {

            onMyResultReceived(result,
                    resultCode, initialDeviceState);
        }
    };

    AntPluginPcc.IDeviceStateChangeReceiver mDeviceStateChangeReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {
        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState) {

            sendTextMessage("New State " + newDeviceState.toString());

            if(newDeviceState.equals(DeviceState.DEAD))
            {
                releaseHandleEquipment = null;
                fePcc = null;
                //startScan();
            }
        }
    };
    AntPlusFitnessEquipmentPcc.IFitnessEquipmentStateReceiver mFitnessEquipmentStateReceiver =
            new AntPlusFitnessEquipmentPcc.IFitnessEquipmentStateReceiver() {
                @Override
                public void onNewFitnessEquipmentState(final long estTimestamp,
                                                       EnumSet<EventFlag> eventFlags, final AntPlusFitnessEquipmentPcc.EquipmentType equipmentType,
                                                       final AntPlusFitnessEquipmentPcc.EquipmentState equipmentState) {

                            switch(equipmentType) {
                                case GENERAL:
                                case TREADMILL:
                                case ELLIPTICAL:
                                case BIKE:
                            }

                            if(estTimestamp != -1)
                                return;
                            /*
                            switch(equipmentState)
                            {
                                case ASLEEP_OFF:
                                    ((TextView) findViewById(R.id.textView_Speed)).setText("OFF");
                                    break;
                                case READY:
                                    ((TextView) findViewById(R.id.textView_Speed)).setText("READY");
                                    break;
                                case IN_USE:
                                    ((TextView) findViewById(R.id.textView_Speed)).setText("IN USE");
                                    break;
                                case FINISHED_PAUSED:
                                    ((TextView) findViewById(R.id.textView_Speed)).setText("FINISHED/PAUSE");
                                    break;
                                case UNRECOGNIZED:
                                    Toast.makeText(IndoorCyclingService.this,
                                            "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                            Toast.LENGTH_SHORT).show();
                                default:
                                    ((TextView) findViewById(R.id.textView_Speed)).setText("INVALID: " + equipmentState);
                            }*/
                    sendTextMessage("Equipment State = " + equipmentState.name());


                }
            };


    private void subscribeToEvents() {
        fePcc.subscribeGeneralFitnessEquipmentDataEvent(new AntPlusFitnessEquipmentPcc.IGeneralFitnessEquipmentDataReceiver() {
            @Override
            public void onNewGeneralFitnessEquipmentData(final long estTimestamp,
                                                         EnumSet<EventFlag> eventFlags, final BigDecimal elapsedTime,
                                                         final long cumulativeDistance, final BigDecimal instantaneousSpeed,
                                                         final boolean virtualInstantaneousSpeed, final int instantaneousHeartRate,
                                                         final AntPlusFitnessEquipmentPcc.HeartRateDataSource heartRateDataSource) {

                IndoorCyclingService.this.cumulativeDistance = cumulativeDistance;

                        if (instantaneousSpeed.intValue() == -1)
                            sendControlText("speed", "Invalid");
                        else {
                            sendControlText("speed", String.valueOf(instantaneousSpeed.intValue() * 3.6) + " km/h");
                            IndoorCyclingService.this.bikeActivity.setSpeed(instantaneousSpeed);
                        }
                        if (cumulativeDistance != -1)
                        {
                            IndoorCyclingService.this.bikeActivity.setDistance(cumulativeDistance);
                        }



                        bikeActivity.setHeartRate(heartRateMonitor.heartRate);
                        bikeActivity.addMessage();
                        sendControlText("message", IndoorCyclingService.this.bikeActivity.getText());


                    }
        });

        fePcc.subscribeManufacturerIdentificationEvent(new AntPlusCommonPcc.IManufacturerIdentificationReceiver() {
            @Override
            public void onNewManufacturerIdentification(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int hardwareRevision,
                                                        final int manufacturerID, final int modelNumber) {
            }
        });

        fePcc.getTrainerMethods().subscribeCalculatedTrainerPowerEvent(new AntPlusFitnessEquipmentPcc.ICalculatedTrainerPowerReceiver() {
            @Override
            public void onNewCalculatedTrainerPower(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                    final AntPlusFitnessEquipmentPcc.TrainerDataSource dataSource, final BigDecimal calculatedPower) {

                        if (calculatedPower.intValue() == -1)
                            sendControlText("power", "Invalid");

                        else {
                            sendControlText("power", "" + calculatedPower.intValue() + " W");
                            IndoorCyclingService.this.bikeActivity.setPower(calculatedPower);

                        }
                        IndoorCyclingService.this.bikeActivity.addMessage();

            }

            ;

        });
        /*
        fePcc.subscribeCapabilitiesEvent(new AntPlusFitnessEquipmentPcc.ICapabilitiesReceiver()
        {

            @Override
            public void onNewCapabilities(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                          final AntPlusFitnessEquipmentPcc.Capabilities capabilities)
            {

                IndoorCyclingService.this.capabilities = capabilities;
            }
        });

        fePcc.getTrainerMethods().subscribeBasicResistanceEvent(new AntPlusFitnessEquipmentPcc.IBasicResistanceReceiver()
        {

            @Override
            public void onNewBasicResistance(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                             final BigDecimal totalResistance)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {

                        Toast.makeText(IndoorCyclingService.this,
                                totalResistance.toString() + "%",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        */
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

    void sendTextMessage(String message)
    {
        Log.i(TAG, "sendTextMessage " + message);
        Intent intent = new Intent(IndoorCyclingService.NOTIFICATION);
        intent.putExtra("command", "message");
        intent.putExtra("message", message);
        this.sendBroadcast(intent);
    }

    void sendControlText(String control,String text)
    {
        Log.i(TAG, "sendControlText " + control + " " + text);
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

}
