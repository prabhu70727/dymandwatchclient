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
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ch.ethz.dymand.Audio.BackgroundAudioRecorder;
import ch.ethz.dymand.Bluetooth.BluetoothCentral;
import ch.ethz.dymand.Bluetooth.BluetoothPeripheral;
import ch.ethz.dymand.BluetoothCouple.BluetoothController;
import ch.ethz.dymand.Sensors.SensorRecorder;

import static ch.ethz.dymand.Config.CHANNEL_ID;


public class FGService_old_version extends Service implements DataClient.OnDataChangedListener {

    private static final String LOG_TAG = "Logs: FGService";
    int ONGOING_NOTIFICATION_ID = Config.NOTIFICATION_ID;
    BackgroundAudioRecorder mAudioRecorder;
    SensorRecorder mSensorRecorder;
    BluetoothCentral mBluetoothCentral = new BluetoothCentral(this);
    BluetoothPeripheral mBluetoothPeripheral = new BluetoothPeripheral(this);
    public static boolean isCentral = Config.isCentral;
    private int syncBufferTime = Config.SYNC_BUFFER;
    private boolean bluetoothPursuitEnabled = false;
    private static final String INTERVENE_KEY = "com.example.key.intervention";

    private static final String INTERVENTION = "/intervention";
    private static final String START_INTERVENTION_MESSAGE = "start_intervention";
    private static final String INTERVENTION_ACK = "intervention_ack";

    private static final String SEND_INTENT_MESSAGE = "send_intent";
    private static final String INTENT_ACK = "intent_ack";
    public static boolean intentSent = false;


    public FGService_old_version() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Wearable.getDataClient(this).addListener(this).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(LOG_TAG, "Success while adding listener");
            }
        });
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
        startIntervention();
        return START_NOT_STICKY;
    }

    private void startIntervention() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //startProcess();
                    // startDemo(); // Its demo
                    //doNothing();
                    startBluetoothModularityTest();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void startBluetoothModularityTest() throws IOException {
        BluetoothController bluetoothController = new BluetoothController(this);
        bluetoothController.startBLE();
    }

    private void doNothing() throws FileNotFoundException  {
    }

    //todo remove
    private void startDemo() throws FileNotFoundException  {

        Log.i(LOG_TAG, "In demo");
        LocalTimer.start();
        mAudioRecorder = new BackgroundAudioRecorder(this);
        mSensorRecorder = new SensorRecorder(this);

        while (LocalTimer.online()){
            if(WatchMobileInterface.intervene()){
                enableBluetoothPursuit();
                if(isCentral) {
                    String timeStamp = LocalTimer.timeStamp();
                    // The sendSignalToPeriToRecord returns if the devices are not connected.
                    // Also the devices can connect only if they are found and they can be only found if
                    // the distance is less than the threshold.
                    if(mBluetoothCentral.sendSignalToPeriToRecord(timeStamp)){

                        disableBluetoothPursuit();
                        record(mBluetoothCentral.getTimeStamp());
                        sendIntention();
                        break;
                    }
                    else if(!mBluetoothCentral.sendSignalToPeriToRecord(timeStamp) &&
                            mBluetoothCentral.isDeviceFoundConnectionInterrupted()){
                        restartBluetoothPursuit();
                    }
                }
                else{
                    if(mBluetoothPeripheral.canStartRecording()) {

                        String timeStamp = mBluetoothPeripheral.getTimeStamp();
                        disableBluetoothPursuit();
                        record(timeStamp);
                        sendIntention();
                        break;
                    }
                }
            }
        }
    }

    //todo remove
    private void startProcess() throws FileNotFoundException {
        WatchMobileInterface.start();
        mAudioRecorder = new BackgroundAudioRecorder(this);
        mSensorRecorder = new SensorRecorder(this);


        // the current prototype wants the app to be central
        if (!(isCentral)) throw new AssertionError();


        while(WatchMobileInterface.online()){
            if(WatchMobileInterface.recordThisHour()){
                enableBluetoothPursuit();
                if(isCentral) {
                    String timeStamp = WatchMobileInterface.timeStamp();
                    // The sendSignalToPeriToRecord returns if the devices are not connected.
                    // Also the devices can connect only if they are found and they can be only found if
                    // the distance is less than the threshold.
                    if(mBluetoothCentral.sendSignalToPeriToRecord(timeStamp)){
                        disableBluetoothPursuit();

                        //Parallel bluetooth recording start

                        record(mBluetoothCentral.getTimeStamp());

                        //

                        //

                        WatchMobileInterface.disableRecordingThisHour(); // this will stop/disable "Record this hour" for 55 min, during which parallel bluetooth recording can happen.

                        stopIntervention();
                    }
                    else if(!mBluetoothCentral.sendSignalToPeriToRecord(timeStamp) &&
                            mBluetoothCentral.isDeviceFoundConnectionInterrupted()){
                        restartBluetoothPursuit();
                    }
                }
                else{
                    if(mBluetoothPeripheral.canStartRecording()) {
                        String timeStamp = mBluetoothPeripheral.getTimeStamp();
                        disableBluetoothPursuit();

                        //Parallel bluetooth recording start

                        record(timeStamp);
                        WatchMobileInterface.disableRecordingThisHour(); // this will stop/disable "Record this hour" for 55 min, during which parallel bluetooth recording can happen.
                    }
                }
            }
        }

        /*while(LocalTimer.online()){
            if(LocalTimer.recordThisHour()) {
                enableBluetoothPursuit(); // returns if bluetooth process is already enabled.
                if(isCentral) {
                    String timeStamp = LocalTimer.timeStamp();
                    // The sendSignalToPeriToRecord returns if the devices are not connected.
                    // Also the devices can connect only if they are found and they can be only found if
                    // the distance is less than the threshold.
                    if(isVoice() && mBluetoothCentral.sendSignalToPeriToRecord(timeStamp)){
                        disableBluetoothPursuit();
                        record(mBluetoothCentral.getTimeStamp());
                        LocalTimer.disableRecordingThishour(); // may not be needed when we use server to control the time
                    }
                    else if(!mBluetoothCentral.sendSignalToPeriToRecord(timeStamp) &&
                            mBluetoothCentral.isDeviceFoundConnectionInterrupted()){
                        restartBluetoothPursuit();
                    }
                }
                else{
                    if(mBluetoothPeripheral.canStartRecording()) {
                        String timeStamp = mBluetoothPeripheral.getTimeStamp();
                        disableBluetoothPursuit();
                        record(timeStamp);
                        LocalTimer.disableRecordingThishour();
                    }
                }
            }
        }*/

        Log.i(LOG_TAG, "Stopping intervention...");
        stopForeground(true);
        stopSelf();
    }

    // not a good name...
    //todo remove
    private void stopIntervention() {
        sendIntention();
    }


    // TODO: what if connection failed. Two while loops not good
    private void sendIntention() {
        long end_time_intention = WatchMobileInterface.curTime() + Config.INTENT_EXPIRY;
        while (WatchMobileInterface.curTime() < end_time_intention){
            LocalTimer.blockingLoop(2000);
            if(!intentSent) {
                sendIntentMessage(SEND_INTENT_MESSAGE);
            }
            else {
                intentSent = false;
                break;
            }
        }
    }

    private void sendIntentMessage(String message) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(INTERVENTION);
        putDataMapReq.getDataMap().putString(INTERVENE_KEY, message + (System.currentTimeMillis()%100000));
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.isUrgent();
        Wearable.getDataClient(this).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.i(LOG_TAG, "Sending intent was successful: " + dataItem);
            }
        });
    }


    //todo remove
    private void restartBluetoothPursuit() {
        disableBluetoothPursuit();
        enableBluetoothPursuit();
    }



    private void record(String timeStamp) throws FileNotFoundException {
        Log.i(LOG_TAG, "In recordVoice");

        startRecording(timeStamp);


        //very important to remove
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
        //vibrate break
        LocalTimer.blockingLoop(2000);

        // This is where the service would wait till the recording is done!!!
        // Can be made better to wait the thread instead of in a loop.
        if (isCentral) LocalTimer.blockingLoop(Config.RECORD_TIME + syncBufferTime);
        else LocalTimer.blockingLoop(Config.RECORD_TIME);

        stopRecording();

    }


    //todo use and remove
    private void stopRecording() throws FileNotFoundException {
        mAudioRecorder.stopRecording();
        mSensorRecorder.stopRecording();
    }

    //todo use and remove
    private void startRecording(String timeStamp) throws FileNotFoundException {

        String tag = "app_";
        if(isCentral) tag = tag+"black_";
        else tag = tag + "white_";

        File recordDir = new File(this.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+tag+timeStamp);
        recordDir.mkdirs();
        String dirPath = this.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+tag+timeStamp+"/";

        mAudioRecorder.startRecording(dirPath); // non blocking return.
        mSensorRecorder.startRecording(dirPath); // non blocking return.
    }


    // TODO: VAD, currently only central needs to do this...
    //todo remove
    private boolean isVoice() {
        if(!isCentral) return true;
        //TO DO
        return true;
    }

    //todo remove
    // TODO: Disable bluetooth if not already
    private void disableBluetoothPursuit() {
        if (!bluetoothPursuitEnabled) throw new AssertionError();
        if (isCentral) mBluetoothCentral.stop();
        else mBluetoothPeripheral.stop();
        bluetoothPursuitEnabled = false;
    }

    // Check couple distance
    //todo remove
    private boolean isCoupleCloseEnough() {
        //Log.i(LOG_TAG, "In isCoupleCloseEnough");
        if (isCentral != true) throw new AssertionError();

        int distance = mBluetoothCentral.checkDistance();
        if(distance < Config.threshold){
            Log.i(LOG_TAG, "The distance is "+distance+".");
            return true;
        }
        else return false;

    }

    // enable bluetooth if not already
    //todo use and remove
    private void enableBluetoothPursuit() {

        // stop parallel bluetooth pursuit


        if(bluetoothPursuitEnabled) return;
        bluetoothPursuitEnabled = true;
        if (isCentral) mBluetoothCentral.scan();
        else mBluetoothPeripheral.advertize();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // TODO: cleanup
    public void onDestroy() {
        super.onDestroy();
        //disableBluetoothPursuit();
        try {
            stopRecording();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "In onDestroy");
        Toast.makeText(this, "Service Detroyed!", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.i(LOG_TAG, "onDataChanged in watch.");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(INTERVENTION) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    update(dataMap.getString(INTERVENE_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    //todo use (`prabhu) and remove
    private void update(String string) {
        if(string.contains(START_INTERVENTION_MESSAGE)){
            Log.i(LOG_TAG, "Start Intervention is received.");
            WatchMobileInterface.setStartIntervention();
            sendAckToMobile();
        }
        else if (string.contains(INTENT_ACK)){
            Log.i(LOG_TAG, "Intent ACK is received.");
            intentSent = true;
        }
    }

    //todo use (`prabhu) and remove
    private void sendAckToMobile() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(INTERVENTION);
        putDataMapReq.getDataMap().putString(INTERVENE_KEY, INTERVENTION_ACK +  (System.currentTimeMillis()%100000));
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.isUrgent();
        Wearable.getDataClient(this).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.i(LOG_TAG, "Sending intervention ack was successful: " + dataItem);
            }
        });
    }
}
