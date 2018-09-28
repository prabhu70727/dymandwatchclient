package OldCode;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import OldCode.SetupActivity_Old;
import ch.ethz.dymand.FGService;
import ch.ethz.dymand.R;
import ch.ethz.dymand.Setup.BlutoothSetupActivity;
import ch.ethz.dymand.VoiceActivityDetection.VAD;


public class MainActivity_Old extends WearableActivity {
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
    private VAD voiceDetector;


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

        //TODO: remove
        startService();

        //TODO: remove
        //VAD example
//        voiceDetector = new VAD(MainActivity.this);
//        voiceDetector.recordSound();



//        Intent intent = new Intent(this, MyWakefulReceiver.class);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        //scheduleAlarm();
    }

    //TODO: Move to terminal activity in the set up process
    private void startService(){
        if(isMyServiceRunning(FGService.class)) {
            Toast.makeText(this, "Service exists. Kill it before starting a new one...", Toast.LENGTH_SHORT).show();
            return;
        }

        FGService.acquireStaticLock(this);
        Intent mService = new Intent(this, FGService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mService);
        }
        else{

            startService(mService);
        }

        Toast.makeText(this, "Starting service: ", Toast.LENGTH_SHORT).show();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
        Intent intent = new Intent(this, SetupActivity_Old.class);
        startActivity(intent);
    }

    public void startBluetoothSetupActivityOnClicked(View view) {
        Log.i(LOG_TAG, "Bluetooth Setup Intent starting...");
        Intent intent = new Intent(this, BlutoothSetupActivity.class);
        startActivity(intent);
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.i(LOG_TAG, "Requested user enables Bluetooth.");
    }

}
