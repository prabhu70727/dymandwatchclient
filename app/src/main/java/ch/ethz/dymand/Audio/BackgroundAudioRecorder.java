package ch.ethz.dymand.Audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import ch.ethz.dymand.Config;

import java.io.FileNotFoundException;
import java.io.IOException;

public class BackgroundAudioRecorder {

    private static final String LOG_TAG = "Logs: BackgroundAudioRecorder";
    private static final int RECORDER_SAMPLE_RATE = Config.RECORDER_SAMPLE_RATE;
    private static final int RECORDER_ENCODING_BIT_RATE = Config.RECORDER_ENCODING_BIT_RATE;
    private static final int RECORDER_AUDIO_CHANNELS = Config.RECORDER_AUDIO_CHANNELS;
    private String fileTag = Config.audioFileTag;
    private boolean isRecording = false;
    private MediaRecorder audioRecorder = null;
    private Context applicationContext;

    public BackgroundAudioRecorder(Context applicationContext){
        this.applicationContext = applicationContext;
    }

    @SuppressLint("LongLogTag")
    public void startRecording(String dirPath) throws FileNotFoundException {
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        String filename = dirPath+fileTag+".m4a";
        Log.i(LOG_TAG, "Recording file:" + filename);
        audioRecorder.setOutputFile(filename);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        audioRecorder.setAudioSamplingRate(RECORDER_SAMPLE_RATE);
        audioRecorder.setAudioEncodingBitRate(RECORDER_ENCODING_BIT_RATE);
        //audioRecorder.setAudioChannels(RECORDER_AUDIO_CHANNELS);

        //Log.i(LOG_TAG, "Max amplitutde till now" + audioRecorder.getMaxAmplitude());

        if (isRecording != false) throw new AssertionError();
        try {
            audioRecorder.prepare();
        } catch (IOException e) {
            Log.i(LOG_TAG, "prepare() failed");
        }
        isRecording = true;
        audioRecorder.start();
        Log.i(LOG_TAG, "Started audio Recording");
    }

    @SuppressLint("LongLogTag")
    public void stopRecording() {
        if (null != audioRecorder) {
            Log.i(LOG_TAG, "Stopped audio recording");
            isRecording = false;
            audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
        }
    }

    public void end() {
        audioRecorder = null;
    }
}
