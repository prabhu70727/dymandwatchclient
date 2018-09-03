package ch.ethz.dymand;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import static ch.ethz.dymand.Config.CHANNEL_ID;


public class FGService extends Service implements Callbacks.MessageCallback {

    private static final String LOG_TAG = "Logs: FGService";
    int ONGOING_NOTIFICATION_ID = Config.NOTIFICATION_ID;
    DataCollection dataCollector;

    public FGService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = null;
        Intent notificationIntent = new Intent(this, SetupActivity.class);
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
        startScheduler();

        dataCollector = DataCollection.getInstance(this);

        return START_NOT_STICKY;
    }

    private void startScheduler(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Scheduler sch = Scheduler.getInstance(FGService.this);

                //Subscribe various callbacks
                sch.subscribeMessageCallback(FGService.this);
                sch.subscribeDataCollectionCallback(dataCollector);
                sch.startHourlyTimer();
            }
        });
        t.start();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // TODO: cleanup
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "In onDestroy");
        Toast.makeText(this, "Service Detroyed!", Toast.LENGTH_SHORT).show();
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
