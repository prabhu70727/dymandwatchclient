package ch.ethz.dymand.Setup;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import ch.ethz.dymand.R;

import static ch.ethz.dymand.Config.saveAppInfo;

public class ConfigReceivedActivity extends WearableActivity {

    private TextView mTextView;
    private String LOG_TAG = "Logs: ConfigReceivedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_received);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();

        //Save hours of data collection
        saveAppInfo(this);
    }

//    @Override
//    public void onBackPressed() {
//        Log.i(LOG_TAG, "on Back pressed");
//        // do not go to previous activity.
//    }

    public void onNextButtonPressed(View view){
        Intent intent = new Intent(this, GetVoiceSampleActivity.class);
        startActivity(intent);
        //finish();
    }

}
