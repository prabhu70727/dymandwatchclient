package ch.ethz.dymand;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import OldCode.SetupActivity_Old;
import ch.ethz.dymand.BluetoothCouple.BluetoothController;

import static ch.ethz.dymand.Config.CHANNEL_ID;


public class FGService extends Service implements Callbacks.MessageCallback {

    private static final String LOG_TAG = "Logs: FGService";
    int ONGOING_NOTIFICATION_ID = Config.NOTIFICATION_ID;
    DataCollection dataCollector;
    BluetoothController bleController;
    private static PowerManager.WakeLock lockStatic=null;
    private PowerManager.WakeLock lockLocal=null;
    private Scheduler sch;

    public FGService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        PowerManager.WakeLock lockLocal = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                "MyApp::MyWakelockTag");
//        lockLocal.setReferenceCounted(true);
//        lockLocal.acquire();
//        PowerManager.WakeLock lock = getLock(this);
//        Log.d(LOG_TAG, lock.toString());
//        lock.release();
    }

    /**
     * Acquire a partial static WakeLock, you need too call this within the class
     * that calls startService()
     * @param context
     */
    public static void acquireStaticLock(Context context) {
        getLock(context).acquire();
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic==null) {
            PowerManager
                    mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "LOCK_NAME_STATIC");
            lockStatic.setReferenceCounted(true);
        }
        return(lockStatic);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        Notification notification = null;
        Intent notificationIntent = new Intent(this, SetupActivity_Old.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID,
                    "Dymand Foregeound Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            notification = new NotificationCompat.Builder(this, Config.CHANNEL_ID)
                    .setContentTitle(getText(R.string.notification_title))
                    .setContentText(getText(R.string.notification_message))
                    .setSmallIcon(R.drawable.icon_steth)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        }
        else {
            notification =
                    new Notification.Builder(this)
                            .setContentTitle(getText(R.string.notification_title))
                            .setContentText(getText(R.string.notification_message))
                            .setSmallIcon(R.drawable.icon_steth)
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .build();
        }

        startForeground(ONGOING_NOTIFICATION_ID, notification);


        dataCollector = DataCollection.getInstance(this);
        bleController = BluetoothController.getInstance(this);
        startScheduler();

        //TODO: What to do if service killed and restarted?
        //return START_STICKY;
        return START_NOT_STICKY;
    }

    private void startScheduler(){
//        Thread t = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                Looper.prepare();


                sch = Scheduler.getInstance(FGService.this);

                //Subscribe various callbacks
                sch.subscribeMessageCallback(FGService.this);
                dataCollector.subscribeMessageCallback(FGService.this);
                bleController.subscribeMessageCallback(FGService.this);
                sch.subscribeDataCollectionCallback(dataCollector);
                sch.subscribeBleCallback(bleController);
                //sch.startDemoTimer();

        try {
            sch.startHourlyTimer();
        } catch (IOException e) {
            e.printStackTrace();
        }

//                Looper.loop();
//            }
//        });
//        t.start();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // TODO: cleanup
    public void onDestroy() {
        Log.i(LOG_TAG, "In onDestroy");
        Toast.makeText(this, "Service Detroyed!", Toast.LENGTH_SHORT).show();

        //Log status
        try {
            sch.logStatus();

        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
        //TODO: Send broadcast message to restart service if killed
    }


    @Override
    public void triggerMsg(final String  msg) {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                Toast.makeText(FGService.this, msg, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }).start();
    }
}
