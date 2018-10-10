package OldCode;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import ch.ethz.dymand.Audio.BackgroundAudioRecorder;
import ch.ethz.dymand.Bluetooth.BluetoothCentral;
import ch.ethz.dymand.Bluetooth.BluetoothPeripheral;
import ch.ethz.dymand.Config;
import ch.ethz.dymand.R;
import ch.ethz.dymand.Sensors.SensorRecorder;

import java.io.File;
import java.io.FileNotFoundException;


public class FGService_depricated extends Service {

    private static final String LOG_TAG = "Logs: FGService";
    int ONGOING_NOTIFICATION_ID = Config.NOTIFICATION_ID;
    BackgroundAudioRecorder mAudioRecorder;
    SensorRecorder mSensorRecorder;
    BluetoothCentral mBluetoothCentral = new BluetoothCentral(this);
    BluetoothPeripheral mBluetoothPeripheral = new BluetoothPeripheral(this);
    public static boolean isCentral = Config.isCentral;
    private int syncBufferTime = Config.SYNC_BUFFER;
    private boolean bluetoothPursuitEnabled = false;


    public FGService_depricated() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, SetupActivity_Old.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.icon_steth)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
        startIntervention();
        return START_NOT_STICKY;
    }

    private void startIntervention() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startProcess();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void startProcess() throws FileNotFoundException {
        LocalTimer.synchronize(); //To sync between central and peripheral
        Log.i(LOG_TAG, "Starting intervention...");
        LocalTimer.start();
        mAudioRecorder = new BackgroundAudioRecorder(this);
        mSensorRecorder = new SensorRecorder(this);


        while(LocalTimer.online()){
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
        }
        Log.i(LOG_TAG, "Stopping intervention...");
        stopForeground(true);
        stopSelf();
    }

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
        // Can be made better to wait the thread intrad of in a loop.
        if (isCentral) LocalTimer.blockingLoop(Config.RECORD_TIME + syncBufferTime);
        else LocalTimer.blockingLoop(Config.RECORD_TIME);

        stopRecording();

    }

    private void stopRecording() throws FileNotFoundException {
        mAudioRecorder.stopRecording();
        mSensorRecorder.stopRecording();
    }

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
    private boolean isVoice() {
        if(!isCentral) return true;
        //TO DO
        return true;
    }

    // TODO: Disable bluetooth if not already
    private void disableBluetoothPursuit() {
        if (!bluetoothPursuitEnabled) throw new AssertionError();
        if (isCentral) mBluetoothCentral.stop();
        else mBluetoothPeripheral.stop();
        bluetoothPursuitEnabled = false;
    }

    // Check couple distance
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
    private void enableBluetoothPursuit() {
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
        disableBluetoothPursuit();
        try {
            stopRecording();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "In onDestroy");
        Toast.makeText(this, "Service Detroyed!", Toast.LENGTH_SHORT).show();
    }


}
