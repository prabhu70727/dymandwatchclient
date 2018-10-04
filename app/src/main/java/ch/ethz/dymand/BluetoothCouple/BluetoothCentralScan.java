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

import ch.ethz.dymand.Bluetooth.BluetoothCentral;
import ch.ethz.dymand.Config;

import static ch.ethz.dymand.Config.SERVICE_UUID;

public class BluetoothCentralScan {
    private static final String LOG_TAG = "Logs: Bluetooth Central Scan";

    private boolean mScanning = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final Context mContext;
    private Map<String, Pair<BluetoothDevice, Integer>> mScanResults;
    private BleScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private File bleSSFile = null;
    private FileOutputStream bleSSFileStream;
    private CentralScanInterface mCentralScanListerner;

    public BluetoothCentralScan(Context context, CentralScanInterface centralScanListerner) {
        mContext = context;
        mCentralScanListerner = centralScanListerner;
    }


    public void startScan() throws IOException {
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
            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
            mScanning = true;
            bleSSFile = new File("", "Bluetooth_signal_strength_log");
            if(!bleSSFile.exists()){
                bleSSFile.createNewFile();
            }
            bleSSFileStream = new FileOutputStream(bleSSFile);
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
        bleSSFile = null;
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
            Log.i("Logs", "BLE Scan Failed with code " + errorCode);
        }

        //TODO : Synchronized? test it..
        synchronized private void addScanResult(ScanResult result) throws IOException {
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();

            Log.i(LOG_TAG, "Signal strength is " +  rssi);
            String toWrite = System.currentTimeMillis()+","+rssi+"\n";
            //bleSSFileStream.write(toWrite.getBytes());

            if(Config.shouldConnect){
                if(rssi >= Config.threshold) {
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
