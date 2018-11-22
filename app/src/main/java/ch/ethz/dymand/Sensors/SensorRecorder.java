package ch.ethz.dymand.Sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import ch.ethz.dymand.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ch.ethz.dymand.Config.getDateNow;

public class SensorRecorder implements SensorEventListener{
    private static final String LOG_TAG = "Logs: SensorRecorder";
    private static final int SENSOR_DELAY = Config.SENSOR_DELAY;
    private SensorManager mSensorManager;
    public static int[] sensorList;
    public static boolean mRecording = false;
    public List<File> mFiles = null;
    private HashMap<String, StringBuilder> mSensorIndex = null;
    private String extension = Config.SENSOR_FILE_EXTENSION;
    private long currentTime = System.currentTimeMillis();
    private long prevTime = System.currentTimeMillis();

    public SensorRecorder(Context applicationContext){
        mSensorManager = (SensorManager) applicationContext.getSystemService(applicationContext.SENSOR_SERVICE);
        sensorList = Config.sensorList;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String sensorName = event.sensor.getStringType();
        StringBuilder toTrack = new StringBuilder(getDateNow() + "," + event.timestamp+","+event.accuracy);

        //Get values from sensor
        for(int i=0; i<event.values.length; i++) {
            toTrack.append("," + event.values[i]);
        }

        Log.i(LOG_TAG, sensorName + "," + toTrack.toString());

        //append new data sample
        mSensorIndex.get(sensorName+extension).append(toTrack.toString()+"\n");
    }

//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        currentTime = System.currentTimeMillis();
//        long diff = currentTime - prevTime;
//
//        String sensorName = event.sensor.getStringType();
//        int sensorType = event.sensor.getType();
//        StringBuilder toTrack = new StringBuilder(diff + "," + event.timestamp+","+event.accuracy);
//
//        //Update values at the specified sampling rate
//        int periodMillis = (Config.sensorPeriods.get(sensorType))/1000;
//
//        //Check if the time passed is more than period
//        if (diff > periodMillis){
//
//            //Get values from sensor
//            for(int i=0; i<event.values.length; i++) {
//                toTrack.append("," + event.values[i]);
//            }
//
//            Log.i(LOG_TAG, sensorName + "," + toTrack.toString());
//
//            //append new data sample
//            mSensorIndex.get(sensorName+extension).append(toTrack.toString()+"\n");
//
//            //Update previous time
//            prevTime = currentTime;
//        }
//    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void stopRecording(){
        if (mRecording) {
            mSensorManager.unregisterListener(this);
            mRecording = false;
            Log.i(LOG_TAG, "Stopped all sensor recording");

            if(mSensorIndex.isEmpty()) return;

            for(File file: mFiles){
                StringBuilder toWrite = mSensorIndex.get(file.getName());
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    stream.write(toWrite.toString().getBytes());
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            Log.i(LOG_TAG, "All sensor recording already stopped");
        }
        mFiles = null;
        mSensorIndex = null;
    }

    public void startRecording(String dirPath) {

        if (mRecording != false) throw new AssertionError();

        Sensor sensor;
        int sensorPeriod = 0;
        mFiles = new ArrayList<>();
        mSensorIndex = new HashMap<>();

        for(int sensorType:sensorList){

            sensor = mSensorManager.getDefaultSensor(sensorType);

            if(sensor != null) {
                String sensorName = sensor.getStringType();

                //creating files
                File file = new File(dirPath, sensorName+extension);
                mFiles.add(file);
                StringBuilder init = new StringBuilder("");
                mSensorIndex.put(file.getName(), init);

//                sensorPeriod = Config.sensorPeriods.get(sensorType);
                //mSensorManager.registerListener(this, sensor, sensorPeriod);

                //Setting sampling frequency as fastest provided by Android
                mSensorManager.registerListener(this, sensor, SENSOR_DELAY);
                Log.i(LOG_TAG, "Started sensor "+sensorName+ " and recording");
            }else {
                Log.i(LOG_TAG, "Sensor of type "+ sensorType +" not available.");
                continue;
            }
        }
        mRecording = true;
    }
}
