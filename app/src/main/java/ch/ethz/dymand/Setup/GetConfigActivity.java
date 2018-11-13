package ch.ethz.dymand.Setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ch.ethz.dymand.Config;
import ch.ethz.dymand.R;

public class GetConfigActivity extends WearableActivity {

    private static final String LOG_TAG = "Logs: GettingConfigurationActivity";
    private TextView mTextView;
    private Button mButton;
    private Handler mHandler;
    private int waitTimeInSec = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_configuration);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onBackPressed() {
        // do not go to previous activity.
    }

    public void receiveButtonPressed(View view) {
        mButton = (Button) findViewById(R.id.receive_button);
        mButton.setEnabled(false);
        if(Config.configReceived){
            configurationReceivedActivity();
        }
        else{
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "Dymand::TimedWakeLockGetConfig");
            wakeLock.acquire();
            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(Config.configReceived)
                    {
                        configurationReceivedActivity();
                    }
                    else{
                        mButton.setEnabled(true);
                    }
                    wakeLock.release();
                }
            }, waitTimeInSec * 1000);
        }
    }

    public void configurationReceivedActivity(){
        mButton = (Button) findViewById(R.id.receive_button);
        mButton.setEnabled(true);
        Log.i(LOG_TAG, "Configuration received activity starting...");
        Intent intent = new Intent(this, ConfigReceivedActivity.class);
        startActivity(intent);
    }
}
