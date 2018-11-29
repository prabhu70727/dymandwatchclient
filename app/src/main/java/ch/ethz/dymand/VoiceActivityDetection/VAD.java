package ch.ethz.dymand.VoiceActivityDetection;

import android.os.Handler;
import android.util.Log;

import static ch.ethz.dymand.Config.getDateNow;
import static ch.ethz.dymand.Config.noSilenceDates;
import static ch.ethz.dymand.Config.noSilenceNum;
import static ch.ethz.dymand.Config.vadDates;
import static ch.ethz.dymand.Config.vadNum;
import static ch.ethz.dymand.VoiceActivityDetection.ConfigVAD.*;


/**
 * This class implements a voice activity detection module. It is a 2-stage module
 * The first stage determines if there is some interesting activity in the sound signal aka nonsilence
 * If it's determined to be nonsilence, the module determines if the sound signal is speech.
 * If the sound is speech, data collection is started
 */
public class VAD implements MicRecorder.MicrophoneListener {
    private boolean startDataCollection = false;
    private static MicRecorder recorder;
    private DataCollectionListener listener;
    private Handler handler = null;

    public VAD(DataCollectionListener listener){
        //Log VAD Parameters
        Log.d("VAD Parameters", "");
        Log.d("FRAME_SIZE_MS", ": "+FRAME_SIZE_MS);
        Log.d("NO_OF_SECONDS", ": "+ NO_OF_SECONDS);
        Log.d("FREQUENCY", ": "+FREQUENCY);
        Log.d("SAMPLES_PER_FRAME", ": "+SAMPLES_PER_FRAME);
        Log.d("WINDOW_SIZE", ": "+WINDOW_SIZE);
        Log.d("VOICE_THRESHOLD", ": "+VOICE_THRESHOLD);
        Log.d("ENERGY_THRESHOLD", ": "+ENERGY_THRESHOLD);
        this.listener = listener;

    }

    /**
     * Returns true if data collection should be started, else false
     * @return startDataCollection
     */
    public boolean shouldStartDataCollection() {
        return startDataCollection;
    }

    /**
     * Sets startDataCollection to true if data collection should be started
     * @param startDataCollection
     */
    private void setStartDataCollection(boolean startDataCollection) {
        this.startDataCollection = startDataCollection;
    }


    /*
    public void shouldCollectData(){
        //recorder.registerListener(VAD.class);
        handler = new Handler();
        //boolean shouldCollectData = false;

        recordSound();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                boolean shouldCollectData = false;

                short[] buffer = recorder.getBuffer();
                finishRecording();
                //Determine if is silence
                if (!isSilence(buffer)){
                    shouldCollectData = isSpeech(buffer);
                    setStartDataCollection(shouldCollectData);
                }

                Log.d("Collect Data:", ""+shouldCollectData);

            }
        };

        handler.postDelayed(r, NO_OF_SECONDS*1000);
    }
    */

    /**
     * Records sound data into buffer
     */
    public void recordSound(){
        recorder = MicRecorder.getInstance();

        recorder.registerListener(VAD.this); //register this class listener for the microphone recorder
        recorder.startRecording();
        Log.d("Audio", "Started recording");
    }

    /**
     *  Stop recording sound data
     */
    private void finishRecording(){
        recorder.stopRecording(); //stop recording
        recorder.unregisterListener(VAD.this);
        Log.d("Audio", "Done recording");

    }

    /**
     *  Checks and returns if sound sample is silence
     * @param buffer
     * @return isSilence
     */
    public static boolean isSilence(short[] buffer){
        boolean isSilence = true;


        //Calculate energy
        double energy = calculateEnergy(buffer);

        //Check if above threshold
        if (energy > ENERGY_THRESHOLD){
            isSilence = false;
        }

        Log.d("Silence:", ""+isSilence);

        //tesing.. TODO: remove
        return false;
        //return isSilence;
    }


    /**
     * Checks and returns if sound sample is speech
     * @param buffer
     * @return isSpeech
     */
    private static boolean isSpeech(short[] buffer){
        boolean isSpeech = false;
        int classification = 0;
        int voiced = 0; //counts number of speech classifications in window

        //Extract features and classify for each frame
        for(int k=0;k<WINDOW_SIZE;k+=SAMPLES_PER_FRAME){
            double[] features = FeatureExtractor.ComputeFeaturesForFrame(buffer,SAMPLES_PER_FRAME,k);
            try {

                //Classify sample
                if (SpeechDetector.Classify(features)== true){
                    voiced++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Check if number of samples classified as voiced is greater than threshold
        if (voiced > VOICE_THRESHOLD){
            classification = 1;
        }
        Log.d("Voice Count", "" + voiced);

        if (classification == 1){
            Log.d("Classify","Speaking");
        }else{
            Log.d("Classify", "Noise");
        }

        isSpeech = classification == 1 ? true : false;

        //Testing... TODO: remove
        return true;

        //return isSpeech;
    }

    /**
     * Estimate the energy and RMS of the sound sample
     * @param buffer
     * @return
     */
    public static double calculateEnergy(short[] buffer) {

        double min = 1;
        double minRaw = Short.MAX_VALUE;
        double max = -1;
        double meanAbsolute =0;
        double sumAbsolute =0;
        double energy = 0;
        double mappedSample;
        int minIndex = -1;
        int i = 0;

        for (short sample: buffer){
            mappedSample = (double) sample/ Math.abs(Short.MIN_VALUE);

            if((i> FIRST_N_SAMPLES_DISCARDED) && Math.abs(mappedSample) > 0.008){
            //if(Math.abs(mappedSample) > 0.008){
                energy+=mappedSample*mappedSample;
                sumAbsolute+= Math.abs(mappedSample);

                if (mappedSample< min){
                    minIndex = i;
                }

                min = Math.min(min,mappedSample);
                max = Math.max(max,mappedSample);
                minRaw = Math.min(minRaw,sample);


                //Log.d("Samples:", i + ": "+sample + ": " + mappedSample );
            }

            i++;

        }

        meanAbsolute = sumAbsolute/buffer.length;
        double rms = Math.sqrt(energy/buffer.length);
        Log.d("No Of Samples:", ""+i);
        Log.d("Energy:", ""+energy);
        Log.d( "RMS:", ""+rms);
        Log.d( "Max:", ""+max);
        Log.d( "Min:", ""+min);
        Log.d( "Min Raw:", ""+minRaw);
        Log.d( "Min Index:", ""+minIndex);
        Log.d( "Mean Absolute:", ""+meanAbsolute);

        return energy;
    }

    @Override
    /**
     *  Implementation of method that was declared in an interface in MicrophoneRecorder
     *  This method gets called the when buffer for the sound data is filled
     */
    public void microphoneBuffer(short[] buffer, int window_size) {
        finishRecording();


        boolean isSilent = isSilence(buffer);

        //Determine if is silence
        if (!isSilent){
            boolean isSpeaking = isSpeech(buffer);
            noSilenceNum++;
            noSilenceDates = noSilenceDates + getDateNow();

            if (isSpeaking){
                vadNum++;
                vadDates = vadDates + getDateNow();

                setStartDataCollection(isSpeaking);
                listener.speech();
            }else{
                listener.noSpeech();
            }

        }else {
            listener.noSpeech();
        }

    }

    /**
     * Listner that defines the function that collects data
     * The defined method is collect when it has been determined that data should be collected
     */
    public interface DataCollectionListener{
        void speech();

        void noSpeech();
    }
}
