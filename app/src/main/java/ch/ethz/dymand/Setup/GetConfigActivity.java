package ch.ethz.dymand.Setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import ch.ethz.dymand.Config;
import ch.ethz.dymand.R;
import ch.ethz.dymand.WatchPhoneCommunication;

public class GetConfigActivity extends WearableActivity implements DataClient.OnDataChangedListener{

    private static final String LOG_TAG = "Logs: GettingConfigurationActivity";
    private TextView mTextView;
    private Button mButton;
    private Handler mHandler;
    private int waitTimeInSec = 1;

    // get config signal
    private static final String GET_CONFIG_PATH = "/getconfig";
    private static final String GET_CONFIG_KEY = "dymand.get.config.key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_configuration);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(LOG_TAG, "Success while adding listener");
            }
        });
    }

//    @Override
//    public void onBackPressed() {
//        // do not go to previous activity.
//    }

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

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.i(LOG_TAG, "onDataChanged called in watch.");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                Log.i(LOG_TAG, "onDataChanged data event - changed.");
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(GET_CONFIG_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    setConfig(dataMap.getString(GET_CONFIG_KEY));
                    Wearable.getDataClient(this).deleteDataItems(item.getUri());
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.i(LOG_TAG, "onDataChanged data event - deleted.");
                // DataItem deleted
            }
        }

    }

    private void setConfig(String configuration) {
        Log.i(LOG_TAG, "Configuration is " + configuration);
        String [] tokens = configuration.split(" ");
        String [] configTokens = tokens[1].split("-");
        Config.morningStartHourWeekday = Integer.parseInt(configTokens[0]);
        Config.morningEndHourWeekday = Integer.parseInt(configTokens[1]);
        Config.eveningStartHourWeekday = Integer.parseInt(configTokens[2]);
        Config.eveningEndHourWeekday = Integer.parseInt(configTokens[3]);
        Config.startHourWeekend = Integer.parseInt(configTokens[4]);
        Config.endHourWeekend = Integer.parseInt(configTokens[5]);
        Config.configReceived = true;
    }


    @Override
    protected void onPause(){
        super.onPause();
        Wearable.getDataClient(this).removeListener(this).addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                Log.i(LOG_TAG, "Success while removing listener");
            }
        });
    }
}
