package ch.ethz.dymand;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static ch.ethz.dymand.Config.CHARACTERISTIC_STRING_BUFF;
import static ch.ethz.dymand.Config.SERVICE_STRING_BUFF;


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
                mService = new Intent(SetupUUID.this, FGService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mService);
                }
                else{

                   startService(mService);
                }

                Toast.makeText(SetupUUID.this, "Starting a  service: " + id, Toast.LENGTH_SHORT).show();
                createUUIDStrings(id);
                Intent intent = new Intent(SetupUUID.this, MainActivity.class);
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
        CHARACTERISTIC_STRING_BUFF = CHARACTERISTIC_STRING_BUFF.delete(len-enteredCodeLen,len).append(str);

        Log.d("Service String", ": "+SERVICE_STRING_BUFF);
        Log.d("Characteristic String", ": "+CHARACTERISTIC_STRING_BUFF);

    }




}
