package ch.ethz.dymand.Setup;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.dymand.FGService;
import ch.ethz.dymand.R;

public class SetupCompleteActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_complete);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
        startService();
    }

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

}
