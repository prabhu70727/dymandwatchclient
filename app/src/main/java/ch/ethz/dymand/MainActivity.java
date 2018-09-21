package ch.ethz.dymand;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import ch.ethz.dymand.VoiceActivityDetection.VAD;


public class MainActivity extends WearableActivity implements VAD.DataCollectionListener, Callbacks.DataCollectionCallback, Callbacks.BleCallback {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    private static String LOG_TAG="Logs: MainActivity";
    private static final int REQUEST_ALL_PERMISSION = 200;
    private boolean permissionToAllAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BODY_SENSORS, Manifest.permission.BLUETOOTH};
    Button mButton;
    private BluetoothAdapter mBluetoothAdapter; // to check the capabilities of BLE
    private BluetoothManager mbluetoothManager;
    private VAD voiceDetector;
    private MyWakefulReceiver receiver;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Log.i(LOG_TAG, "Length of grant Results:" + grantResults.length);
        switch (requestCode){
            case REQUEST_ALL_PERMISSION:
                if(grantResults.length > 0)
                    permissionToAllAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToAllAccepted ) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Device does not support BLE.");
            finish();
        }

        mbluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mbluetoothManager.getAdapter();

        setAmbientEnabled();

        mButton = findViewById(R.id.button2);
        mButton.setEnabled(false);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSION); //Non blocking return
        boolean check = true;
        while(check) {
            //Log.i(LOG_TAG, "check");
            boolean allPermissionsAccepted = true;
            for(String permission: permissions){
                if(!(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)){
                    allPermissionsAccepted = false;
                }
            }
            if(allPermissionsAccepted){
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
                    requestBluetoothEnable();
                check = false;
                mButton.setEnabled(true);
            }
        }


        /*SensorManager mSensorManager;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor:deviceSensors){
            Log.i(LOG_TAG, sensor.getStringType()+" "+sensor.getName());
        }*/

        //TODO: remove
        startService();

        //TODO: remove
        //VAD example
        voiceDetector = new VAD(MainActivity.this);
        voiceDetector.recordSound();



//        Intent intent = new Intent(this, MyWakefulReceiver.class);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        //scheduleAlarm();
    }

    //TODO: Move to terminal activity in the set up process
    private void startService(){
        if(isMyServiceRunning(FGService.class)) {
            Toast.makeText(this, "Service exists. Kill it before starting a new one...", Toast.LENGTH_SHORT).show();
            return;
        }

        FGService.acquireStaticLock(this);
        Intent mService = new Intent(this, FGService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mService);
        }
        else{

            startService(mService);
        }

        Toast.makeText(this, "Starting service: ", Toast.LENGTH_SHORT).show();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startSetupActivityOnClicked(View view) {
        Log.i(LOG_TAG, "Setup Intent starting...");
        Intent intent = new Intent(this, SetupActivity.class);
        startActivity(intent);
    }

    public void startBluetoothSetupActivityOnClicked(View view) {
        Log.i(LOG_TAG, "Bluetooth Setup Intent starting...");
        Intent intent = new Intent(this, BlutoothSetupActivity.class);
        startActivity(intent);
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.i(LOG_TAG, "Requested user enables Bluetooth.");
    }


    //TODO Implement these function
    @Override
    public void speech() {
        Log.d("Data Collection", "Collecting sensor data...");
    }

    //TODO Implement these function
    @Override
    public void noSpeech(){
        Log.d("Data Collection", "No data collection...");
    }

    @Override
    public void startBleCallback() {

    }

    @Override
    public void reStartBleCallback() {

    }

    @Override
    public void stopBleCallback() {

    }

    @Override
    public void collectDataCallBack() {

    }


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
                    sch.startHourlyTimer();

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

    // Setup a recurring alarm every half hour
    public void scheduleAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), MyWakefulReceiver.class);
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

}
