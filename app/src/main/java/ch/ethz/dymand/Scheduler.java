package ch.ethz.dymand;


import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.VIBRATOR_SERVICE;
import static ch.ethz.dymand.Config.endHourWeekend;
import static ch.ethz.dymand.Config.eveningEndHourWeekday;
import static ch.ethz.dymand.Config.eveningStartHourWeekday;
import static ch.ethz.dymand.Config.hasStartedRecording;
import static ch.ethz.dymand.Config.lastRecordedTime;
import static ch.ethz.dymand.Config.morningEndHourWeekday;
import static ch.ethz.dymand.Config.morningStartHourWeekday;
import static ch.ethz.dymand.Config.shouldConnect;
import static ch.ethz.dymand.Config.startHourWeekend;
import static ch.ethz.dymand.Config.DEBUG_MODE;
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
    NONE //none of them
}

/**
 * This class is used to schedule the timer to trigger data collection
 */
public class Scheduler {

    private  static Scheduler instance = null; //singleton instance of class
    static Context  context;
    private static BleCallback ble;
    private static DataCollectionCallback dataCollection;
    private static MessageCallback msg;
    private static long minTimeBtnRecordings = 20 * 60 * 1000; //minimum time between recordings is 20 mins
    private static long DELAY_FOR_55_MINS = 50 * 60 * 1000; //5000; //
    private static long DELAY_FOR_60_MINS = 60 * 60 * 1000; //10000; //
    //private static long DELAY_FOR_55_MINS = 1 * 60 * 1000; //5000; //
    //private static long DELAY_FOR_60_MINS = 5 * 60 * 1000; //10000; //
    private static Calendar endOf7daysDate;

    //Ensures it is a singleton class
    public static Scheduler getInstance(Context contxt) {
        if (instance == null) {
            instance = new Scheduler();

            context = contxt;
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

    /**
     * Start the timer that runs each hour
     * Set to start the next Monday at the morning start time
     */
    public void startHourlyTimer(){
        //Sets start time to next monday morning start time
        Calendar rightNow = Calendar.getInstance(); //get calendar instance
        int today = rightNow.get(Calendar.DAY_OF_WEEK);
        int daysUntilNextMonday = 8;

        if (today !=  Calendar.MONDAY) {
            daysUntilNextMonday =(Calendar.SATURDAY - today + 2) % 7; //the 2 is the difference between Saturday and Monday
        }

        Calendar nextMondayDate = Calendar.getInstance();
        Calendar nextHour = Calendar.getInstance();
        nextHour.add(Calendar.HOUR_OF_DAY,1);
        nextHour.set(Calendar.MINUTE, 0);
        nextHour.set(Calendar.SECOND, 0);
        long millisUntilNextHour = nextHour.getTimeInMillis()- rightNow.getTimeInMillis();

        nextMondayDate.add(Calendar.DAY_OF_YEAR,daysUntilNextMonday);
        nextMondayDate.set(Calendar.HOUR_OF_DAY,morningStartHourWeekday);
        nextMondayDate.set(Calendar.MINUTE, 0);
        nextMondayDate.set(Calendar.SECOND, 0);
        long millisUntilNextMondayStart = nextMondayDate.getTimeInMillis()- rightNow.getTimeInMillis();

        //Set end of 7 days date
        endOf7daysDate = (Calendar) nextMondayDate.clone();
        endOf7daysDate.add(Calendar.DAY_OF_YEAR,7);

        //test
        //nextMondayDate.add(Calendar.MINUTE, 1);


        //Debug logs
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Log.d("Scheduler", "Today: " + today);
        Log.d("Scheduler", "Current date: " + df.format(rightNow.getTime()));
        Log.d("Scheduler", "Next monday date: " + df.format(nextMondayDate.getTime()));
        Log.d("Scheduler", "End of 7 days  date: " + df.format(endOf7daysDate.getTime()));
        Log.d("Scheduler", "Seconds till next monday is: " + millisUntilNextMondayStart/1000);
        Log.d("Scheduler", "Hours till next monday is: " + millisUntilNextMondayStart/(1000*60*60));

        Log.d("Scheduler", "Next hour date: " + df.format(nextHour.getTime()));
        Log.d("Scheduler", "Minutes till next hour: " + millisUntilNextHour/(1000*60));;
        Log.d("Scheduler", "Seconds till next hour " + millisUntilNextHour/1000);


        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {

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
                }

                timerHandler.postDelayed(this, DELAY_FOR_60_MINS);
            }
        };

        //timerHandler.postDelayed(timerRunnable, 5000);
        timerHandler.postDelayed(timerRunnable, millisUntilNextHour);
        //timerHandler.postDelayed(timerRunnable, millisUntilNextMondayStart);
    }

    /**
     * Executes at the start of each hour
     * Decides whether to collect data in this hour, start BLE, restart BLE, or stop BLE
     */
    public static void runEachHourly() throws FileNotFoundException {
        long delayDuration;

        //Check which hour it is
        DataCollectionHour hour = checkHour();

        switch (hour){

            case START:
                startTimerfor55mins();
                //delayDuration = setDelayDuration();
                collectData(); //test
                //startTimerForBleStart(delayDuration);
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
                //End timer
                if (ble != null) {
                    ble.stopBleCallback();
                }
                //TODO: cancel timer

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
            shouldConnect = true;
            return 0;
        }else{
            shouldConnect = false;

            if (ble != null){
                ble.reStartBleCallback();
            }

            return diff;
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

        //Check if end of 7 days
        if (endOf7daysDate.get(Calendar.DAY_OF_YEAR) == rightNow.get(Calendar.DAY_OF_YEAR) &&
                endOf7daysDate.get(Calendar.HOUR_OF_DAY) == rightNow.get(Calendar.HOUR_OF_DAY) ){
            return END_OF_7_DAYS;
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

        Log.d("Scheduler", "Current hour is " + hour);

        //return hour;
        //test
        return START;
    }


    /**
     *  Collects data if data collection in this hour hasn't started
     */
    private static void collectData() throws FileNotFoundException {
        if(!hasStartedRecording){
            if (dataCollection != null){
                dataCollection.collectDataCallBack();
            }
        }
    }


}
