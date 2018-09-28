package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

import ch.ethz.dymand.Callbacks.MessageCallback;
import ch.ethz.dymand.Callbacks.BleCallback;
import ch.ethz.dymand.Config;
import ch.ethz.dymand.DataCollection;
import ch.ethz.dymand.VoiceActivityDetection.VAD;

import static android.content.Context.VIBRATOR_SERVICE;
import static ch.ethz.dymand.Config.DEBUG_MODE;
import static ch.ethz.dymand.Config.hasStartedRecording;


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

    public static BluetoothController getInstance(Context context) {
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

    public void startBLE() throws IOException {

        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("BLE started??");
        }

        Config.shouldConnect = true; // may be an assertion..
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
            Log.i(LOG_TAG, "Central Connected");
            Log.i(LOG_TAG, "Start recording");
        }

        // Code to start recording..
        collectData();
        // After recording
    }

    // Central device
    @Override
    public void notConnected(BluetoothDevice device) {
        Log.i(LOG_TAG, "Not connected");
        // maybe retry connection... or scanning part?
        Config.shouldConnect = true;
    }

    // Central device
    @Override
    public void found(BluetoothDevice device) {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("Device found trying for VAD");
            Log.i(LOG_TAG, "Device found trying for VAD");
        }

        voiceDetector = new VAD(this);
        voiceDetector.recordSound();
        mDevice = device;
    }

    // peripheral device
    @Override
    public void connected(String timestamp) throws FileNotFoundException {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("Peripheral Connected");
            Log.i(LOG_TAG, "Peripheral Connected");
            Log.i(LOG_TAG, "Start recording");
        }

        // Code to start recording..\
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
            Log.i(LOG_TAG, "Voice detected trying to connect (also sending a timestamp message)");
        }

        mBluetoothCentralConnect = new BluetoothCentralConnect(mContext, this);
        mBluetoothCentralConnect.connectDevice(mDevice, System.currentTimeMillis() + "");
        //mDevice = null;
    }

    //todo: prabhu: Logically it works, but still it is better if we can restart the scanning/adv process.
    @Override
    public void noSpeech() {
        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("No voice detected");
        }

        Config.shouldConnect = true;
    }


    @Override
    public void startBleCallback() {
        if ((!(Config.shouldConnect == true))) throw new AssertionError();

        if (Config.isCentral) {
            if (mBluetoothCentralScan != null) {
                try {
                    mBluetoothCentralScan.stopScan();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mBluetoothCentralScan = new BluetoothCentralScan(mContext, this);
            try {
                mBluetoothCentralScan.startScan();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (mBluetoothPeripheral != null) {
                mBluetoothPeripheral.stopAdvertising();
            }
            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            mBluetoothPeripheral.startAdvertising();
        }

        if (DEBUG_MODE == true) {
            v.vibrate(500); // Vibrate for 500 milliseconds
            msg.triggerMsg("BLE started");
        }
    }

    @Override
    public void stopBleCallback() {
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
            if (mBluetoothCentralScan != null) {
                try {
                    mBluetoothCentralScan.stopScan();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mBluetoothCentralScan = new BluetoothCentralScan(mContext, this);
            try {
                mBluetoothCentralScan.startScan();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (mBluetoothPeripheral != null) {
                mBluetoothPeripheral.stopAdvertising();
            }
            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            mBluetoothPeripheral.startAdvertising();
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
        if (!hasStartedRecording) {
            if (dataCollector != null) {
                Looper.prepare();
                dataCollector.collectDataCallBack();
                Looper.loop();
            }

        }

    }

}
