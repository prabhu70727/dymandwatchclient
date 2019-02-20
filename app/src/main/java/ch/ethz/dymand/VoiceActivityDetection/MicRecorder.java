package ch.ethz.dymand.VoiceActivityDetection;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.LinkedList;

public class MicRecorder extends Thread {
    private static final String TAG = "VoiceService";
    private  int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    private  int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private static final int NO_OF_SECONDS = 5; //5 seconds
    private static final int FREQUENCY = 8000; //8kHz
    private  static MicRecorder instance = null;
    private boolean isRecording = false;
    private short[] buffer;
    int bufferSize = FREQUENCY * NO_OF_SECONDS;
    AudioRecord audioRecord = null;
    private MicRecorder.MicrophoneListener listener = null;
    private LinkedList<MicrophoneListener> listeners = new LinkedList<MicrophoneListener>();

    public static MicRecorder getInstance() {
        if (instance == null) {
            instance = new MicRecorder();
        }
        return instance;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void registerListener(MicRecorder.MicrophoneListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(MicRecorder.MicrophoneListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public short[] getBuffer() {
        return buffer;
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            instance = null;
        }
    }

    public void startRecording() {
        if (!isRecording) {
            isRecording = true;
            // Create a new AudioRecord object to record the audio.

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY,
                    channelConfiguration, audioEncoding, bufferSize);
            buffer = new short[bufferSize];

            instance.start();
            Log.d(TAG, "VoiceService:MR: audioRecord.startRecording()");

        }
    }

    public void run() {
        try {
            int offset = 0;
            int bufferReadResult = 0;

            try {
                audioRecord.startRecording();
            }catch (Exception e){
                Log.i(TAG, "Error when starting recording: " + e.toString());
            }

            Log.d("Thread", "Running " + isRecording);

            while (isRecording) {
                try{
                    bufferReadResult += audioRecord.read(buffer, offset, bufferSize - bufferReadResult);
                }catch (Exception e){
                    Log.i(TAG, "Error when reading audio buffer: " + e.toString());
                }
                                offset += bufferReadResult;
                if (bufferReadResult == bufferSize) {
                    Log.d("Total Samples Collected", ": " + bufferReadResult);
                    for (MicRecorder.MicrophoneListener listener : listeners) {
                        listener.microphoneBuffer(buffer, bufferReadResult);
                    }
                    offset = 0;
                    bufferReadResult = 0;
                    audioRecord.stop();
                    isRecording = false;
                }
                // maybe sleep to save battery
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "VoiceService:MR: Recording Failed");
        }
    }

    private final float mRecordingGain = 10;
    private void adjustGain(short[] audioData) {
        for(int i = 0; i < audioData.length; i++){
            audioData[i] = (short) Math.min((int)(audioData[i] * mRecordingGain), (int) Short.MAX_VALUE);
        }
    }

    public static interface MicrophoneListener {
        public void microphoneBuffer(short[] buffer, int window_size);
    }
}

