package ch.ethz.dymand;

import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import ch.ethz.dymand.Sensors.SensorRecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Config {
    private static String LOG_TAG = "Logs: CONFIG: ";
    public static boolean DEBUG_MODE = false;
    public static final int NOTIFICATION_ID = 71193; //ID for foreground service - can be any number except zero
    public static final String CHANNEL_ID = "DynamdNotificationServiceChannel";

    public static int noOfExceptionsInHour = 0;

    public static int STUDY_DURATION = 7; //number of days for the study
    public static int HR_FREQ = 1; //Hz
    public static int ACCEL_FREQ = 20;  //Hz
    public static int GYRO_FREQ = 20; //Hz
    public static int LIGHT_FREQ = 1; //HZ
    public static int MILLIS = 1000;

    public static int ACCEL_PERIOD = MILLIS/ACCEL_FREQ;
    public static int GYRO_PERIOD = MILLIS/GYRO_FREQ;
    public static int HR_PERIOD = MILLIS/ HR_FREQ;
    public static int LIGHT_PERIOD = MILLIS/LIGHT_FREQ;

    public static final int MINUTE = 60000; // number of milliseconds in a minute
    public static final int HOUR = MINUTE * 60;
    public static final int SERVICE_LIFE = 1 * HOUR; // needs to be 7 days

    // For Demo
    public static final int RECORD_TIME = MINUTE * 1; // min of recording per hour

    public static final int SCAN_MODE_BLUETOOTH = ScanSettings.SCAN_MODE_LOW_POWER;
    public static final int RECORDER_AUDIO_CHANNELS = 2;
    public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    //public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    //public static String SERVICE_STRING = "e81f4267-5403-4646-8429-3d6a2ef85123";
    //public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE123";
    //public static String SERVICE_STRING = "e81f4267-5403-4646-8429-3d6a2ef85cc5";
    public static String SERVICE_STRING = "";
    public static StringBuilder SERVICE_STRING_BUFF = new StringBuilder("e81f4267-5403-4646-8429-3d6a2ef85cc2");
    //public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);
    public static UUID SERVICE_UUID;

    public static StringBuilder CHARACTERISTIC_STRING_BUFF = new StringBuilder("922e1110-dd53-41b4-bae1-05f795ccdcc");
    public static String CHARACTERISTIC_STRING = "7D2EBAAD-F7BD-485A-BD9D-92AD6ECFE93E";
    //public static String CHARACTERISTIC_STRING = "922e1110-dd53-41b4-bae1-05f795cc123";
    //public static String CHARACTERISTIC_STRING = "7D2EBAAD-F7BD-485A-BD9D-92AD6ECFE9123";
    //public static String CHARACTERISTIC_STRING = "922e1110-dd53-41b4-bae1-05f795ccdcc";
    public static UUID CHARACTERISTIC_UUID = UUID.fromString(CHARACTERISTIC_STRING);

    public static String audioFileTag = "watchRecordAudio";
    public static final int RECORDER_SAMPLE_RATE = 96000;
    public static final int RECORDER_ENCODING_BIT_RATE = 200000;

    public static boolean isCentral = false;
    public static final int SYNC_BUFFER = 5 * (MINUTE/60); //To sync between central and peripheral
    public static final int threshold = -75;
    private static final int PP_INTERVAL_SENSOR = 65547;
    private static final int NEW_ACTIVITY_SENSOR = 65549;
    private static final int HR_PPG_GAIN_SENSOR = 65544;
    private static final int HR_PPG_SENSOR = 65541;

    public static int[] sensorList = new int[]{
            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT
//            Sensor.TYPE_HEART_BEAT,
//            PP_INTERVAL_SENSOR,
//            NEW_ACTIVITY_SENSOR,
//            HR_PPG_GAIN_SENSOR,
//            HR_PPG_SENSOR
//            Sensor.TYPE_LINEAR_ACCELERATION,
//            Sensor.TYPE_STEP_COUNTER,
//            Sensor.TYPE_STEP_DETECTOR,
//            Sensor.TYPE_SIGNIFICANT_MOTION
    };

    public static HashMap<Integer,Integer> sensorPeriods= new HashMap<Integer,Integer>();
    static{
        sensorPeriods.put(Sensor.TYPE_HEART_RATE, HR_PERIOD);
        sensorPeriods.put(Sensor.TYPE_ACCELEROMETER, ACCEL_PERIOD);
        sensorPeriods.put(Sensor.TYPE_GYROSCOPE, GYRO_PERIOD);
        sensorPeriods.put(Sensor.TYPE_LIGHT, LIGHT_PERIOD);
    }


    public static final String SENSOR_FILE_EXTENSION = ".csv";

    public static final int INTENT_EXPIRY = 5 * (MINUTE); // Time for the intent to expiry

    //Timer configuration
    public static long lastRecordedTime = 0;
    public static long prevLastRecordedTime = 0;
    public static boolean hasStartedRecording = false;
    public static int morningStartHourWeekday = 8;
    public static int morningEndHourWeekday = 10;
    public static int eveningStartHourWeekday = 18;
    public static int eveningEndHourWeekday = 23;
    public static int startHourWeekend = 9;
    public static int endHourWeekend = 23;
    public static boolean isSelfReportCompleted = false;
    public static boolean hasSelfReportBeenStarted = false;
    public static boolean configReceived = false;

    //Bluetooth couple
    public static boolean shouldConnect = false;
    public static boolean scanAtFirstAllDevices = true;

    //For testing
    public static boolean SHOULD_SKIP_SET_UP = false;

    public static boolean recordedInHour = false;

    //App status
    public static boolean isSetupComplete = false;
    public static boolean isDemoComplete = false;
    public static boolean hasStudyStarted = false;
    public static boolean hasLogFileBeenCreated = false;
    public static boolean hasVoiceSampleBeenCollected = false;

    public static Calendar nextMondayDate = null;

    //Log Status

    public static String errorLogs = "";
    public static String subjectID;
    public static File bleSSFile = null;
    public static File logFile = null;
    public static File errorLogFile = null;
    public static boolean logStatusFileCreated = false;
    public static boolean isBeforeStudyLogFileCreated = false;
    public static File beforeStudylogFile = null;
    public static int batteryPercentage = 0;
    public static int noSilenceNum = 0;
    public static String noSilenceDates = "";
    public static int vadNum = 0;
    public static String vadDates = "";
    public static String surveyAlert1Date = "";
    public static boolean surveyAlert1 = false;
    public static String surveyAlert2Date  = "";
    public static boolean surveyAlert2 = false;
    public static int surveyTriggerNum = 0;
    public static String surveyTriggerDate  = "";
    public static String dataCollectStartDate = "";
    public static String dataCollectEndDate = "";
    public static int closeEnoughNum = 0;
    public static String closeEnoughDates = "";
    public static boolean last5Mins = false;
    public static String advertisingStarted = "";
    public static String advertisingStartedDates = "";
    public static String scanWasStarted = "";
    public static String scanStartDates = "";
    public static int startScanTriggerNum = 0; //number of times startScan() is called
    public static String startScanTriggerDates = ""; //dates when startScan() is called
    public static int startAdvertTriggerNum = 0;
    public static String startAdvertTriggerDates = "";
    public static int connectedNum = 0;
    public static String connectedDates = "";
    public static int recordingTriggeredNum = 0;
    public static String recordingTriggeredDates = "";
    public static int collectDataNum = 0;
    public static String collectDataDates = "";
    public static String discardDates = "";
    public static boolean selfReportStarted = false;
    public static String selfReportStartedDates = "";
    public static boolean selfReportCompleted = false;
    public static String selfReportCompletedDates = "";
    public static int noOfRestarts = 0;
    public static String restartDates = "";

    static SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
    static SimpleDateFormat df2 = new SimpleDateFormat("MM-dd-yyyy HH_mm_ss");

    public static String oldheader = "Date Time, " +
            "Battery Percentage, noSilenceNum, noSilenceDates,vadNum,vadDates," +
            "surveyAlert1, surveyAlert1Date, surveyAlert2, surveyAlert2Date, surveyTriggerNum, " +
            "surveyTriggerDate, dataCollectStartDate, dataCollectEndDate, closeEnoughNum, closeEnoughDates, " +
            "last5Mins,advertisingStarted, advertisingStartedDates,scanWasStarted, scanStartDates,startScanTriggerNum," +
            "startScanTriggerDates, startAdvertTriggerNum, startAdvertTriggerDates,connectedNum,connectedDates";

    @NonNull
    public static String getDateNow(){

        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        return df.format(rightNow.getTime())  + " | ";
    }

    public static String getDateNowForFilename(){

        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        return df2.format(rightNow.getTime());
    }

    public static void saveAppInfo(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();

        //Data collection hours
        editor.putInt("morningStartHourWeekday", morningStartHourWeekday);
        editor.putInt("morningEndHourWeekday", morningEndHourWeekday);
        editor.putInt("eveningStartHourWeekday", eveningStartHourWeekday);
        editor.putInt("eveningEndHourWeekday", eveningEndHourWeekday);
        editor.putInt("startHourWeekend", startHourWeekend);
        editor.putInt("endHourWeekend", endHourWeekend);
        editor.apply();
    }

    public static void updateUUID(){
        SERVICE_STRING = SERVICE_STRING_BUFF.toString();
        SERVICE_UUID = UUID.fromString(SERVICE_STRING);
    }

    public static void getStoredAppData(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        isSetupComplete = sharedPref.getBoolean("isSetupComplete", false);
        isCentral = sharedPref.getBoolean("isCentral", false);
        subjectID =  sharedPref.getString("subjectID", "0");
        isDemoComplete = sharedPref.getBoolean("isDemoComplete", false);
        hasVoiceSampleBeenCollected = sharedPref.getBoolean("hasVoiceSampleBeenCollected", false);
        hasLogFileBeenCreated = sharedPref.getBoolean("hasLogFileBeenCreated", false);
        hasStudyStarted = sharedPref.getBoolean("hasStudyStarted", false);
        SERVICE_STRING = sharedPref.getString("SERVICE_STRING", "");
        SERVICE_UUID = UUID.fromString(SERVICE_STRING);

        morningStartHourWeekday = sharedPref.getInt("morningStartHourWeekday", 0);
        morningEndHourWeekday = sharedPref.getInt("morningEndHourWeekday", 0);
        eveningStartHourWeekday = sharedPref.getInt("eveningStartHourWeekday", 0);
        eveningEndHourWeekday = sharedPref.getInt("eveningEndHourWeekday", 0);
        startHourWeekend = sharedPref.getInt("startHourWeekend", 0);
        endHourWeekend = sharedPref.getInt("endHourWeekend", 0);

        //TODO: Log values to cross-check
        Log.d(LOG_TAG, "isSetupComplete - " + isSetupComplete);
        Log.d(LOG_TAG, "isCentral - " + isCentral);
        Log.d(LOG_TAG, "subjectID - " + subjectID);
        Log.d(LOG_TAG, "hasVoiceSampleBeenCollected - " + hasVoiceSampleBeenCollected);
        Log.d(LOG_TAG, "hasLogFileBeenCreated - " + hasLogFileBeenCreated);
        Log.d(LOG_TAG, "hasStudyStarted - " + hasStudyStarted);
        Log.d(LOG_TAG, "SERVICE_STRING - " + SERVICE_STRING);
        Log.d(LOG_TAG, "morningStartHourWeekday - " + morningStartHourWeekday);
        Log.d(LOG_TAG, "morningEndHourWeekday - " + morningEndHourWeekday);
        Log.d(LOG_TAG, "eveningStartHourWeekday - " + eveningStartHourWeekday);
        Log.d(LOG_TAG, "eveningEndHourWeekday - " + eveningEndHourWeekday);
        Log.d(LOG_TAG, "startHourWeekend - " + startHourWeekend);
        Log.d(LOG_TAG, "endHourWeekend - " + endHourWeekend);
    }

    public static File getStorageLocation() {
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return root;
    }

    public static void setShouldConnect(){

        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        int currentMinute = rightNow.get(Calendar.MINUTE);

        //Check to make sure it's less than 15 mins to next hour
        int minUntilNextHour = 60 - currentMinute;

        if ( !(minUntilNextHour <= 15)){
            Log.i(LOG_TAG, "setting shouldConnect as true");
            shouldConnect = true;
        }
    }

    /**
     * Checks if devices connecting should be allowed at that time
     * @return
     */
    public static boolean allowedToConnect(){

        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        int currentMinute = rightNow.get(Calendar.MINUTE);

        //Check to make sure it's before x:44
        if (currentMinute < 44){
            return true;
        }

        return false;
    }

    public static boolean shouldConnectStatus(){
        return shouldConnect;
    }

    public static void resetShouldConnect(){
        Log.i(LOG_TAG, "setting shouldConnect as false");
        shouldConnect = false;
    }

    /* Checks if external storage is available for read and write */
    public static void isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Log.d(LOG_TAG, "external storage unvailable");
        }else{
            Log.d(LOG_TAG, "external storage vailable");
        }
    }

    public static void makeDirectories(String dirPath){
        Log.d(LOG_TAG, "making directories...");

        int WEEKEND_START = 6;
        int HOURS_IN_DAY = 24;

        //Create main folder with subject's id
        //File mainFolder = new File(getStorageLocation()+"/Subject_" + subjectID);
        String subject = "/Subject_" + subjectID;
        File mainFolder = new File(dirPath+subject);

//        if(!mainFolder.mkdirs()){
//            Log.d(LOG_TAG, "could not create  directories");
//        }else{
//            Log.d(LOG_TAG, "created  directories");
//        }
        mainFolder.mkdirs();

        File subFolder = null;
        String day = "";

        //Create folders for the 7 days
        for (int i = 1; i <= STUDY_DURATION; i++){
            day = "/Day_" + i;
            subFolder = new File(dirPath + subject + day);
            subFolder.mkdir();

            //Create sub folders for the weekday hours
            if (i < WEEKEND_START){

                for (int currentHour = 1; currentHour <=HOURS_IN_DAY; currentHour++){
                    //Log.d(LOG_TAG, "Weekday, Day " + i + " , " + "Hour " + currentHour);
                    if ((currentHour >= morningStartHourWeekday && currentHour <= morningEndHourWeekday) ||
                    (currentHour >= eveningStartHourWeekday && currentHour <= eveningEndHourWeekday)){

                        subFolder = new File(dirPath  + subject + day+ "/Hour_" + currentHour);
                        subFolder.mkdir();
                    }

                }

            //Create sub folders for the weekend hours
            }else {
                for (int currentHour = 1; currentHour <=HOURS_IN_DAY; currentHour++) {
                    //Log.d(LOG_TAG, "Weekend, Day " + i + " , " + "Hour " + currentHour);
                    if (currentHour >= startHourWeekend && currentHour <= endHourWeekend) {
                        subFolder = new File(dirPath  + subject + day + "/Hour_" + currentHour);
                        subFolder.mkdir();
                    }
                }
            }
        }

    }

    public static String createLogHeader(){
        StringBuilder headerBuff = new StringBuilder();

        headerBuff.append("Date");
        headerBuff.append(",");

        headerBuff.append("batteryPercentage");
        headerBuff.append(",");

        if (Config.isCentral){

            headerBuff.append("startScanTriggerNum");
            headerBuff.append(",");

            headerBuff.append("startScanTriggerDates");
            headerBuff.append(",");

            headerBuff.append("scanWasStarted");
            headerBuff.append(",");

            headerBuff.append("scanStartDates");
            headerBuff.append(",");

            headerBuff.append("closeEnoughNum");
            headerBuff.append(",");

            headerBuff.append("closeEnoughDates");
            headerBuff.append(",");

            headerBuff.append("noSilenceNum");
            headerBuff.append(",");

            headerBuff.append("noSilenceDates");
            headerBuff.append(",");

            headerBuff.append("vadNum");
            headerBuff.append(",");

            headerBuff.append("vadDates");
            headerBuff.append(",");

        }else{
            headerBuff.append("startAdvertTriggerNum");
            headerBuff.append(",");

            headerBuff.append("startAdvertTriggerDates");
            headerBuff.append(",");

            headerBuff.append("advertisingStarted");
            headerBuff.append(",");

            headerBuff.append("advertisingStartedDates");
            headerBuff.append(",");
        }

        headerBuff.append("connectedNum");
        headerBuff.append(",");

        headerBuff.append("connectedDates");
        headerBuff.append(",");


        headerBuff.append("collectDataNum");
        headerBuff.append(",");

        headerBuff.append("collectDataDates");
        headerBuff.append(",");

        headerBuff.append("recordingTriggeredNum");
        headerBuff.append(",");

        headerBuff.append("recordingTriggeredDates");
        headerBuff.append(",");

        headerBuff.append("dataCollectStartDate");
        headerBuff.append(",");

        headerBuff.append("dataCollectEndDate");
        headerBuff.append(",");

        headerBuff.append("surveyAlert1");
        headerBuff.append(",");

        headerBuff.append("surveyAlert1Date");
        headerBuff.append(",");

        headerBuff.append("surveyAlert2");
        headerBuff.append(",");

        headerBuff.append("surveyAlert2Date");
        headerBuff.append(",");

        headerBuff.append("surveyTriggerNum");
        headerBuff.append(",");

        headerBuff.append("surveyTriggerDate");
        headerBuff.append(",");

        headerBuff.append("last5Mins");
        headerBuff.append(",");

        headerBuff.append("discardDates");
        headerBuff.append(",");

        headerBuff.append("noOfErrors");

        return headerBuff.toString();
    }
}

/* The Sensors in Polar M600 are
android.sensor.accelerometer
android.sensor.linear_acceleration
android.sensor.gravity
android.sensor.gyroscope
android.sensor.significant_motion
android.sensor.step_detector
android.sensor.step_counter
android.sensor.wrist_tilt_gesture
android.sensor.light
android.sensor.heart_rate
com.polar.sensor.activity.met
com.polar.sensor.hr.ppg
com.polar.sensor.anymotion
com.polar.sensor.hr.ppg.gain
com.polar.sensor.sleep.std
com.polar.sensor.hr.247
com.polar.sensor.hr.ppInterval
android.sensor.heart_beat
com.polar.sensor.activity.acckcal
com.polar.sensor.activity.fusion
android.sensor.game_rotation_vector
 */