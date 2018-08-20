package ch.ethz.dymand;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;


public class MainActivity extends WearableActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    private static String LOG_TAG="Logs: MainActivity";
    private static final int REQUEST_ALL_PERMISSION = 200;
    private boolean permissionToAllAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BODY_SENSORS, Manifest.permission.BLUETOOTH};
    Button mButton;
    private BluetoothAdapter mBluetoothAdapter; // to check the capabilities of BLE
    private BluetoothManager mbluetoothManager;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Log.i(LOG_TAG, "Length of grant Results:" + grantResults.length);
        switch (requestCode){
            case REQUEST_ALL_PERMISSION:
                if(grantResults.length > 0)
                    permissionToAllAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToAllAccepted ) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Device does not support BLE.");
            finish();
        }

        mbluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mbluetoothManager.getAdapter();

        setAmbientEnabled();

        mButton = findViewById(R.id.button2);
        mButton.setEnabled(false);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSION); //Non blocking return
        boolean check = true;
        while(check) {
            //Log.i(LOG_TAG, "check");
            boolean allPermissionsAccepted = true;
            for(String permission: permissions){
                if(!(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)){
                    allPermissionsAccepted = false;
                }
            }
            if(allPermissionsAccepted){
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
                    requestBluetoothEnable();
                check = false;
                mButton.setEnabled(true);
            }
        }


        /*SensorManager mSensorManager;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor:deviceSensors){
            Log.i(LOG_TAG, sensor.getStringType()+" "+sensor.getName());
        }*/

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startSetupActivityOnClicked(View view) {
        Log.i(LOG_TAG, "Setup Intent starting...");
        Intent intent = new Intent(this, SetupActivity.class);
        startActivity(intent);
    }

    /* Use it if necessary.
    private boolean blueToothHasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions()) {
            requestLocationPermission();
            return false;
        }
        return true;
    }*/

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.i(LOG_TAG, "Requested user enables Bluetooth.");
    }

    /*
    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
    }*/

}
