package ch.ethz.dymand;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.dymand.Audio.BackgroundAudioRecorder;
import ch.ethz.dymand.Sensors.SensorRecorder;

import static android.content.Context.VIBRATOR_SERVICE;
import static ch.ethz.dymand.Config.DEBUG_MODE;
import static ch.ethz.dymand.Config.dataCollectStartDate;
import static ch.ethz.dymand.Config.dataCollectEndDate;
import static ch.ethz.dymand.Config.discardDates;
import static ch.ethz.dymand.Config.getDateNow;
import static ch.ethz.dymand.Config.hasSelfReportBeenStarted;
import static ch.ethz.dymand.Config.hasStartedRecording;
import static ch.ethz.dymand.Config.isCentral;
import static ch.ethz.dymand.Config.isSelfReportCompleted;
import static ch.ethz.dymand.Config.lastRecordedTime;
import static ch.ethz.dymand.Config.prevLastRecordedTime;
import static ch.ethz.dymand.Config.recordedInHour;
import static ch.ethz.dymand.Config.recordingTriggeredDates;
import static ch.ethz.dymand.Config.recordingTriggeredNum;
import static ch.ethz.dymand.Config.shouldConnect;
import static ch.ethz.dymand.Callbacks.WatchPhoneCommCallback;
import static ch.ethz.dymand.Callbacks.MessageCallback;
import static ch.ethz.dymand.Config.surveyAlert1;
import static ch.ethz.dymand.Config.surveyAlert1Date;
import static ch.ethz.dymand.Config.surveyAlert2;
import static ch.ethz.dymand.Config.surveyAlert2Date;
import static ch.ethz.dymand.Config.surveyTriggerDate;

public class DataCollection implements Callbacks.DataCollectionCallback{

    BackgroundAudioRecorder mAudioRecorder;
    SensorRecorder mSensorRecorder;
    private  static DataCollection instance = null;
    private static MessageCallback msg;
    private static long DELAY_FOR_5_MINS = 5 * 60 * 1000;
    private static long DELAY_FOR_2_MINS = 2 * 60 * 1000;
    private static long DELAY_FOR_3_MINS = 3 * 60 * 1000;
    private static long DELAY_FOR_6_MINS = 6 * 60 * 1000;
    private static long DELAY_FOR_8_MINS = 8 * 60 * 1000;

    //private static long DELAY_FOR_5_MINS = 1 * 60  * 1000;

    private static final String LOG_TAG = "Data Collection";
    private WatchPhoneCommCallback commCallback;

    private static Context context;
    
    public static DataCollection getInstance(Context contxt) {
        if (instance == null) {
            instance = new DataCollection();
            context = contxt;
        }
        return instance;
    }

    public void subscribeMessageCallback(MessageCallback msgInput){
        msg = msgInput;
    }

    public void subscribeMessageCallback(WatchPhoneCommCallback commCallbackInput){
        commCallback = commCallbackInput;
    }

    private void startRecording(String timeStamp) throws FileNotFoundException {

        //Set variable to track whether recording has started
        hasStartedRecording = true;

        //Log that recording has started
        recordingTriggeredNum++;
        recordingTriggeredDates = recordingTriggeredDates + getDateNow();

        String tag = "app_";
        if(isCentral) tag = tag+"black_";
        else tag = tag + "white_";

        File recordDir = new File(context.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+tag+timeStamp);
        recordDir.mkdirs();
        String dirPath = context.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+tag+timeStamp+"/";

        mAudioRecorder = new BackgroundAudioRecorder(context);
        mSensorRecorder = new SensorRecorder(context);
        mAudioRecorder.startRecording(dirPath); // non blocking return.
        mSensorRecorder.startRecording(dirPath); // non blocking return.

        //Record start of data collection
        dataCollectStartDate = dataCollectStartDate + getDateNow();

        //Perform task after 5 minutes recording
        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    stopRecording();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                //Alert user
                alertUser();


                //Record alert
                surveyAlert1 = true;
                surveyAlert1Date = surveyAlert1Date + getDateNow();

                //Send User intent to phone
                if (commCallback != null) {
                    commCallback.signalPhone();

                    surveyTriggerDate = surveyTriggerDate + getDateNow();
                }


                //Testing. TODO: Remove
                hasStartedRecording = false;
                hasSelfReportBeenStarted = true;
                isSelfReportCompleted = true;

                //Start 2 minute timer to check the survey has been completed
                startFirst2minTimer();
            }
        };

        //Start timer to stop recording after 5 mins
        timerHandler.postDelayed(timerRunnable,DELAY_FOR_5_MINS);

        if (DEBUG_MODE == true){
            msg.triggerMsg("Recording started");
            Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            v.vibrate(500); // Vibrate for 500 milliseconds
        }
    }

    /**
     * Alerts user to fill self report
     */
    private void alertUser(){

        //Alert user to fill self report
        Vibrator v = (Vibrator)  context.getSystemService(VIBRATOR_SERVICE);
        v.vibrate(1000); // Vibrate for 500 milliseconds

        if (DEBUG_MODE == true) {
            //TODO: Remove trigger message to be displayed
            msg.triggerMsg("Recording for 5 mins done");
            Log.d("Logs: Data Collection", "Recording for 5 mins done: " + new Date() + "n" +
                    "Thread's name: " + Thread.currentThread().getName());

        }

        //TODO: Trigger image indicating that self report should be filled
    }

    /**
     * Starts 2-minute timer to check if filling of survey has started
     */
    private void startFirst2minTimer(){

        //Performs task after 2 minutes

        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {

                //Check if self report has been started
                //TODO: Add code that updates isSelfReportCompleted
                if (!hasSelfReportBeenStarted){

                    //Alerts user
                    alertUser();

                    //Record alert
                    surveyAlert2 = true;
                    surveyAlert2Date = surveyAlert2Date + getDateNow();

                    //Start another 2 minute timer to check the survey has been completed, else disregard recording
                    startSecond2minTimer();
                }else{
                    //Check if self report has been completed
                    if (!isSelfReportCompleted){
                        if (DEBUG_MODE == true) {
                            //TODO: Remove trigger message to be displayed
                            msg.triggerMsg("3 mins alert");
                            Log.d("Scheduler", "3 mins alert" + new Date() + "n" +
                                    "Thread's name: " + Thread.currentThread().getName());

                        }

                        //Start timer to check if self report has been submitted
                        startSelfReportCheckTimer(DELAY_FOR_8_MINS);
                    }
                }
            }
        };

        //Starts 2 minute timer
        timerHandler.postDelayed(timerRunnable, DELAY_FOR_2_MINS);
    }


    /**
     *  Starts second 2 minutes timer to check if filling of survey has started
     */
    private void startSecond2minTimer(){
        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {

                //Check again if filling of self report has been started.
                if (!hasSelfReportBeenStarted){

                    //Mark recording as discarded if not completed
                    discardDates = discardDates + getDateNow();

                    //Reset values
                    shouldConnect = true;
                    lastRecordedTime = prevLastRecordedTime; //reset last recorded time
                    hasStartedRecording = false;
                }else{
                    //Check if self report has been completed
                    if (!isSelfReportCompleted){
                        if (DEBUG_MODE == true) {
                            //TODO: Remove trigger message to be displayed
                            msg.triggerMsg("3 mins alert");
                            Log.d("Scheduler", "Recording for 5 mins done: " + new Date() + "n" +
                                    "Thread's name: " + Thread.currentThread().getName());

                        }
                        startSelfReportCheckTimer(DELAY_FOR_6_MINS);
                    }
                }
            }
        };

        //Starts 2 minute timer
        timerHandler.postDelayed(timerRunnable, DELAY_FOR_2_MINS);
    }


    /**
     *  Starts timer to check if survey has been submitted
     */
    private void startSelfReportCheckTimer(long delay){
        TimerTask task = new TimerTask() {
            public void run() {

                //Check again if self report has been completed.
                if (!isSelfReportCompleted) {

                    //Mark recording as discarded if not completed
                    discardDates = discardDates + getDateNow();

                    //Reset values
                    hasStartedRecording = false;
                    lastRecordedTime = prevLastRecordedTime; //reset last recorded time
                    hasStartedRecording = false;
                }
            }
        };

        //Starts timer
        Timer timer = new Timer("Timer");
        timer.schedule(task, delay);
    }


    /**
     * Stops recording
     * @throws FileNotFoundException
     */
    private void stopRecording() throws FileNotFoundException {
        mAudioRecorder.stopRecording();
        mSensorRecorder.stopRecording();

        //Record end of data collection
        dataCollectEndDate = dataCollectEndDate + getDateNow();

        //Update values
        prevLastRecordedTime = lastRecordedTime;
        lastRecordedTime = System.currentTimeMillis();
        recordedInHour = true;

        if (DEBUG_MODE == true){
            msg.triggerMsg("Recording stopped");
            Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            v.vibrate(500); // Vibrate for 500 milliseconds
        }
    }

    @Override
    public void collectDataCallBack() throws FileNotFoundException {
        //Looper.prepare();
        if(!hasStartedRecording && !recordedInHour) {
            startRecording("" + System.currentTimeMillis());
        }
        //Looper.loop();
    }
}
