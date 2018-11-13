package ch.ethz.dymand;

import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import ch.ethz.dymand.Sensors.SensorRecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class Config {
    public static boolean DEBUG_MODE = true;
    public static final int NOTIFICATION_ID = 71193;
    public static final String CHANNEL_ID = "DynamdNotificationServiceChannel";


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
    public static String SERVICE_STRING = "e81f4267-5403-4646-8429-3d6a2ef85cc5";
    public static StringBuilder SERVICE_STRING_BUFF = new StringBuilder("e81f4267-5403-4646-8429-3d6a2ef85cc2");
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

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
    public static final int threshold = -100;

    public static int[] sensorList = new int[]{
            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT
    };
    public static final String SENSOR_FILE_EXTENSION = ".csv";

    public static final int INTENT_EXPIRY = 5 * (MINUTE); // Time for the intent to expiry

    //Timer configuration
    public static long lastRecordedTime = 0;
    public static long prevLastRecordedTime = 0;
    public static boolean hasStartedRecording = false;
    public static int morningStartHourWeekday = 8;
    public static int morningEndHourWeekday = 10;
    public static int eveningStartHourWeekday = 18;
    public static int eveningEndHourWeekday = 22;
    public static int startHourWeekend = 9;
    public static int endHourWeekend = 22;
    public static boolean isSelfReportCompleted = false;
    public static boolean hasSelfReportBeenStarted = false;
    public static boolean configReceived = false;

    //Bluetooth couple
    public static boolean shouldConnect = false;
    public static File bleSSFile = new File("Bluetooth");


    //For testing
    public static boolean SHOULD_SKIP_SET_UP = false;

    public static boolean recordedInHour = false;

    //Log Status
    public static String errorLogs = "";
    public static String subjectID;
    public static File logFile = null;
    public static File errorLogFile = null;
    public static boolean logStatusFileCreated = false;
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

    static SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    public static String getDateNow(){

        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        return df.format(rightNow.getTime())  + " | ";
    }

    public static String oldheader = "Date Time, " +
            "Battery Percentage, noSilenceNum, noSilenceDates,vadNum,vadDates," +
            "surveyAlert1, surveyAlert1Date, surveyAlert2, surveyAlert2Date, surveyTriggerNum, " +
            "surveyTriggerDate, dataCollectStartDate, dataCollectEndDate, closeEnoughNum, closeEnoughDates, " +
            "last5Mins,advertisingStarted, advertisingStartedDates,scanWasStarted, scanStartDates,startScanTriggerNum," +
            "startScanTriggerDates, startAdvertTriggerNum, startAdvertTriggerDates,connectedNum,connectedDates";

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