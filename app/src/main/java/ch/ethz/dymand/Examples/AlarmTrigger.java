package ch.ethz.dymand.Examples;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;

import ch.ethz.dymand.Callbacks;
import ch.ethz.dymand.DataCollection;
import ch.ethz.dymand.FGService;
import ch.ethz.dymand.Scheduler;

public class AlarmTrigger {
    private MyWakefulReceiver receiver;
    //        Intent intent = new Intent(this, MyWakefulReceiver.class);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    //scheduleAlarm();
    public static class MyWakefulReceiver extends WakefulBroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Main Activity", "Wakeful Receiver");
            // Start the service, keeping the device awake while the service is
            // launching. This is the Intent to deliver to the service.
            Intent service = new Intent(context, MyIntentService.class);
            startWakefulService(context, service);
        }
    }

    public static class MyIntentService extends IntentService implements Callbacks.MessageCallback {
        public static final int NOTIFICATION_ID = 1;
        private NotificationManager mNotificationManager;
        NotificationCompat.Builder builder;
        DataCollection dataCollector;
        private static long DELAY_FOR_60_MINS = 5 * 60 * 1000;

        public MyIntentService() {
            super("MyIntentService");
        }


        @Override
        protected void onHandleIntent(Intent intent) {
            Bundle extras = intent.getExtras();
            // Do the work that requires your app to keep the CPU running.


            //startService();

            Log.d("","Intent service handling intent");

            startScheduler();
            //dataCollector = DataCollection.getInstance(this);


            // ...
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            //MyWakefulReceiver.completeWakefulIntent(intent);
        }

        private void startService(){
//            if(isMyServiceRunning(FGService.class)) {
//                Toast.makeText(this, "Service exists. Kill it before starting a new one...", Toast.LENGTH_SHORT).show();
//                return;
//            }

            Intent mService = new Intent(this, FGService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(mService);
            }
            else{

                startService(mService);
            }

            Toast.makeText(this, "Starting service: ", Toast.LENGTH_SHORT).show();
        }

        private void startScheduler(){
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {


                    Looper.prepare();
                    //createTimer();


                    Scheduler sch = Scheduler.getInstance(MyIntentService.this);

                    //Subscribe various callbacks
                    sch.subscribeMessageCallback(MyIntentService.this);
                    sch.subscribeDataCollectionCallback(dataCollector);
                    try {
                        sch.startHourlyTimer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Looper.loop();

                }
            });
            t.start();
        }

        @Override
        public void triggerMsg(final String  msg) {
            new Thread(new Runnable() {
                public void run() {
                    Looper.prepare();
                    Toast.makeText(MyIntentService.this, msg, Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }).start();
        }


        public void createTimer(){
            //Create timer using handler and runnable
            final Handler timerHandler = new Handler();

            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {


                    //TODO: Remove Trigger message to be displayed
                    Toast.makeText(MyIntentService.this, "Start of new hour", Toast.LENGTH_LONG).show();

                    Log.d("Scheduler","New hour start task performed on " + new Date());

                    //TODO: Remove vibrator test in final version
                    Vibrator v = (Vibrator)  getSystemService(VIBRATOR_SERVICE);
                    v.vibrate(500); // Vibrate for 500 milliseconds

                    timerHandler.postDelayed(this, DELAY_FOR_60_MINS);
                }
            };

            timerHandler.postDelayed(timerRunnable, 5000);
        }
    }

    /*
    // Setup a recurring alarm every half hour
    public void scheduleAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), MainActivity.MyWakefulReceiver.class);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setup periodic alarm every every half hour from this point onwards
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
//        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
////                AlarmManager.INTERVAL_HALF_HOUR, pIntent);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() +
                        1000, pIntent);
        Log.d("Main Activity", "Schedule Alarm");

    }
    */
}
