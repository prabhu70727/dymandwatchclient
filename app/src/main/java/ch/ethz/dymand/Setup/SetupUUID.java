package ch.ethz.dymand.Setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.dymand.FGService;
import ch.ethz.dymand.R;

import static ch.ethz.dymand.Config.CHARACTERISTIC_STRING_BUFF;
import static ch.ethz.dymand.Config.SERVICE_STRING;
import static ch.ethz.dymand.Config.SERVICE_STRING_BUFF;
import static ch.ethz.dymand.Config.isCentral;
import static ch.ethz.dymand.Config.subjectID;
import static ch.ethz.dymand.Config.updateUUID;


public class SetupUUID extends WearableActivity {

    private TextView mTextView;
    Button btn;
    Intent mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_uuid);

        final TextInputLayout idWrapper = (TextInputLayout) findViewById(R.id.idWrapper);
        btn = (Button) findViewById(R.id.btn);
        // Enables Always-on
        setAmbientEnabled();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = idWrapper.getEditText().getText().toString();

                subjectID = subjectID + id;

                Toast.makeText(SetupUUID.this, "Entered ID: " + id, Toast.LENGTH_SHORT).show();
                createUUIDStrings(id);
                Intent intent = new Intent(SetupUUID.this, GetConfigActivity.class);
                startActivity(intent);
            }
        });
    }

    private void createUUIDStrings(String str){
        //Associate UUID to entered number by appending number to already declared UUID
        int enteredCodeLen = str.length();
        int len = SERVICE_STRING_BUFF.length();

        //Create new UUID strings
        SERVICE_STRING_BUFF = SERVICE_STRING_BUFF.delete(len-enteredCodeLen,len).append(str);
        //CHARACTERISTIC_STRING_BUFF = CHARACTERISTIC_STRING_BUFF.delete(len-enteredCodeLen,len).append(str);

        updateUUID();

        //Save the subject ID
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("subjectID", subjectID);
        editor.putString("SERVICE_STRING", SERVICE_STRING);
        editor.apply();

        Log.d("Service String", ": "+SERVICE_STRING_BUFF);

    }




}
