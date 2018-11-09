package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.dymand.Config;

import static ch.ethz.dymand.Config.SERVICE_UUID;
import static ch.ethz.dymand.Config.bleSSFile;
import static ch.ethz.dymand.Config.closeEnoughDates;
import static ch.ethz.dymand.Config.closeEnoughNum;
import static ch.ethz.dymand.Config.errorLogs;
import static ch.ethz.dymand.Config.getDateNow;
import static ch.ethz.dymand.Config.scanStartDates;
import static ch.ethz.dymand.Config.scanWasStarted;
import static ch.ethz.dymand.Config.startScanTriggerDates;
import static ch.ethz.dymand.Config.startScanTriggerNum;

public class BluetoothCentralScan {
    private static final String LOG_TAG = "Logs: Bluetooth Central Scan";

    private boolean mScanning = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final Context mContext;
    private Map<String, Pair<BluetoothDevice, Integer>> mScanResults;
    private BleScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private FileOutputStream bleSSFileStream;
    private CentralScanInterface mCentralScanListerner;

    public BluetoothCentralScan(Context context, CentralScanInterface centralScanListerner) {
        mContext = context;
        mCentralScanListerner = centralScanListerner;
    }


    public void startScan() throws IOException {
        startScanTriggerNum++;
        startScanTriggerDates = startScanTriggerDates + Config.getDateNow();

        if(!mScanning){
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            mScanResults = new HashMap<>();
            mScanCallback = new BluetoothCentralScan.BleScanCallback(mScanResults);
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                    .build();
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(scanFilter);

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(Config.SCAN_MODE_BLUETOOTH)
                    .build();

            //Asynchronous return? need to check
            Log.i(LOG_TAG, "Bluetooth started Scanning");
            mScanning = true;

            scanWasStarted = scanWasStarted + mScanning;
            scanStartDates = scanStartDates + Config.getDateNow();

            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

//            Log.i(LOG_TAG, "About to create BLE Log file" );
//            String dirPath = mContext.getApplicationContext().getFilesDir().getAbsolutePath();
//            bleSSFile = new File(dirPath, "Bluetooth_signal_strength_log");
//
//            if(!bleSSFile.exists()){
//                bleSSFile.createNewFile();
//            }

//            Log.i(LOG_TAG, "BLE Log File created: " + bleSSFile.getAbsolutePath());
            bleSSFileStream = new FileOutputStream(bleSSFile, true);


        }
    }

    public void stopScan() throws IOException {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            Log.i(LOG_TAG, "Bluetooth stopped Scanning");
            mBluetoothLeScanner.stopScan(mScanCallback);

            if (bleSSFileStream != null){
                bleSSFileStream.close();
            }

        }
        mBluetoothManager = null;
        mScanning = false;
        mScanResults = null;
        //bleSSFile = null;
    }

    private class BleScanCallback extends ScanCallback {

        private Map<String, Pair<BluetoothDevice, Integer>> mScanResults;

        public BleScanCallback(Map<String, Pair<BluetoothDevice, Integer>> scanResults) {
            mScanResults = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                addScanResult(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                try {
                    addScanResult(result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Logs", "BLE Scan Failed with code " + errorCode);
            errorLogs =  errorLogs + LOG_TAG + ": BLE Scan Failed with code " + errorCode  + " \n";
            mScanning = false;

            scanWasStarted = scanWasStarted + mScanning;
            scanStartDates = scanWasStarted + Config.getDateNow();
        }

        //TODO : Synchronized? test it..
        synchronized private void addScanResult(ScanResult result) throws IOException {
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();

            Log.i(LOG_TAG, "Signal strength is " +  rssi);
            String toWrite = getDateNow() + "," + System.currentTimeMillis()+","+rssi+"\n";
            bleSSFileStream.write(toWrite.getBytes());

            if(Config.shouldConnect){
                if(rssi >= Config.threshold) {

                    //Record closeness info
                    closeEnoughNum++;
                    closeEnoughDates = closeEnoughDates + " | " + getDateNow();

                    Log.i(LOG_TAG, "To connect callback");
                    mCentralScanListerner.found(device);
                    Config.shouldConnect = false;
                }
            }
        }
    }


    public interface CentralScanInterface {
        void found(BluetoothDevice device);
    }
    
}
