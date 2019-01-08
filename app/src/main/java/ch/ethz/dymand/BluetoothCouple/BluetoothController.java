package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

import OldCode.LocalTimer;
import ch.ethz.dymand.Callbacks.MessageCallback;
import ch.ethz.dymand.Callbacks.BleCallback;
import ch.ethz.dymand.Config;
import ch.ethz.dymand.DataCollection;
import ch.ethz.dymand.VoiceActivityDetection.VAD;

import static android.content.Context.VIBRATOR_SERVICE;
import static ch.ethz.dymand.Config.DEBUG_MODE;
import static ch.ethz.dymand.Config.closeEnoughDates;
import static ch.ethz.dymand.Config.collectDataDates;
import static ch.ethz.dymand.Config.collectDataNum;
import static ch.ethz.dymand.Config.connectedDates;
import static ch.ethz.dymand.Config.connectedNum;
import static ch.ethz.dymand.Config.errorLogs;
import static ch.ethz.dymand.Config.getDateNow;
import static ch.ethz.dymand.Config.hasStartedRecording;
import static ch.ethz.dymand.Config.closeEnoughNum;
import static ch.ethz.dymand.Config.recordedInHour;
import static ch.ethz.dymand.Config.setShouldConnect;
import static ch.ethz.dymand.Config.shouldConnectStatus;


public class BluetoothController implements
        BluetoothCentralConnect.CentralConnectInterface,
        BluetoothCentralScan.CentralScanInterface,
        BluetoothPeripheral.PeripheralInterface,BleCallback, VAD.DataCollectionListener {

    public static BluetoothController bluetoothController = null;
    BluetoothCentralScan mBluetoothCentralScan;
    BluetoothPeripheral mBluetoothPeripheral;
    BluetoothCentralConnect mBluetoothCentralConnect;
    BluetoothDevice mDevice;


    private Context mContext;
    private VAD voiceDetector;
    private static DataCollection dataCollector;
    private static MessageCallback msg;
    private static Vibrator v;
    private static final String LOG_TAG = "Logs: Bluetooth Controller";

    public synchronized static BluetoothController getInstance(Context context) {
        if (bluetoothController != null) return bluetoothController;
        else {
            dataCollector = DataCollection.getInstance(context);
            bluetoothController = new BluetoothController();
            bluetoothController.mContext = context;
            v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            return bluetoothController;
        }
    }

    public void subscribeMessageCallback(MessageCallback msgInput) {
        msg = msgInput;
    }


    private synchronized void restartScanning(){
        if(mBluetoothCentralScan!=null){
            try {
                mBluetoothCentralScan.stopScan();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBluetoothCentralScan = null;
        }
        mBluetoothCentralScan = new BluetoothCentralScan(mContext, this);
        try {
            mBluetoothCentralScan.startScan();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void restartAdv() {
        if (mBluetoothPeripheral != null) {
            mBluetoothPeripheral.stopAdvertising();
        }
        mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
        mBluetoothPeripheral.startAdvertising();
    }

    public void startBLE() throws IOException {

        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("BLE started??");
        }

        setShouldConnect();

        if (Config.isCentral) {
            mBluetoothCentralScan = new BluetoothCentralScan(mContext, this);
            mBluetoothCentralScan.startScan();
        } else {
            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            mBluetoothPeripheral.startAdvertising();
        }
    }

    // Central device
    @Override
    public void connected() throws FileNotFoundException {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("Central Connected");
        }

        connectedNum++;
        connectedDates = connectedDates + Config.getDateNow();

        Log.i(LOG_TAG, "Central Connected");

        // Disconnect gatt too..
        if(mBluetoothCentralConnect != null){
            mBluetoothCentralConnect.disconnectGattServer();
            mBluetoothCentralConnect = null;
        }

        // restart scanning again before recording..
        restartScanning();
        // restart scanning - end

        // TODO: 18.11.18 remove
        //Config.recordedInHour = false;

        Log.i(LOG_TAG, "Start recording");
        // Code to start recording..
        collectData();
        // After recording

    }

    // Central device
    @Override
    public void notConnected(BluetoothDevice device) {
        Log.i(LOG_TAG, "Not connected");
        setShouldConnect();

        // restart scanning... Gatt is disconnected already..
        restartScanning();
        // restart scanning - end

    }

    // Central device
    @Override
    public void found(BluetoothDevice device) {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("Device found trying for VAD");
        }

        Log.i(LOG_TAG, "Device found trying for VAD");
        voiceDetector = new VAD(this);
        voiceDetector.recordSound();
        mDevice = device;
        Log.i(LOG_TAG, "found() central returned...");
    }

    // peripheral device
    @Override
    public void connected(String timestamp) throws FileNotFoundException {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("Peripheral Connected");

        }
        connectedNum++;
        connectedDates = connectedDates + Config.getDateNow();

        Log.i(LOG_TAG, "Peripheral Connected");

        // restart advertising
        restartAdv();
        // restart advertising - end


        // TODO: 18.11.18 remove
        //Config.recordedInHour = false;

        Log.i(LOG_TAG, "Start recording");
        // Code to start recording..
        //Looper.prepare();
        collectData();
        //Looper.loop();
        // After recording


    }

    @Override
    public void speech() {
        if ((mDevice == null)) throw new AssertionError();
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("Voice detected");

        }

        Log.i(LOG_TAG, "Voice detected trying to connect (also sending a timestamp message)");
        mBluetoothCentralConnect = new BluetoothCentralConnect(mContext, this);
        mBluetoothCentralConnect.connectDevice(mDevice, System.currentTimeMillis() + "");
        //mDevice = null;
    }

    //It is better when we can restart the scanning process.
    @Override
    public void noSpeech() {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("No voice detected");
        }

        // restart scanning...
        restartScanning();
        // restart scanning - end

        setShouldConnect();
    }


    // todo check why mBluetoothManager.openGattServer() returns null
    @Override
    public synchronized void startBleCallback() {
        if ((!(shouldConnectStatus() == true))) {
            Log.e("Logs", "BStart BLE failed");
            errorLogs =  errorLogs + LOG_TAG + ": Start BLE failed "   + " \n";
            throw new AssertionError();
        }

        if (Config.isCentral) {
            restartScanning();
        } else {
            if (mBluetoothPeripheral != null) {
                mBluetoothPeripheral.stopAdvertising();
            }

            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            boolean advertizeDone = mBluetoothPeripheral.startAdvertising();

            if(!advertizeDone){
                LocalTimer.blockingLoop(2000);
                mBluetoothPeripheral = null;
                mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
                if ((!mBluetoothPeripheral.startAdvertising())) {
                    // todo HL
                }
            }
        }

        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("BLE started");
        }
    }

    @Override
    public synchronized void stopBleCallback() {
        if (Config.isCentral) {
            if (mBluetoothCentralScan != null) {
                try {
                    mBluetoothCentralScan.stopScan();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mBluetoothCentralScan = null;
        } else {
            if (mBluetoothPeripheral != null) {
                mBluetoothPeripheral.stopAdvertising();
            }
            mBluetoothPeripheral = null;
        }

        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("BLE stopped");
        }

    }

    @Override
    public void reStartBleCallback() {
        if (Config.isCentral) {
            restartScanning();
        } else {
            restartAdv();
        }

        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("BLE Restarted");
            Log.d(LOG_TAG, "BLE Restarted");
        }
    }

    /**
     * Collects data if data collection in this hour hasn't started
     */
    private static void collectData() throws FileNotFoundException {
        collectDataNum++;
        collectDataDates = collectDataDates + getDateNow();
        //+ "H: " + hasStartedRecording + " R: " + recordedInHour;

        if (!hasStartedRecording) {
            if (dataCollector != null) {
                Looper.prepare();
                Log.i(LOG_TAG, "to data collection callback");
                dataCollector.collectDataCallBack();
                Looper.loop();
            }

        }

    }

}
