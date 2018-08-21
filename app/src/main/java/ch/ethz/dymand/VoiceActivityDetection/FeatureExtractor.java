package ch.ethz.dymand.VoiceActivityDetection;


import java.util.Arrays;

public class FeatureExtractor {

	
	private static final int FFT_SIZE = 8192;
	private static final int BITRATE = 8000;
    private static final int MFCCS_VALUE = 13;
    private static final int MEL_BANDS = 20;
    
    private static FFT featureFFT = new FFT(FFT_SIZE);
    private static Window featureWin = new Window(BITRATE);
    private static MFCC featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, BITRATE);
    
    public static double[] ComputeFeaturesForFrame(short[] data16bit, int size, int index)
	{
		double[] fftBufferR = new double[FFT_SIZE];
        double[] fftBufferI = new double[FFT_SIZE];
        double[] featureCepstrum = new double[MFCCS_VALUE];

        // Frequency analysis
        Arrays.fill(fftBufferR, 0);
        Arrays.fill(fftBufferI, 0);

        // Convert audio buffer to doubles
        for (int i = 0; i < size; i++)
        {
                fftBufferR[i] = data16bit[index+i];
        }

        // In-place windowing
        featureWin.applyWindow(fftBufferR);

        // In-place FFT
        featureFFT.fft(fftBufferR, fftBufferI);

        // Get MFCCs
        featureCepstrum = featureMFCC.cepstrum(fftBufferR, fftBufferI);
        
        return featureCepstrum;
	}

}
