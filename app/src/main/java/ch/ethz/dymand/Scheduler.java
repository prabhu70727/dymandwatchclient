package ch.ethz.dymand;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static ch.ethz.dymand.Config.MILLIS;
import static ch.ethz.dymand.Config.advertisingStarted;
import static ch.ethz.dymand.Config.advertisingStartedDates;
import static ch.ethz.dymand.Config.batteryPercentage;
import static ch.ethz.dymand.Config.beforeStudylogFile;
import static ch.ethz.dymand.Config.closeEnoughDates;
import static ch.ethz.dymand.Config.closeEnoughNum;
import static ch.ethz.dymand.Config.collectDataDates;
import static ch.ethz.dymand.Config.collectDataNum;
import static ch.ethz.dymand.Config.connectedDates;
import static ch.ethz.dymand.Config.connectedNum;
import static ch.ethz.dymand.Config.createLogHeader;
import static ch.ethz.dymand.Config.dataCollectEndDate;
import static ch.ethz.dymand.Config.dataCollectStartDate;
import static ch.ethz.dymand.Config.discardDates;
import static ch.ethz.dymand.Config.endHourWeekend;
import static ch.ethz.dymand.Config.errorLogFile;
import static ch.ethz.dymand.Config.errorLogs;
import static ch.ethz.dymand.Config.eveningEndHourWeekday;
import static ch.ethz.dymand.Config.eveningStartHourWeekday;
import static ch.ethz.dymand.Config.hasLogFileBeenCreated;
import static ch.ethz.dymand.Config.hasSelfReportBeenStarted;
import static ch.ethz.dymand.Config.hasStartedRecording;
import static ch.ethz.dymand.Config.hasStudyStarted;
import static ch.ethz.dymand.Config.isBeforeStudyLogFileCreated;
import static ch.ethz.dymand.Config.isCentral;
import static ch.ethz.dymand.Config.isDemoComplete;
import static ch.ethz.dymand.Config.isSelfReportCompleted;
import static ch.ethz.dymand.Config.last5Mins;
import static ch.ethz.dymand.Config.lastRecordedTime;
import static ch.ethz.dymand.Config.logStatusFileCreated;
import static ch.ethz.dymand.Config.makeDirectories;
import static ch.ethz.dymand.Config.morningEndHourWeekday;
import static ch.ethz.dymand.Config.morningStartHourWeekday;
import static ch.ethz.dymand.Config.nextMondayDate;
import static ch.ethz.dymand.Config.noOfExceptionsInHour;
import static ch.ethz.dymand.Config.noSilenceDates;
import static ch.ethz.dymand.Config.noSilenceNum;
import static ch.ethz.dymand.Config.recordedInHour;
import static ch.ethz.dymand.Config.recordingTriggeredDates;
import static ch.ethz.dymand.Config.recordingTriggeredNum;
import static ch.ethz.dymand.Config.resetShouldConnect;
import static ch.ethz.dymand.Config.scanStartDates;
import static ch.ethz.dymand.Config.scanWasStarted;
import static ch.ethz.dymand.Config.selfReportCompleted;
import static ch.ethz.dymand.Config.selfReportCompletedDates;
import static ch.ethz.dymand.Config.selfReportStarted;
import static ch.ethz.dymand.Config.selfReportStartedDates;
import static ch.ethz.dymand.Config.setShouldConnect;
import static ch.ethz.dymand.Config.startAdvertTriggerDates;
import static ch.ethz.dymand.Config.startAdvertTriggerNum;
import static ch.ethz.dymand.Config.startHourWeekend;
import static ch.ethz.dymand.Config.DEBUG_MODE;
import static ch.ethz.dymand.Config.logFile;
import static ch.ethz.dymand.Config.startScanTriggerDates;
import static ch.ethz.dymand.Config.startScanTriggerNum;
import static ch.ethz.dymand.Config.subjectID;
import static ch.ethz.dymand.Config.surveyAlert1;
import static ch.ethz.dymand.Config.surveyAlert1Date;
import static ch.ethz.dymand.Config.surveyAlert2;
import static ch.ethz.dymand.Config.surveyAlert2Date;
import static ch.ethz.dymand.Config.surveyTriggerDate;
import static ch.ethz.dymand.Config.surveyTriggerNum;
import static ch.ethz.dymand.Config.vadDates;
import static ch.ethz.dymand.Config.vadNum;
import static ch.ethz.dymand.Config.bleSSFile;
import static ch.ethz.dymand.DataCollectionHour.BEFORE_START;
import static ch.ethz.dymand.DataCollectionHour.COLLECT_DATA;
import static ch.ethz.dymand.DataCollectionHour.END;
import static ch.ethz.dymand.DataCollectionHour.END_OF_7_DAYS;
import static ch.ethz.dymand.DataCollectionHour.END_OF_DAY;
import static ch.ethz.dymand.DataCollectionHour.NONE;
import static ch.ethz.dymand.DataCollectionHour.START;


import ch.ethz.dymand.Callbacks.*;

enum DataCollectionHour{
    START, //start of time block for data collection
    END, //end of time block for data collection
    COLLECT_DATA, //hours during which data should be collected
    END_OF_DAY, //end of data hour
    END_OF_7_DAYS, //end of 7 days
    BEFORE_START, //before start of study
    NONE //none of them
}

/**
 * This class is used to schedule the timer to trigger data collection
 */
public class Scheduler {

    private String LOG_TAG = "Scheduler";
    private  static Scheduler instance = null; //singleton instance of class
    private  static String subject = "/Subject_" + subjectID + "/";
    static Context  context;
    private static String dirPath;
    private static BleCallback ble;
    private static DataCollectionCallback dataCollection;
    private static MessageCallback msg;
    private static long DELAY_FOR_1_MIN =  5 * 1000; //10000; //
    private static long minTimeBtnRecordings = 20 * 60 * 1000; //minimum time between recordings is 20 mins
    private static long DELAY_FOR_55_MINS = 44 * 60 * 1000; //5000; //
    private static long DELAY_FOR_60_MINS = 60 * 60 * 1000; //10000; //
    private static Calendar endOf7daysDate;
    private static Calendar nextMondayDate;

//    private static long minTimeBtnRecordings = 4 * 60 * 1000; //minimum time between recordings is 20 mins
//    private static long DELAY_FOR_55_MINS = 1 * 60 * 1000; //5000; //
//    private static long DELAY_FOR_60_MINS = 5 * 60 * 1000; //10000; //

//    private static long DELAY_FOR_60_MINS = 1 * 60 * 1000; //10000; //

    //Ensures it is a singleton class
    public static Scheduler getInstance(Context contxt) {
        if (instance == null) {
            instance = new Scheduler();

            context = contxt;
            dirPath = context.getApplicationContext().getFilesDir().getAbsolutePath();

            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context));


            //startHourlyTimer();
        }

        return instance;
    }


    public void subscribeBleCallback(BleCallback bleInput){
        ble = bleInput;
    }

    public void subscribeMessageCallback(MessageCallback msgInput){
        msg = msgInput;
    }

    public void subscribeDataCollectionCallback (DataCollectionCallback dataCollectionInput){
        dataCollection = dataCollectionInput;
    }

    public void startDemoTimer(){
        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {

                //Record that demo is complete
                //TODO: proxy for demo complete. Replace with actual check
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isDemoComplete", true);
                editor.apply();

                if (DEBUG_MODE == true) {
                    //TODO: Remove vibrator test in final version
                    Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
                    v.vibrate(500); // Vibrate for 500 milliseconds

                    //TODO: Remove Trigger message to be displayed
                    if (msg != null) {
                        msg.triggerMsg("Start of new hour");
                    }

                    Log.d("Scheduler", "New hour start task performed on " + new Date());

                }

                try {
                    collectData();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, DELAY_FOR_1_MIN);
    }

    /**
     * Start the timer that runs each hour
     * Set to start the next Monday at the morning start time
     */
    public void startHourlyTimer() throws IOException {
        long millisUntilNextHour = 0;
        long millisUntilNextMondayStart = 0;

        //Create files with headers
        createFilesWithHeaders();

        //Get current date
        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        int today = rightNow.get(Calendar.DAY_OF_WEEK);

        //Set next Monday date only once when the app is started
        if (!hasStudyStarted) {
            //Sets start time to next monday morning start time
            int daysUntilNextMonday = 8;

            if (today != Calendar.MONDAY) {
                daysUntilNextMonday = (Calendar.SATURDAY - today + 2) % 7; //the 2 is the difference between Saturday and Monday
            }

            nextMondayDate = Calendar.getInstance();
            nextMondayDate.add(Calendar.DAY_OF_YEAR, daysUntilNextMonday);
            nextMondayDate.set(Calendar.HOUR_OF_DAY, morningStartHourWeekday);
            nextMondayDate.set(Calendar.MINUTE, 0);
            nextMondayDate.set(Calendar.SECOND, 0);
            millisUntilNextMondayStart = nextMondayDate.getTimeInMillis() - rightNow.getTimeInMillis();

            //Save Monday date to storarage so it can retrieved later
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            int dayOfNextMonday = nextMondayDate.get(Calendar.DAY_OF_YEAR);
            editor.putInt("dayOfNextMonday", dayOfNextMonday);
            editor.apply();

        }else{
            //Get info about next Monday date from storage
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            int dayOfNextMonday = sharedPref.getInt("dayOfNextMonday", 1);

            //Then recreate the date object
            nextMondayDate = Calendar.getInstance();
            nextMondayDate.set(Calendar.DAY_OF_YEAR, dayOfNextMonday);
            nextMondayDate.set(Calendar.HOUR_OF_DAY, morningStartHourWeekday);
            nextMondayDate.set(Calendar.MINUTE, 0);
            nextMondayDate.set(Calendar.SECOND, 0);

            //Test
        }

        //Sets start time to next hour start time
        Calendar nextHour = Calendar.getInstance();
        nextHour.add(Calendar.HOUR_OF_DAY,1);
        nextHour.set(Calendar.MINUTE, 0);
        nextHour.set(Calendar.SECOND, 0);
        millisUntilNextHour = nextHour.getTimeInMillis()- rightNow.getTimeInMillis();

        //Set end of 7 days date
        endOf7daysDate = (Calendar) nextMondayDate.clone();
        endOf7daysDate.add(Calendar.DAY_OF_YEAR,7);

        //endOf7daysDate = (Calendar) rightNow.clone();

        //test
        //nextMondayDate.add(Calendar.MINUTE, 1);


        //Debug logs
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Log.d("Scheduler", "Today: " + today);
        Log.d("Scheduler", "Current date: " + df.format(rightNow.getTime()));
        Log.d("Scheduler", "Next xmonday date: " + df.format(nextMondayDate.getTime()));
        Log.d("Scheduler", "End of 7 days  date: " + df.format(endOf7daysDate.getTime()));
        Log.d("Scheduler", "Total Seconds till next monday is: " + millisUntilNextMondayStart/1000);
        Log.d("Scheduler", "Total Hours till next monday is: " + millisUntilNextMondayStart/(1000*60*60));

        Log.d("Scheduler", "Next hour date: " + df.format(nextHour.getTime()));
        Log.d("Scheduler", "Minutes till next hour: " + millisUntilNextHour/(1000*60));;
        Log.d("Scheduler", "Seconds till next hour " + millisUntilNextHour/1000);


        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {

                //The first time the hourly timer get's triggered, note that the study has started
                if (!hasStudyStarted){
                    hasStudyStarted = true;

                    //Store in persistent data storage
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("hasStudyStarted", hasStudyStarted);
                    editor.apply();
                }

                if (DEBUG_MODE == true) {
                    //TODO: Remove vibrator test in final version
                    Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
                    v.vibrate(500); // Vibrate for 500 milliseconds

                    //TODO: Remove Trigger message to be displayed
                    if (msg != null) {
                        msg.triggerMsg("Start of new hour");
                    }

                    Log.d("Scheduler", "New hour start task performed on " + new Date());

                }

                try {
                    runEachHourly();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //TODO: remove for actual deployment
//                try {
//                    collectData();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }

                timerHandler.postDelayed(this, DELAY_FOR_60_MINS);


                //Test
//                Log.i(LOG_TAG, "Throwing Exception");
//                throw new NullPointerException();

            }
        };



        timerHandler.postDelayed(timerRunnable, millisUntilNextHour);

        //timerHandler.postDelayed(timerRunnable, 100000);
//        if (hasStudyStarted){
//            timerHandler.postDelayed(timerRunnable, millisUntilNextHour);
//
//            //TODO: remove after testing
//            //timerHandler.postDelayed(timerRunnable, 5000);
//        }else {
//
//            //TODO: remove after testing
//            //timerHandler.postDelayed(timerRunnable, 5000);
//
//            timerHandler.postDelayed(timerRunnable, millisUntilNextHour);
//
//            //timerHandler.postDelayed(timerRunnable, millisUntilNextMondayStart);
//        }

        //TODO: Remove
        //logBeforeStudyStart();

    }

    /**
     *
     */
    private static String createLogStatusString(){
        //Create string to log
        StringBuilder outputString = new StringBuilder();

        //Get info to log
        BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        batteryPercentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Calendar rightNow = Calendar.getInstance(); //get calendar instance

        //Create String
        outputString.append(df.format(rightNow.getTime()));
        outputString.append(",");

        outputString.append(batteryPercentage);
        outputString.append(",");

        if (isCentral){

            outputString.append(startScanTriggerNum);
            outputString.append(",");

            outputString.append(startScanTriggerDates);
            outputString.append(",");

            outputString.append(scanWasStarted);
            outputString.append(",");

            outputString.append(scanStartDates);
            outputString.append(",");

            outputString.append(closeEnoughNum);
            outputString.append(",");

            outputString.append(closeEnoughDates);
            outputString.append(",");

            outputString.append(noSilenceNum);
            outputString.append(",");

            outputString.append(noSilenceDates);
            outputString.append(",");

            outputString.append(vadNum);
            outputString.append(",");

            outputString.append(vadDates);
            outputString.append(",");

        }else{
            outputString.append(startAdvertTriggerNum);
            outputString.append(",");

            outputString.append(startAdvertTriggerDates);
            outputString.append(",");

            outputString.append(advertisingStarted);
            outputString.append(",");

            outputString.append(advertisingStartedDates);
            outputString.append(",");
        }

        outputString.append(connectedNum);
        outputString.append(",");

        outputString.append(connectedDates);
        outputString.append(",");

        outputString.append(collectDataNum);
        outputString.append(",");

        outputString.append(collectDataDates);
        outputString.append(",");

        outputString.append(recordingTriggeredNum);
        outputString.append(",");

        outputString.append(recordingTriggeredDates);
        outputString.append(",");

        outputString.append(dataCollectStartDate);
        outputString.append(",");

        outputString.append(dataCollectEndDate);
        outputString.append(",");

        outputString.append(surveyAlert1);
        outputString.append(",");

        outputString.append(surveyAlert1Date);
        outputString.append(",");

        outputString.append(surveyAlert2);
        outputString.append(",");

        outputString.append(surveyAlert2Date);
        outputString.append(",");

        outputString.append(surveyTriggerNum);
        outputString.append(",");

        outputString.append(surveyTriggerDate);
        outputString.append(",");

        outputString.append(last5Mins);
        outputString.append(",");

        outputString.append(discardDates);
        outputString.append(",");

        outputString.append(noOfExceptionsInHour);
        outputString.append(",");

        outputString.append(selfReportStarted);
        outputString.append(",");

        outputString.append(selfReportStartedDates);
        outputString.append(",");

        outputString.append(selfReportCompleted);
        outputString.append(",");

        outputString.append(selfReportCompletedDates);

        outputString.append("\n");

        //Reset previous hour's status info
        resetStatusInfo();

        String logStatusString = outputString.toString();
        return logStatusString;
    }

    /**
     * Resets previous hour's status info
     */
    private static void resetStatusInfo(){
        batteryPercentage = 0;
        noSilenceNum = 0;
        noSilenceDates = "";
        vadNum = 0;
        vadDates = "";
        surveyAlert1Date = "";
        surveyAlert1 = false;
        surveyAlert2Date  = "";
        surveyAlert2 = false;
        surveyTriggerNum = 0;
        surveyTriggerDate  = "";
        dataCollectStartDate = "";
        dataCollectEndDate = "";
        closeEnoughNum = 0;
        closeEnoughDates = "";
        last5Mins = false;
        advertisingStarted = "";
        advertisingStartedDates = "";
        scanWasStarted = "";
        scanStartDates = "";
        startScanTriggerNum = 0; //number of times startScan() is called
        startScanTriggerDates = ""; //dates when startScan() is called
        startAdvertTriggerNum = 0;
        startAdvertTriggerDates = "";
        connectedNum = 0;
        connectedDates = "";
        recordingTriggeredNum = 0;
        recordingTriggeredDates = "";
        collectDataNum = 0;
        collectDataDates = "";
        discardDates = "";
        errorLogs = "";
        selfReportStarted = false;
        selfReportStartedDates = "";
        selfReportCompleted = false;
        selfReportCompletedDates = "";
    }

    /**
     * Creates string containing info about duration until study starts
     * @return logString
     */
    private static String createBeforeStudyLogString(){
        //Create string to log
        StringBuilder outputString = new StringBuilder();

        //Get battery info
        BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        batteryPercentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        //Get date info
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Calendar rightNow = Calendar.getInstance(); //get calendar instance

        //Get duration until next Monday

        int SECONDS_IN_DAYS = 24*60*60;
        int SECONDS_IN_HOURS = 60*60;
        int SECONDS_IN_MINUTES = 60;

        long millisUntilNextMondayStart = nextMondayDate.getTimeInMillis()- rightNow.getTimeInMillis();
//        int secondsUntilNextMonday = (int) millisUntilNextMondayStart/MILLIS;
//        int minutesUntilNextMonday = secondsUntilNextMonday/60;
//        int hoursUntilNextMonday = minutesUntilNextMonday/60;
//        int daysUntilNextMonday = hoursUntilNextMonday/24;

        int totalSecondsUntilNextMonday = (int) millisUntilNextMondayStart/MILLIS;
        int daysUntilNextMonday = totalSecondsUntilNextMonday/SECONDS_IN_DAYS;

        int remainingHoursExpressedInSeconds = totalSecondsUntilNextMonday%SECONDS_IN_DAYS;
        int hoursUntilNextMonday = remainingHoursExpressedInSeconds/SECONDS_IN_HOURS;

        int remainingMinutesExpressedInSeconds = remainingHoursExpressedInSeconds%SECONDS_IN_HOURS;
        int minutesUntilNextMonday = remainingMinutesExpressedInSeconds/SECONDS_IN_MINUTES;

        int secondsUntilNextMonday = remainingMinutesExpressedInSeconds%SECONDS_IN_MINUTES;

        Log.d("Scheduler", "Log Information Before Study Starts");
        Log.d("Scheduler", "millisUntilNextMondayStart " + millisUntilNextMondayStart);
        Log.d("Scheduler", "totalSecondsUntilNextMonday " + totalSecondsUntilNextMonday);
        Log.d("Scheduler", "remainingHoursExpressedInSeconds " + remainingHoursExpressedInSeconds);
        Log.d("Scheduler", "remainingMinutesExpressedInSeconds " + totalSecondsUntilNextMonday);

        Log.d("Scheduler", "Current date: " + df.format(rightNow.getTime()));
        Log.d("Scheduler", "Next monday date: " + df.format(nextMondayDate.getTime()));
        Log.d("Scheduler", "End of 7 days  date: " + df.format(endOf7daysDate.getTime()));
        Log.d("Scheduler", "Remaining days till next monday is: " + daysUntilNextMonday );
        Log.d("Scheduler", "Remaining hours till next monday is: " + hoursUntilNextMonday);
        Log.d("Scheduler", "Remaining minutes till next monday is: " + minutesUntilNextMonday);
        Log.d("Scheduler", "Remaining seconds till next monday is: " + secondsUntilNextMonday);

        //Create String
        outputString.append(df.format(rightNow.getTime()));
        outputString.append(",");

        outputString.append(batteryPercentage);
        outputString.append(",");

        outputString.append(daysUntilNextMonday);
        outputString.append(",");

        outputString.append(hoursUntilNextMonday);
        outputString.append(",");

        outputString.append(minutesUntilNextMonday);
        outputString.append(",");

        outputString.append(secondsUntilNextMonday);
        outputString.append(",");

        outputString.append(noOfExceptionsInHour);
        outputString.append("\n");

        String logString = outputString.toString();
        return logString;
    }

    /**
     * Logs errors
     * @throws IOException
     */
    public static void logErrors() throws IOException {
        //Write system and app logs
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK)-1;
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int week = cal.get(Calendar.WEEK_OF_YEAR);

        String log = dirPath + subject+ "logs_" + subjectID + "_Week_" + week + "_Day_" + day + "_Hour_" + hour + ".csv";
        //Runtime.getRuntime().exec(new String[]{"logcat", "-f", log, "MyAppTAG:V", "*:S"});
        Runtime.getRuntime().exec(new String[]{"logcat", "-v", "time", "-f", log});

        //Check if the Files references are null. If they are, then it means the app was restarted
        //In which case, we need need to create an object reference to the file
        if (errorLogFile == null){
            //Create files for logging status of app
            errorLogFile = new File(dirPath, subject+"error_logs_" + subjectID + ".csv");
        }

        //Write status info to file
        FileOutputStream errorLogStream = new FileOutputStream(errorLogFile, true);

        try {
            errorLogStream.write(errorLogs.getBytes());
        } finally {
            errorLogStream.close();
        }

    }

    /**
     * Creates log files and adds headers to them
     * @throws FileNotFoundException
     */
    public static void createFilesWithHeaders() throws IOException {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        logStatusFileCreated = sharedPref.getBoolean("hasLogFileBeenCreated", false);

        if (!logStatusFileCreated) {

            //Create folders
            //makeDirectories(dirPath);

            //Create main folder with subject's id
            File mainFolder = new File(dirPath+subject);
            mainFolder.mkdirs();

            //Create files for logging status of app
            logFile = new File(dirPath, subject+ "log_status_" + subjectID + ".csv");
            errorLogFile = new File(dirPath, subject+"error_logs_" + subjectID + ".csv");
            bleSSFile = new File(dirPath, subject+"ble_signal_strength_log.csv");

            logStatusFileCreated = true;

            //Record that the files have been created and put in storage
            hasLogFileBeenCreated = true;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("hasLogFileBeenCreated", hasLogFileBeenCreated);
            editor.apply();

            //Log Subject id and header
            FileOutputStream stream = new FileOutputStream(logFile);
            FileOutputStream errorLogStream = new FileOutputStream(errorLogFile);
            FileOutputStream bleSSFileStream = null;
            if (isCentral){
                bleSSFileStream = new FileOutputStream(bleSSFile);
            }


            String header = createLogHeader();
            String log = "Subject ID: " + subjectID + "\n" + header + "\n";
            String errorLogHeader = "Subject ID: " + subjectID + "\n";
            String bleLogHeader = "Subject ID: " + subjectID + "\n" + "Date,Signal Strength" + "\n";

            try {
                stream.write(log.getBytes());
                errorLogStream.write(errorLogHeader.getBytes());

                if (isCentral){
                    bleSSFileStream.write(bleLogHeader.getBytes());
                }

            } finally {
                stream.close();
                errorLogStream.close();

                if (isCentral){
                    bleSSFileStream.close();
                }

            }
        }
    }

    /**
     * Logs status of app over the past 1 hour to file
     */
    public static void logStatus() throws IOException {
        //Check if the Files references are null. If they are, then it means the app was restarted
        //In which case, we need need to create an object reference to the file
        if (logFile == null){
            //Create files for logging status of app
            logFile = new File(dirPath, subject+"log_status_" + subjectID + ".csv");
        }

        if (isCentral && (bleSSFile == null)){
            bleSSFile = new File(dirPath, subject+"ble_signal_strength_log.csv");
        }

        //Get data to log
        String outputString = createLogStatusString();

        //Write status info to file
        FileOutputStream stream = new FileOutputStream(logFile,true);

        try {
            stream.write(outputString.toString().getBytes());
        } finally {
            stream.close();
        }
    }


    /**
     * Logs hourly information about time until study starts
     * @throws IOException
     */
    public static void logBeforeStudyStart() throws IOException {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        isBeforeStudyLogFileCreated = sharedPref.getBoolean("isBeforeStudyLogFileCreated", false);

        if (!isBeforeStudyLogFileCreated) {

            //Create main folder with subject's id
            File mainFolder = new File(dirPath + subject);
            mainFolder.mkdirs();

            //Create files for logging status of app
            beforeStudylogFile = new File(dirPath, subject + "before_study_log_" + subjectID + ".csv");
            isBeforeStudyLogFileCreated = true;

            //Record that the files have been created and put in storage
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("isBeforeStudyLogFileCreated", true);
            editor.apply();

            //Log Subject id and header
            FileOutputStream stream = new FileOutputStream(beforeStudylogFile);

            String header = "Date, Battery %, Days, Hours, Minutes, Seconds, No of Exceptions"; //describes the time until the study starts
            String log = "Subject ID: " + subjectID + "\n" + header + "\n";

            try {
                stream.write(log.getBytes());
            } finally {
                stream.close();
            }
        }

        //Check if the Files references are null. If they are, then it means the app was restarted
        //In which case, we need need to create an object reference to the file before writing to it
        if (beforeStudylogFile == null){
            //Create file reference for logging
            beforeStudylogFile = new File(dirPath, subject + "before_study_log_" + subjectID + ".csv");
        }

        //Get data to log
        String outputString = createBeforeStudyLogString();

        //Write status info to file
        FileOutputStream stream = new FileOutputStream(beforeStudylogFile,true);

        try {
            stream.write(outputString.toString().getBytes());
        } finally {
            stream.close();
        }
    }

    /**
     *Executes at the start of each hour
     * Decides whether to collect data in this hour, start BLE, restart BLE, or stop BLE
     */
    public static void runEachHourly() throws IOException {
        long delayDuration;

        //Reset values associated with recording each hour
        recordedInHour = false;
        hasStartedRecording = false;
        isSelfReportCompleted = false;
        hasSelfReportBeenStarted = false;

        //Reset general values
        noOfExceptionsInHour = 0;

        //Check which hour it is
        DataCollectionHour hour = checkHour();

        //Test
        //hour = COLLECT_DATA;
        Log.d("Scheduler", "Current hour is " + hour);

        //Log status only after study has started
        if (hour != BEFORE_START){
            logStatus();
        }

        logErrors();

        switch (hour){
            case BEFORE_START:
                logBeforeStudyStart();
                break;

            case START:
                startTimerfor55mins();
                delayDuration = setDelayDuration();
                //collectData(); //test
                startTimerForBleStart(delayDuration);
                break;

            case COLLECT_DATA:
                startTimerfor55mins();
                delayDuration = setDelayDuration();
                startTimerForBleStart(delayDuration);
                break;

            case END:
                if (ble != null) {
                    ble.stopBleCallback();
                }
                break;

            case END_OF_DAY:
                if (ble != null) {
                    ble.stopBleCallback();
                }

                break;

            case END_OF_7_DAYS:
                //TODO: Cancel timers

                //Stop ble
                if (ble != null) {
                    ble.stopBleCallback();
                }

                //Stop service
                Intent mService = new Intent(context, FGService.class);
                context.stopService(mService);

                //Kill app
                //android.os.Process.killProcess(android.os.Process.myPid());
                //System.exit(1);

            default:
                break;

        }
    }

    /**
     * Sets timer for 55 mins and decides if data should be collected
     */
    private static void startTimerfor55mins(){


        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {

                if (DEBUG_MODE == true) {
                    //TODO: Remove vibrator test in final version
                    Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
                    v.vibrate(500); // Vibrate for 500 milliseconds


                    //TODO: Remove trigger message to be displayed
                    msg.triggerMsg("Last 5 mins in hour");
                    Log.d("Scheduler", "Last 5 mins task performed on: " + new Date() + "n" +
                            "Thread's name: " + Thread.currentThread().getName());
                }

                try {
                    collectData();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, DELAY_FOR_55_MINS);
    }


    /**
     * Calculates the duration after which to start BLE and returns it
     * It sets shouldConnect to be true if it's past the minimum duration between recordings
     * It restarts the BLE if it's not past the minimum duration between recordings
     * @return diff
     */
    private static long setDelayDuration(){
        long curTime = System.currentTimeMillis();
        long diff = curTime - lastRecordedTime;

        //Check if minimum time between recordings has elapsed
        if (diff > minTimeBtnRecordings ){
            setShouldConnect();
            return 0;
        }else{
            resetShouldConnect();

            if (ble != null){
                ble.reStartBleCallback();
            }

            return (minTimeBtnRecordings - diff); //return remaining time to try to connect ;
        }
    }

    /**
     * Sets a timer to start BLE after delay
     * @param delay
     */
    private static void startTimerForBleStart(long delay){

        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            public void run() {
                if(ble != null) {
                    setShouldConnect();
                    ble.startBleCallback();
                }

                Log.d("Scheduler", "Task performed on: " + new Date() + "n" +
                        "Thread's name: " + Thread.currentThread().getName());
            }
        };
        timerHandler.postDelayed(timerRunnable, delay);
    }

    /**
     * Checks what kind of hour it is
     * @return hour
     */
    public static DataCollectionHour checkHour(){
        DataCollectionHour hour = NONE;
        boolean isWeekDay = true;
        Calendar rightNow = Calendar.getInstance(); //get calendar instance

        //Check if it's before study starts
        if (rightNow.get(Calendar.YEAR) <= nextMondayDate.get(Calendar.YEAR) && //current year is before of same as year of study
                (rightNow.get(Calendar.DAY_OF_YEAR) < nextMondayDate.get(Calendar.DAY_OF_YEAR)) ||  //day is before day of study
                (rightNow.get(Calendar.DAY_OF_YEAR) == nextMondayDate.get(Calendar.DAY_OF_YEAR) &&  //same day of study
                        (rightNow.get(Calendar.HOUR_OF_DAY) < nextMondayDate.get(Calendar.HOUR_OF_DAY)))){  //and hour before study start hour
            hour = BEFORE_START;
            return hour;
        }

        //Check if end of 7 days
//        if (endOf7daysDate.get(Calendar.DAY_OF_YEAR) == rightNow.get(Calendar.DAY_OF_YEAR) && //end of study is same as current day
//                endOf7daysDate.get(Calendar.HOUR_OF_DAY) == rightNow.get(Calendar.HOUR_OF_DAY) ){ //hour of study end date is same as current hour

        //Check if end of 7 days
        if (rightNow.get(Calendar.DAY_OF_YEAR) >= endOf7daysDate.get(Calendar.DAY_OF_YEAR)){ //today's date is same as or after end of study date
            hour = END_OF_7_DAYS;
            return hour;
        }

        //Check if today is a weekday
        int today = rightNow.get(Calendar.DAY_OF_WEEK);

        if (today == Calendar.SATURDAY || today == Calendar.SUNDAY) {
            isWeekDay = false;
        }

        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY); // return the hour in 24 hrs format (ranging from 0-23
        //int currentHour = 5; //test

        if (isWeekDay){
            if ((currentHour == morningStartHourWeekday) || (currentHour == eveningStartHourWeekday)){
                hour = START;

            }else if(currentHour == morningEndHourWeekday ) {
                hour = END;

            }else if(currentHour == eveningEndHourWeekday){
                hour = END_OF_DAY;

            } else if ((currentHour > morningStartHourWeekday && currentHour < morningEndHourWeekday) ||
                    (currentHour > eveningStartHourWeekday && currentHour < eveningEndHourWeekday)){
                hour = COLLECT_DATA;
            }
        }else{
            if (currentHour == startHourWeekend) {
                hour = START;

            }else if (currentHour == endHourWeekend){
                hour = END_OF_DAY;

            }else if ((currentHour > startHourWeekend && currentHour < endHourWeekend)){
                hour = COLLECT_DATA;
            }
        }

        //Log.d("Scheduler", "Current hour is " + hour);

        return hour;

        //test
        //return START;
    }


    /**
     *  Collects data if data collection recording isn't onging,
     *  and (either recording has not been done in this hour already or self report has not been completed)
     */
    private static void collectData() throws FileNotFoundException {
        if(!hasStartedRecording && (!recordedInHour || !isSelfReportCompleted)){
            if (dataCollection != null){
                last5Mins = true;
                dataCollection.collectDataCallBack();
            }
        }
    }


}
