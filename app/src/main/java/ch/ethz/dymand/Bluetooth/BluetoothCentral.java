package ch.ethz.dymand.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;

import ch.ethz.dymand.Config;
import ch.ethz.dymand.VoiceActivityDetection.VADMain;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ch.ethz.dymand.Config.CHARACTERISTIC_STRING;
import static ch.ethz.dymand.Config.CHARACTERISTIC_UUID;
import static ch.ethz.dymand.Config.SERVICE_STRING;
import static ch.ethz.dymand.Config.SERVICE_UUID;

public class BluetoothCentral {
    private static final String LOG_TAG = "Logs: Bluetooth Central";
    private final Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = false;
    private Map<String, Pair<BluetoothDevice, Integer>> mScanResults;
    private BtleScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private boolean mConnected = false;
    private BluetoothGatt mGatt;
    private boolean mInitialized = false;
    private boolean mPeriReadMessage = false;
    private boolean mCentralMessageSent = false;
    private boolean mDeviceFoundConnectionInterrupted = false;
    private BluetoothManager mBluetoothManager;
    private String mTimeStamp = "";

    public BluetoothCentral(Context context) {
        mContext = context;
    }


    // mScanning checks whether scanning is already running or not.
    public void scan() {
        if(!mScanning){
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            mScanResults = new HashMap<>();

            mScanCallback = new BtleScanCallback(mScanResults);
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
            mDeviceFoundConnectionInterrupted = false;
        }
    }

    public void stop() {
        stopScan(); // Stop scanning if it is already not stopped by the device discovery.
        disconnectGattServer(); // disconnect if connected
    }

    public String getTimeStamp() {
        return mTimeStamp;
    }


    private synchronized void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            Log.i(LOG_TAG, "Bluetooth stopped Scanning");
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        mBluetoothManager = null;
        mScanCallback = null;
        mScanning = false;
        mHandler = null;
        mScanResults = null;
        mCentralMessageSent = false;
    }

    // TODO: probably not required.
    public int checkDistance() {
        int distance = 200;
        if (!mScanResults.isEmpty()) {
            for (String deviceAddress : mScanResults.keySet()) {
                Pair<BluetoothDevice, Integer> pair = mScanResults.get(deviceAddress);
                BluetoothDevice device = pair.first;
                distance = pair.second;
                Log.i("Logs", "Found required device: " + deviceAddress + " with name: " +
                        device.getName() + " at distance: " + distance);
            }
        }
        else{
            //Log.i("Logs", "The required device could not be found currently.");
        }
        return distance;
    }

    public boolean sendSignalToPeriToRecord(String timeStamp) {
        //return !mScanResults.isEmpty();


        if(mConnected){ //connected
            if(!mCentralMessageSent){ // connected but message not sent.
                if(VADMain.isVoice()) mCentralMessageSent = sendMessage(timeStamp);
            }
            else if(mPeriReadMessage) { // connected and message sent and message read.
                mCentralMessageSent = false; // may not be required, TODO check
                disconnectGattServer(); // this will be called again... by disableBluetoothPursuit
                return true;
            }
        }
        return false;
    }

    public boolean isDeviceFoundConnectionInterrupted() {
        return mDeviceFoundConnectionInterrupted;
    }

    private class BtleScanCallback extends ScanCallback {
        private Map<String, Pair<BluetoothDevice, Integer>> mScanResults;

        public BtleScanCallback(Map<String, Pair<BluetoothDevice, Integer>> scanResults) {
            mScanResults = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Logs", "BLE Scan Failed with code " + errorCode);
        }

        // TODO: need to experiment the distance and check whether the scan continues when the device is not added.
        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            int distance = result.getRssi();
            if(distance >= Config.threshold){
                Log.i(LOG_TAG, "Distance between devices is " +  distance + " and is NOT ENOUGH.");
            }
            else {
                Log.i(LOG_TAG, "Distance between devices is " +  distance + " and is ENOUGH.");
                String deviceAddress = device.getAddress();
                mScanResults.put(deviceAddress, new Pair<>(device, distance));
                stopScan();
                mCentralMessageSent = false; //message sent check is for all times when we connect to the device
                connectDevice(device);
            }
        }
    }

    // Connect and send just one message, follow field mCentralMessageSent
    private void connectDevice(BluetoothDevice device) {
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(mContext, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private class GattClientCallback extends BluetoothGattCallback {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 1");
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 2 :" + status);
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                gatt.discoverServices();
                Log.i(LOG_TAG, "Connected and discovering services.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 3");
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 4");
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = gatt.getServices();
                Log.i(LOG_TAG, "onServicesDiscovered Services count: "+gattServices.size());

                for (BluetoothGattService gattService : gattServices) {
                    String serviceUUID = gattService.getUuid().toString();
                    Log.i(LOG_TAG, "onServicesDiscovered Service uuid "+serviceUUID);
                }
            }

            Log.i(LOG_TAG, "Preparing to send the message");

            List<BluetoothGattService> serviceList = gatt.getServices();
            BluetoothGattService service = findService(serviceList);

            if (service == null) {
                Log.i(LOG_TAG, "Service is null");
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
                return;
            }

            List<BluetoothGattCharacteristic> matchingCharacteristics = findCharacteristics(gatt);
            if (matchingCharacteristics.isEmpty()) {
                Log.i(LOG_TAG,"Unable to find characteristics.");
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
                return;
            }

            Log.i(LOG_TAG, "Initializing: setting write type and enabling notification");
            for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                enableCharacteristicNotification(gatt, characteristic);
            }


           /* BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service == null) {
                Log.i(LOG_TAG, "Service is null");
            }

            Log.i(LOG_TAG, "The value of mInitialized: " + mInitialized);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            if(characteristic == null){
                Log.i(LOG_TAG, "characteristic is null");
            }
            Log.i(LOG_TAG, "The value of mInitialized: " + mInitialized);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            Log.i(LOG_TAG, "The value of mInitialized: " + mInitialized);
            mInitialized = gatt.setCharacteristicNotification(characteristic, true);
            Log.i(LOG_TAG, "The value of mInitialized: " + mInitialized);*/
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Characteristic (message) written successfully");
                mPeriReadMessage = true;
            } else {
                mDeviceFoundConnectionInterrupted = true;
                disconnectGattServer();
                Log.i(LOG_TAG, "Characteristic write unsuccessful, status: " + status);
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] messageBytes = characteristic.getValue();
            String messageString = null;
            try {
                messageString = new String(messageBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.i(LOG_TAG, "Unable to convert message bytes to string");
            }
            Log.i(LOG_TAG, "Received message: " + messageString);
        }

    }

    private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mInitialized = gatt.setCharacteristicNotification(characteristic, true);
        if (mInitialized) {
            Log.i(LOG_TAG, "Characteristic notification set successfully for " + characteristic.getUuid().toString());
        } else {
            Log.i(LOG_TAG, "Characteristic notification set failure for " + characteristic.getUuid().toString());
            mDeviceFoundConnectionInterrupted = true;
            disconnectGattServer();
        }
    }


    private List<BluetoothGattCharacteristic> findCharacteristics(BluetoothGatt bluetoothGatt) {
        List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();

        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
        BluetoothGattService service = findService(serviceList);
        if (service == null) {
            return matchingCharacteristics;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            if (isMatchingCharacteristic(characteristic)) {
                matchingCharacteristics.add(characteristic);
            }
        }

        return matchingCharacteristics;
    }

    private static boolean isMatchingCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return matchesCharacteristicUuidString(uuid.toString());
    }

    private static boolean matchesCharacteristicUuidString(String characteristicIdString) {
        return uuidMatches(characteristicIdString, CHARACTERISTIC_STRING);
    }


    private BluetoothGattService findService(List<BluetoothGattService> serviceList) {
        for (BluetoothGattService service : serviceList) {
            String serviceIdString = service.getUuid()
                    .toString();
            if (matchesServiceUuidString(serviceIdString)) {
                return service;
            }
        }
        return null;
    }

    private static boolean matchesServiceUuidString(String serviceIdString) {
        return uuidMatches(serviceIdString, SERVICE_STRING);
    }

    private static boolean uuidMatches(String uuidString, String... matches) {
        for (String match : matches) {
            if (uuidString.equalsIgnoreCase(match)) {
                return true;
            }
        }

        return false;
    }

    public void disconnectGattServer() {
        mConnected = false;
        mInitialized = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    private boolean sendMessage(String message) {
        //Log.i(LOG_TAG, "Sending message...");

        if (!mConnected) throw new AssertionError();

        if (!mInitialized) {
            return false;
        }

        Log.i(LOG_TAG, "Sending message:"+ message);
        mTimeStamp = message;
        mPeriReadMessage = false;
        BluetoothGattService service = mGatt.getService(SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        byte[] messageBytes = new byte[0];
        try {
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.i(LOG_TAG, "Failed to convert message string to byte array");
        }
        characteristic.setValue(messageBytes);
        boolean success = mGatt.writeCharacteristic(characteristic);
        Log.i(LOG_TAG, "Sending message initiated...");
        return success;
    }

}
