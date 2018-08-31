package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.io.IOException;

import ch.ethz.dymand.Config;


public class BluetoothController implements
        BluetoothCentralConnect.CentralConnectInterface,
        BluetoothCentralScan.CentralScanInterface,
        BluetoothPeripheral.PeripheralInterface {

    BluetoothCentralScan mBluetoothCentralScan;
    BluetoothPeripheral mBluetoothPeripheral;
    BluetoothCentralConnect mBluetoothCentralConnect;

    private final Context mContext;

    public BluetoothController(Context context) {
        mContext = context;
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
    @Override
    public void connected() {
        Log.i(LOG_TAG, "Central Connected");
        Log.i(LOG_TAG, "Start recording");


        // --- start of Example code to stop scanning
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            mBluetoothCentralScan.stopScan();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // --- end of Example code to stop scanning

    }

    // Central device
    @Override
    public void notConnected(BluetoothDevice device) {
        Log.i(LOG_TAG, "Not connected");
        // maybe retry connection... or just scanning part?
    }

    // Central device

    @Override
    public void found(BluetoothDevice device) {
        // I am skipping the code to try for VAD... after VAD returns true, we use the below code for it's callback and not here
        // The below code should be put in VAD's return
        Log.i(LOG_TAG, "Device found trying to connect (also sending a timestamp message)");
        mBluetoothCentralConnect = new BluetoothCentralConnect(mContext, this);
        mBluetoothCentralConnect.connectDevice(device,System.currentTimeMillis()+"");
    }

    // peripheral device
    @Override
    public void connected(String timestamp) {
        Log.i(LOG_TAG, "Peripheral Connected");
        Log.i(LOG_TAG, "Start recording");
        // Code to start recording..
        // After recording

        //-- start Example code to stop advertising
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBluetoothPeripheral.stopAdvertising();
        // -- end Example code to stop advertising

    }
}
