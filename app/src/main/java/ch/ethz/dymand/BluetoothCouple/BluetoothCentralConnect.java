// Reference BLE tutorial: https://www.bignerdranch.com/blog/bluetooth-low-energy-on-android-part-2/


package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ch.ethz.dymand.Config.CHARACTERISTIC_STRING;
import static ch.ethz.dymand.Config.CHARACTERISTIC_UUID;
import static ch.ethz.dymand.Config.SERVICE_STRING;
import static ch.ethz.dymand.Config.SERVICE_UUID;

public class BluetoothCentralConnect {

    private static final String LOG_TAG = "Logs: Bluetooth Central";
    private final Context mContext;
    private BluetoothGatt mGatt;
    private boolean mInitialized = false;
    private boolean mConnected = false;
    private String mTimestamp;
    private CentralConnectInterface mCentralConnectListener;
    private BluetoothDevice mDevice;

    public BluetoothCentralConnect(Context context, CentralConnectInterface centralConnectListener) {
        mContext = context;
        mCentralConnectListener = centralConnectListener;
    }

    void connectDevice(BluetoothDevice device, String timestamp) {
        Log.i(LOG_TAG, "Trying to connect with Characteristic UUID:" + CHARACTERISTIC_UUID);
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(mContext, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE);
        mTimestamp = timestamp;
        mDevice = device;
    }

    private class GattClientCallback extends BluetoothGattCallback {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 1");
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 2 :" + status);
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                gatt.discoverServices();
                Log.i(LOG_TAG, "Connected and discovering services.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 3");
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Device found but connection Interrupted - 4");
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
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
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
                return;
            }

            List<BluetoothGattCharacteristic> matchingCharacteristics = findCharacteristics(gatt);
            if (matchingCharacteristics.isEmpty()) {
                Log.i(LOG_TAG,"Unable to find characteristics.");
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
                return;
            }

            Log.i(LOG_TAG, "Initializing: setting write type and enabling notification");
            for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                enableCharacteristicNotification(gatt, characteristic);
            }
            // sending the message in the enableCharacteristicNotification(gatt, characteristic) function
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Characteristic (message) written successfully");
                //mPeriReadMessage = true;
            } else {
                //mDeviceFoundConnectionInterrupted = true;
                Log.i(LOG_TAG, "Characteristic write unsuccessful, status: " + status);
                disconnectGattServer();
                mCentralConnectListener.notConnected(mDevice);
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
            try {
                mCentralConnectListener.connected();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mInitialized = gatt.setCharacteristicNotification(characteristic, true);
        if (mInitialized) {
            Log.i(LOG_TAG, "Characteristic notification set successfully for " + characteristic.getUuid().toString());
            sendMessage(mTimestamp);
        } else {
            Log.i(LOG_TAG, "Characteristic notification set failure for " + characteristic.getUuid().toString());
            //mDeviceFoundConnectionInterrupted = true;
            disconnectGattServer();
            mCentralConnectListener.notConnected(mDevice);
        }
    }


    private List<BluetoothGattCharacteristic> findCharacteristics(BluetoothGatt bluetoothGatt) {
        List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();

        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
        BluetoothGattService service = findService(serviceList);
        if (service == null) {
            Log.i(LOG_TAG, "findCharacteristics: service is null");
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
        Log.i(LOG_TAG, "Disconnecting gatt server");
        mConnected = false;
        mInitialized = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
        blockingLoop(500); // Waiting for 500 ms
    }

    private boolean sendMessage(String message) {
        //Log.i(LOG_TAG, "Sending message...");

        if (!mConnected) throw new AssertionError();

        if (!mInitialized) {
            return false;
        }

        Log.i(LOG_TAG, "Sending message:"+ message);
        //mPeriReadMessage = false;
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
        blockingLoop(500); // Waiting for 500 ms
        return success;
    }


    public void blockingLoop(int recordTime) {
        Log.i(LOG_TAG, "Waiting for " + recordTime + " milliseconds");
        long endLoop = curTime() + recordTime;
        while(curTime()<endLoop);
    }

    public long curTime(){
        return System.currentTimeMillis();
    }

    public interface CentralConnectInterface{
        void connected() throws FileNotFoundException;
        void notConnected(BluetoothDevice device);
    }


}
