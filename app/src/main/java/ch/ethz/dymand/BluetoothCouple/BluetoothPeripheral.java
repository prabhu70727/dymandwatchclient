// Reference BLE tutorial: https://www.bignerdranch.com/blog/bluetooth-low-energy-on-android-part-2/

package ch.ethz.dymand.BluetoothCouple;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static ch.ethz.dymand.Config.CHARACTERISTIC_UUID;
import static ch.ethz.dymand.Config.SERVICE_UUID;

public class BluetoothPeripheral {
    private static final String LOG_TAG = "Logs: BluetoothPeripheral";
    private Context mContext;
    private boolean mAdvertising = false;
    private List<BluetoothDevice> mDevices;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;
    private String mTimeStamp = "0";
    private AdvertiseCallback mAdvertiseCallback = null;
    private PeripheralInterface mPeripheralListener;


    public BluetoothPeripheral(Context context, PeripheralInterface peripheralListener) {
        mContext = context;
        mPeripheralListener = peripheralListener;
    }

    public void startAdvertising() {
        if(mAdvertising) {
            Log.i(LOG_TAG, "Assertion error");
            throw new AssertionError();
        }
        else{
            mDevices = new ArrayList<>();
            mBluetoothManager = (BluetoothManager)
                    mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) throw new AssertionError();
            if (!mBluetoothAdapter.isEnabled()) throw new AssertionError();

            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            GattServerCallback gattServerCallback = new GattServerCallback();
            mGattServer = mBluetoothManager.openGattServer(mContext, gattServerCallback);
            setupServer();
            advertise();
            mAdvertising = true;
            mTimeStamp = "0";
        }
    }

    public void stopAdvertising() {
        if (!mAdvertising) throw new AssertionError();
        if (mGattServer != null) {
            mGattServer.close();
            Log.i(LOG_TAG, "Gatt Server is closed");
        }
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            Log.i(LOG_TAG, "Bluetooth Advertiser is stopped");
        }
        mAdvertising = false;
        mDevices = null;
    }


    private void advertise() {
        if (mBluetoothLeAdvertiser == null) {
            Log.i(LOG_TAG, "mBluetoothLeAdvertiser is null");
            return;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();
        ParcelUuid parcelUuid = new ParcelUuid(SERVICE_UUID);
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .build();

        mAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(LOG_TAG, "Peripheral advertising started.");
            }

            @Override
            public void onStartFailure(int errorCode) {
                //AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED
                Log.i(LOG_TAG, "Peripheral advertising failed: " + errorCode);
            }
        };

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void setupServer() {
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(writeCharacteristic);

        if(writeCharacteristic == null){
            Log.i(LOG_TAG, "The writeCharacteristic is null.");
        }

        mGattServer.addService(service);
        Log.i(LOG_TAG, "The server is set up.");
    }

    private class GattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mDevices.add(device);
                Log.i(LOG_TAG, "The device is added.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mDevices.remove(device);
                Log.i(LOG_TAG, "The device is removed.");
            }
        }


        public void onServiceAdded(int status, BluetoothGattService service) {
            if(BluetoothGatt.GATT_SUCCESS == status){
                Log.i(LOG_TAG, "Service successfully added");
                String serviceUUID = service.getUuid().toString();
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    Log.i(LOG_TAG, "onServiceAdded characteristic uuid "+
                            characteristic.getUuid().toString());
                }
                Log.i(LOG_TAG, "onServiceAdded Service uuid "+serviceUUID);
            }
            else{
                Log.i(LOG_TAG, "Service not successfully added and Status:"+ status);
            }
        }

        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic,
                    preparedWrite, responseNeeded, offset, value);

            Log.i(LOG_TAG, "in onCharacteristicWriteRequest");

            if (characteristic.getUuid().equals(CHARACTERISTIC_UUID)) {
                mGattServer.sendResponse(device, requestId,
                        BluetoothGatt.GATT_SUCCESS, 0, null);
                Log.i(LOG_TAG, "Respond with GATT success");
            }

            String messageString = null;

            try {
                messageString = new String(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.i(LOG_TAG, "Unable to convert message bytes to string");
            }

            mTimeStamp = messageString;
            Log.i(LOG_TAG, "Received message:" + mTimeStamp);

            //connected with timestamp
            Log.i(LOG_TAG, "Call back to connected");
            mPeripheralListener.connected(mTimeStamp);

            characteristic.setValue(value);
            for (BluetoothDevice dev : mDevices) {
                mGattServer.notifyCharacteristicChanged(dev, characteristic, false);
            }
        }

    }

    public interface PeripheralInterface{
        void connected(String timestamp);
    }

}
