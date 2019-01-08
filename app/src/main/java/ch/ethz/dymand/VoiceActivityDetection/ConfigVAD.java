package ch.ethz.dymand.VoiceActivityDetection;

public class ConfigVAD {

    //VAD Constants
    public static final double DEVICE_NOISE_LEVEL = 0.01; //the level of noise by device
    public static final int FRAME_SIZE_MS = 25; // each frame is 25ms
    public static final double RMS_THRESHOLD = 0.002; //If energy is above this threshold, then the should sample is no silence
    public static final int NO_OF_SECONDS = 5; //5 seconds
    public static final int FREQUENCY = 8000; //8kHz
    public static final int SAMPLES_PER_FRAME = (FRAME_SIZE_MS * FREQUENCY)/1000; //(8000*25)/100 or 8*25 = 200 samples
    public static final int WINDOW_SIZE = FREQUENCY * NO_OF_SECONDS; //8000*5 = 40,000
    public static final int VOICE_THRESHOLD = (WINDOW_SIZE/SAMPLES_PER_FRAME)/2; //(40,000/200)/2; //threshold for deciding classification of window is half the total count
    public static int FIRST_N_SAMPLES_DISCARDED = 9000; //(int)(0.1*WINDOW_SIZE); //discard the first 10% of values during the energy calculation because of blip when microphone starts

    //Speech Classification Constants
    static boolean shouldNormalize = true;
    static double MEAN[] = {70.12902147890048,9.142384520427468,2.528480640673784,0.7298028892619988,-0.22555648058769387,0.5600517247730882,-0.26119927873329757,-0.22558989777956084,-2.0276324693430507,-1.3677102967284889,-0.41428386444632725,-0.6703190688987327,-0.1261285673079645};
    static double SCALER[] = {13.446659873189867,2.6945376714712657,3.8354717297768968,3.148688539983778,3.4680397934988716,3.364248404958805,3.3018660223576832,3.115889032760712,2.9947152895368885,2.9863405497001385,2.8355411240627015,2.4432959661200524,2.348801798098901};
    static double COEFFICIENTS[] ={0.47447853377297355,-0.21397418277033475,0.4420619876802315,-0.43459444277646275,-0.012645792316555776,-0.2285590135833364,0.07257085693767044,0.2198160664470281,-0.30695366034514754,0.24012051332377413,-0.09312912426935699,0.02933081828646356,-0.15258918937772054};
    static double INTERCEPT = 0.09793356;

}
