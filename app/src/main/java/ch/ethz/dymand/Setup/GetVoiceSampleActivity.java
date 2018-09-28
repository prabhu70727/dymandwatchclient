package ch.ethz.dymand.Setup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import ch.ethz.dymand.Audio.BackgroundAudioRecorder;
import ch.ethz.dymand.R;

import static ch.ethz.dymand.Config.DEBUG_MODE;

public class GetVoiceSampleActivity extends WearableActivity {

    private Button recordButton;
    private boolean isRecording = false;
    private boolean doneRecording = false;
    private String TAG = "GetVoiceSampleActivity";
    private BackgroundAudioRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_voice_sample);

        // Enables Always-on
        setAmbientEnabled();

        recordButton = findViewById(R.id.recordBtn);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder = new BackgroundAudioRecorder(GetVoiceSampleActivity.this);

                if (doneRecording){
                    completeRecording();
                }else {

                    if (isRecording) {
                        //recordButton.setEnabled(false);
                        isRecording = false;
                        stopRecording();
                        doneRecording = true;
                        recordButton.setText("Done");
                        scheduleTransition();

                    } else {

                        isRecording = true;
                        recordButton.setText(R.string.stop_msg);
                        try {
                            startRecording();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
    }


    public void startRecording() throws FileNotFoundException {
        Log.d(TAG, "starting recording");
        long timeStamp = System.currentTimeMillis();
        File recordDir = new File(getApplicationContext().getFilesDir().getAbsolutePath()+"/"+TAG+timeStamp);
        recordDir.mkdirs();
        String dirPath = getApplicationContext().getFilesDir().getAbsolutePath()+"/"+TAG+timeStamp+"/";
        recorder.startRecording(dirPath);
    }

    public void stopRecording(){
        Log.d(TAG, "stopping recording");
        recorder.stopRecording();
    }

    public void completeRecording(){
        Intent intent = new Intent(this, SetupCompleteActivity.class);
        startActivity(intent);
    }

    public void scheduleTransition(){
        //Create timer using handler and runnable
        final Handler timerHandler = new Handler();

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                completeRecording();
            }
        };

        timerHandler.postDelayed(timerRunnable, 3000);
    }
}
