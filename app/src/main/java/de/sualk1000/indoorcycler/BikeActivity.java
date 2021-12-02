package de.sualk1000.indoorcycler;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.garmin.fit.Activity;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.FileCreatorMesg;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class BikeActivity {
    private final static String TAG = BikeActivity.class.getSimpleName();
    private final IndoorCyclingService indoorCyclingService;

    //Context context;
    //FragmentActivity activity;
    int lat = (int) (11930465 * 51.904491);
    int lon = (int) (11930465 * 10.427830);
    Date start;
    //private EventMesg eventMesgStop;
    LinkedList<Mesg> messages = new LinkedList<Mesg>();
    Calendar cal = Calendar.getInstance();
    //EventMesg eventMesgStart = null;
    private double speed = -1;
    private double power = -1;
    private long distance = -1;
    private int heartRate = 0;

    private long distance_offset = 0;


    BikeActivity(IndoorCyclingService indoorCyclingService)
    {
        this.indoorCyclingService = indoorCyclingService;
        //this.context = context;
        //this.activity = activity;
    }

    public void setSpeed(BigDecimal speed) {
        this.speed = speed.doubleValue();
    }
    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public void setPower(BigDecimal power) {
        this.power=power.doubleValue();
    }

    public void setDistance(long distance) {
        this.distance = distance;
    }
    SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("mm:ss");
    public String getText() {
        String ret = "" ;
        if(start != null) {
            long time = (cal.getTime().getTime() - start.getTime());
            if(time > ( 3600 * 1000) )
                ret += simpleDateFormat1.format(new Date(time));
            else
                ret += simpleDateFormat2.format(new Date(time));
            if(distance + distance_offset < 1000)
                ret += "  " + (distance + distance_offset) + " m";
            else
                ret += "  " + ((double)(((int)( (distance + distance_offset) * 100) / 1000) * 100)/1000) + " km";
            ret += " " +heartRate + " bpm";
        }
        return ret;
    }

    private class sendDataTask extends AsyncTask<Void, Void, Void> {

        private final File file;

        sendDataTask( File file) {
            this.file = file;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {

                GarminConnect gc = new GarminConnect();
                if (gc.signin("sualk1000@googlemail.com", "4WQv9!q1") == false)
                    throw new Exception("Login error");






                File file = Export.buildFitFile((Context)indoorCyclingService, messages);
                String upload_message = gc.uploadFitFile(file);
                gc.close();
                if (upload_message == null) {
                    upload_message = "Upload ok";
                    if(file != null & file.exists())
                        file.delete();

                    indoorCyclingService.sendTextMessage(upload_message);

                }else
                {
                    indoorCyclingService.sendTextMessage(upload_message);
                    indoorCyclingService.sendCommand("ask");

                }
                Log.e(TAG, upload_message);



            } catch (Exception e) {
                Log.e(TAG, e.toString());

                indoorCyclingService.sendTextMessage("Failed: " + e.getMessage());
                return null;
            }

            return null;
        }
    }


    void addStartMessage(Date startDate)
    {

        cal.setTime(startDate);
        FileIdMesg fileIdMesg = new FileIdMesg();
        fileIdMesg.setType(com.garmin.fit.File.ACTIVITY);
        fileIdMesg.setManufacturer(Manufacturer.GARMIN);
        fileIdMesg.setProduct(1);
        fileIdMesg.setSerialNumber(3986196270L);
        fileIdMesg.setTimeCreated(new DateTime(startDate));

        messages.add(fileIdMesg);


        FileCreatorMesg creatorMesg = new FileCreatorMesg();
        creatorMesg.setSoftwareVersion(700);


        DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();
        deviceInfoMesg.setDeviceIndex((short) 1);
        deviceInfoMesg.setManufacturer(Manufacturer.GARMIN);
        deviceInfoMesg.setProduct(2888);
        deviceInfoMesg.setProductName("FIT Cookbook"); // Max 20 Chars
        deviceInfoMesg.setSerialNumber(1L);
        deviceInfoMesg.setSoftwareVersion((float) 7L);
        deviceInfoMesg.setTimestamp(new DateTime(startDate));

        deviceInfoMesg.setDeviceIndex(DeviceIndex.CREATOR);
        deviceInfoMesg.setManufacturer(Manufacturer.DEVELOPMENT);


        messages.add(deviceInfoMesg);

        EventMesg eventMesgStart = new EventMesg();
        eventMesgStart.setTimestamp(new DateTime(cal.getTime()));
        eventMesgStart.setEvent(Event.TIMER);
        eventMesgStart.setEventType(EventType.START);
        messages.add(eventMesgStart);

    }
    public boolean isStarted()
    {
        if(start != null)
            return true;
        File file = new File(indoorCyclingService.getCacheDir() , "MyCache.json");
        if(file.exists() == false)
            return false;

        return resume(file);
    }

    public boolean resume(File file) {

        try {

            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String lineData = bufferedReader.readLine();

            Date startDate = null;
            Date endDate = null;
            long distance = 0;
            while (lineData != null) {

                Log.i(TAG, lineData);
                JSONObject jsonObject = new JSONObject(lineData);

                RecordMesg recordMesg = new RecordMesg();
                Date date = new Date ((long) jsonObject.get("time"));

                if(startDate == null)
                    startDate = date;
                endDate = date;
                recordMesg.setTimestamp(new DateTime(date));
                distance =  Long.valueOf(jsonObject.get("distance").toString());

                recordMesg.setDistance((float) distance); // Ramp
                recordMesg.setSpeed(Float.valueOf(jsonObject.get("speed").toString())); // Speed in m/s = km/h / 3.6
                recordMesg.setPower(Float.valueOf(jsonObject.get("power").toString()).intValue()); // Watt
                if(jsonObject.has("heartRate")) {
                    recordMesg.setHeartRate(Short.valueOf( jsonObject.get("heartRate").toString())); // Sine
                }

                //recordMesg.setCadence((short) 90); // Trittfrequenz bpm
                //recordMesg.setAltitude(200F); // Triangle
                recordMesg.setPositionLat(lat);
                recordMesg.setPositionLong(lon);
                addMessage(recordMesg,date);
                messages.add(recordMesg);

                //retBuf.append(lineData);
                lineData = bufferedReader.readLine();

            }

            if(endDate.getTime() - startDate.getTime() < 1000 * 60 * 10)
            {
                start = startDate;
                distance_offset = distance;
                return true;
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        }

        Log.i(TAG, "End");
        return false;

    }

    public void start() {
        if(start != null)
        {
            indoorCyclingService.sendTextMessage("Already started");

            return;
        }

        start = new Date();
        cal.setTime(start);
        addStartMessage(start);

        indoorCyclingService.sendControlStatus("start",false);
        indoorCyclingService.sendControlStatus("stop",true);


    }

    void addEndMessage(Date startDate,Date endDate,int lat,int lon,double distance)
    {
        EventMesg eventMesgStop = new EventMesg();
        cal.setTime(endDate);

        eventMesgStop.setTimestamp(new DateTime(cal.getTime()));
        eventMesgStop.setEvent(Event.TIMER);
        eventMesgStop.setEventType(EventType.STOP_ALL);
        messages.add(eventMesgStop);

        LapMesg lapMesg = new LapMesg();
        lapMesg.setMessageIndex(0);
        lapMesg.setStartTime(new DateTime(cal.getTime()));
        lapMesg.setTimestamp(new DateTime(cal.getTime()));

        long time = (cal.getTime().getTime() - startDate.getTime()) / 1000;
        lapMesg.setTotalElapsedTime((float) (new DateTime(cal.getTime()).getTimestamp() - new DateTime(startDate).getTimestamp()));
        lapMesg.setTotalTimerTime((float) (new DateTime(cal.getTime()).getTimestamp() - new DateTime(startDate).getTimestamp()));

        lapMesg.setStartPositionLat((int) lat);
        lapMesg.setStartPositionLong((int) lon);
        lapMesg.setEndPositionLat((int) lat);
        lapMesg.setEndPositionLong((int) lon);
        lapMesg.setTotalDistance((float) distance);

        lapMesg.setAvgSpeed((float) (distance / time));
        messages.add(lapMesg);

        SessionMesg sessionMesg = new SessionMesg();
        sessionMesg.setMessageIndex(0);
        sessionMesg.setTimestamp(new DateTime(cal.getTime()));
        sessionMesg.setStartTime(new DateTime(startDate));
        sessionMesg.setEvent(Event.LAP);
        sessionMesg.setEventType(EventType.STOP);
        sessionMesg.setTotalElapsedTime((float) (new DateTime(cal.getTime()).getTimestamp() - new DateTime(startDate).getTimestamp()));
        sessionMesg.setTotalTimerTime((float) (new DateTime(cal.getTime()).getTimestamp() - new DateTime(startDate).getTimestamp()));
        sessionMesg.setSport(Sport.CYCLING);
        sessionMesg.setSubSport(SubSport.INDOOR_CYCLING);
        sessionMesg.setFirstLapIndex(0);
        sessionMesg.setNumLaps(1);
        sessionMesg.setStartPositionLat(lat);
        sessionMesg.setStartPositionLong(lon);
        messages.add(sessionMesg);


        ActivityMesg activityMesg = new ActivityMesg();
        activityMesg.setTimestamp(new DateTime(cal.getTime()));
        activityMesg.setNumSessions(1);
        activityMesg.setLocalTimestamp((new DateTime(endDate).getTimestamp()));
        activityMesg.setTotalTimerTime(sessionMesg.getTotalTimerTime());
        activityMesg.setEvent(Event.ACTIVITY);
        activityMesg.setEventType(EventType.STOP);
        activityMesg.setType(Activity.MANUAL);


        messages.add(activityMesg);

        start = null;
        distance_offset = 0;
    }
    public void stop() {
        if(start == null)
        {

            indoorCyclingService.sendTextMessage("Not started");

            return;
        }

        addEndMessage(start,new Date(),lat,lon,distance+distance_offset);

        send(null);


    }
    public void pause() {

    }
    public void send(File file) {
        if(start != null && file == null)
        {
            indoorCyclingService.sendTextMessage("Not stopped");

            return;
        }
        new sendDataTask(file).execute();

    }
    public void addMessage(RecordMesg recordMesg,Date date) {

        if(messages.size() == 0) {
            addStartMessage(date);
        }
        messages.add(recordMesg);
    }

    public void loadMessages(File file)
    {
        try {

            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String lineData = bufferedReader.readLine();

            Date startDate = null;
            Date endDate = null;
            long distance = 0;
            while (lineData != null) {

                Log.i(TAG, lineData);
                JSONObject jsonObject = new JSONObject(lineData);

                RecordMesg recordMesg = new RecordMesg();
                Date date = new Date ((long) jsonObject.get("time"));

                if(startDate == null)
                    startDate = date;
                endDate = date;
                recordMesg.setTimestamp(new DateTime(date));
                distance =  Long.valueOf(jsonObject.get("distance").toString());
                recordMesg.setDistance((float) distance); // Ramp
                recordMesg.setSpeed(Float.valueOf(jsonObject.get("speed").toString())); // Speed in m/s = km/h / 3.6
                recordMesg.setPower(Float.valueOf(jsonObject.get("power").toString()).intValue()); // Watt
                if(jsonObject.has("heartRate")) {
                    recordMesg.setHeartRate(Short.valueOf( jsonObject.get("heartRate").toString())); // Sine
                }

                //recordMesg.setCadence((short) 90); // Trittfrequenz bpm
                //recordMesg.setAltitude(200F); // Triangle
                recordMesg.setPositionLat(lat);
                recordMesg.setPositionLong(lon);
                addMessage(recordMesg,date);
                messages.add(recordMesg);

                //retBuf.append(lineData);
                lineData = bufferedReader.readLine();

            }
            addEndMessage(startDate,endDate,lat,lon,distance);
            Log.i(TAG, "End");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        }

    }

    public void addMessage() {

        if(start == null)
        {
            return;
        }


        if(this.speed < 0 )
            return;
        if(this.power < 0 )
            return;
        if(this.distance+this.distance_offset < 0 )
            return;

        cal.setTime(new Date());

        try{
                File file = new File(indoorCyclingService.getCacheDir() , "MyCache.json");

                FileOutputStream fileOutputStream = new FileOutputStream(file,true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                //BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("time",cal.getTime().getTime());
                jsonObject.put("distance",distance+distance_offset);
                jsonObject.put("power",power);
                jsonObject.put("speed",speed);
                if(this.heartRate > 0 ) {
                    jsonObject.put("heartRate",heartRate);
                }
                outputStreamWriter.write(jsonObject.toString());
                outputStreamWriter.write("\n");
                outputStreamWriter.close();
                fileOutputStream.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        }

        RecordMesg recordMesg = new RecordMesg();
            recordMesg.setTimestamp(new DateTime(cal.getTime()));

            // Fake Record Data of Various Signal Patterns
            recordMesg.setDistance((float) (distance+distance_offset)); // Ramp
            recordMesg.setSpeed((float) speed); // Speed in m/s = km/h / 3.6
            if(this.heartRate > 0 ) {

                recordMesg.setHeartRate((short) heartRate); // Sine
            }

            //recordMesg.setCadence((short) 90); // Trittfrequenz bpm
            recordMesg.setPower((int) power); // Watt
            //recordMesg.setAltitude(200F); // Triangle
            recordMesg.setPositionLat(lat);
            recordMesg.setPositionLong(lon);
        messages.add(recordMesg);

    }

}
