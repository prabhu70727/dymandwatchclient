package ch.ethz.dymand.Setup;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.dymand.FGService;
import ch.ethz.dymand.R;

import static ch.ethz.dymand.Config.SERVICE_STRING_BUFF;
import static ch.ethz.dymand.Config.isSetupComplete;
import static ch.ethz.dymand.Config.saveAppInfo;
import static ch.ethz.dymand.Config.subjectID;

public class SetupCompleteActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_complete);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
        isSetupComplete = true;

        //Save that setup is complete
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isSetupComplete", isSetupComplete);
        editor.apply();

        //TODO: move to GetConfigActivity after Prabhu pushes his changes
        //Save hours of data collection
        //saveAppInfo(this);

        //Start service
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
