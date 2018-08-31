package ch.ethz.dymand;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import ch.ethz.dymand.Bluetooth.BluetoothCentral;


public class BlutoothSetupActivity extends WearableActivity {

    private static final String LOG_TAG = "Logs: SetupActivity";
    Intent mService = null;
    BluetoothCentral mbluetoothCentral;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_setup);
        //getServiceNames();
        // Enables Always-on

        mbluetoothCentral = new BluetoothCentral(this);
        mbluetoothCentral.scan();

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable(){
            public void run() {
                mbluetoothCentral.stop();
            }
        },2000);

        setAmbientEnabled();
    }

    private int getFGServiceCount() {
        int count = 0;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.foreground){
                Log.i(LOG_TAG, service.service.getClassName());
                count++;
            }
        }
        return count;
    }

    private void getServiceNames() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
             Log.i(LOG_TAG, service.service.getClassName());
        }
    }


    public void startServiceOnClickedCentral(View view) {
        mbluetoothCentral.stop();
        Log.i(LOG_TAG, "Foreground services count: " + getFGServiceCount());
        Log.i(LOG_TAG, "Checking whether the required service exists..." );
        if(isMyServiceRunning(FGService.class)) {
            Toast.makeText(this, "Service exists. Kill it before starting a new one...", Toast.LENGTH_SHORT).show();
            return;
        }

//        mService = new Intent(BlutoothSetupActivity.this, FGService.class);
        Log.i(LOG_TAG, "Starting a central service" );
        Toast.makeText(this, "Starting a central service", Toast.LENGTH_SHORT).show();

        Config.isCentral = true;


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(mService);
//        }
//        else{
//            startService(mService);
//        }

        Intent intent = new Intent(this, SetupUUID.class);
        startActivity(intent);
    }


    public void killServiceOnClicked(View view) {
        mbluetoothCentral.stop();
        Log.i(LOG_TAG, "Foreground services count: " + getFGServiceCount());
        Log.i(LOG_TAG, "Checking whether the required service exists..." );
        if(!isMyServiceRunning(FGService.class)) {
            Toast.makeText(this, "Service does not exist. Start it before trying to kill...", Toast.LENGTH_SHORT).show();
            return;
        }

        stopService(mService);
    }

    public void startServiceOnClickedPeripheral(View view) {
        mbluetoothCentral.stop();
        Log.i(LOG_TAG, "Foreground services count: " + getFGServiceCount());
        Log.i(LOG_TAG, "Checking whether the required service exists..." );
        if(isMyServiceRunning(FGService.class)) {
            Toast.makeText(this, "Service exists. Kill it before starting a new one...", Toast.LENGTH_SHORT).show();
            return;
        }
//        mService = new Intent(BlutoothSetupActivity.this, FGService.class);
        Log.i(LOG_TAG, "Starting a peripheral service" );
        Toast.makeText(this, "Starting a peripheral service", Toast.LENGTH_SHORT).show();

        Config.isCentral = false;

        //Bundle b = new Bundle();
        //b.putParcelable("data",mbluetoothManager );
        //service.putExtra(mbluetoothManager);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(mService);
//        }
//        else{
//            startService(mService);
//        }

        Intent intent = new Intent(this, SetupUUID.class);
        startActivity(intent);

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


}
