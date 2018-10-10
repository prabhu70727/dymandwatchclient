package ch.ethz.dymand.Setup;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import ch.ethz.dymand.R;

public class GetConfigActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_config);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();

        getConfig();
    }

    //TODO: Add code for configuration
    private void getConfig(){

        configReceived();
    }

    //Starts next activity after receiving configuration
    private void configReceived(){
        Intent intent = new Intent(this, GetVoiceSampleActivity.class);
        startActivity(intent);
    }
}
