package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.io.IOException;

import ch.ethz.dymand.Callbacks;
import ch.ethz.dymand.Config;
import ch.ethz.dymand.MainActivity;
import ch.ethz.dymand.VoiceActivityDetection.VAD;


public class BluetoothController implements
        BluetoothCentralConnect.CentralConnectInterface,
        BluetoothCentralScan.CentralScanInterface,
        BluetoothPeripheral.PeripheralInterface,
        Callbacks.BleCallback, VAD.DataCollectionListener {

    public static BluetoothController bluetoothController = null;
    BluetoothCentralScan mBluetoothCentralScan;
    BluetoothPeripheral mBluetoothPeripheral;
    BluetoothCentralConnect mBluetoothCentralConnect;
    BluetoothDevice mDevice;

    private Context mContext;
    private VAD voiceDetector;

    public BluetoothController getInstance(Context context) {
        if(bluetoothController!=null) return bluetoothController;
        else {
            bluetoothController = new BluetoothController();
            bluetoothController.mContext = context;
            return bluetoothController;
        }
    }

    private static final String LOG_TAG = "Logs: BluetoothController";

    public void startBLE() throws IOException {
        Config.shouldConnect = true; // may be an assertion..
        if(Config.isCentral) {
            mBluetoothCentralScan = new BluetoothCentralScan(mContext, this);
            mBluetoothCentralScan.startScan();
        }
        else {
            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            mBluetoothPeripheral.startAdvertising();
        }
    }

    // Central device
    // todo: george.. callback for data collection
    @Override
    public void connected() {
        Log.i(LOG_TAG, "Central Connected");
        Log.i(LOG_TAG, "Start recording");
        // Code to start recording..

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
        Log.i(LOG_TAG, "Device found trying for VAD");
        voiceDetector = new VAD(this);
        voiceDetector.recordSound();
        mDevice = device;
    }

    // peripheral device
    @Override
    public void connected(String timestamp) {
        Log.i(LOG_TAG, "Peripheral Connected");
        Log.i(LOG_TAG, "Start recording");
        // Code to start recording..
        // After recording
    }

    @Override
    public void speech() {
        if ((mDevice == null)) throw new AssertionError();
        Log.i(LOG_TAG, "Voice detected trying to connect (also sending a timestamp message)");
        mBluetoothCentralConnect = new BluetoothCentralConnect(mContext, this);
        mBluetoothCentralConnect.connectDevice(mDevice,System.currentTimeMillis()+"");
        //mDevice = null;
    }

    //todo: prabhu: Logically it works, but still it is better if we can restart the scanning/adv process.
    @Override
    public void noSpeech() {
        Config.shouldConnect = true;
    }


    @Override
    public void startBleCallback()  {
        if ((!(Config.shouldConnect == true))) throw new AssertionError();

        if(Config.isCentral) {
            if(mBluetoothCentralScan!=null){
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
        }
        else {
            if(mBluetoothPeripheral!=null){
                mBluetoothPeripheral.stopAdvertising();
            }
            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            mBluetoothPeripheral.startAdvertising();
        }
    }

    @Override
    public void stopBleCallback() {
        if(Config.isCentral) {
            if(mBluetoothCentralScan!=null){
                try {
                    mBluetoothCentralScan.stopScan();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mBluetoothCentralScan = null;
        }
        else {
            if(mBluetoothPeripheral!=null){
                mBluetoothPeripheral.stopAdvertising();
            }
            mBluetoothPeripheral = null;
        }

    }

    @Override
    public void reStartBleCallback() {
        if(Config.isCentral) {
            if(mBluetoothCentralScan!=null){
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
        }
        else {
            if(mBluetoothPeripheral!=null){
                mBluetoothPeripheral.stopAdvertising();
            }
            mBluetoothPeripheral = new BluetoothPeripheral(mContext, this);
            mBluetoothPeripheral.startAdvertising();
        }
    }
}
