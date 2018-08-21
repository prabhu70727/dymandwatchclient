package ch.ethz.dymand.VoiceActivityDetection;

import static ch.ethz.dymand.VoiceActivityDetection.ConfigVAD.*;


/**
 * Created by George Boateng on 11/19/17.
 */
public class SpeechDetector {

/*
//    static boolean shouldNormalize = true;
//    static double MEAN[] = {63.692215337671094,5.669018422622394,4.182359657536262,1.5279457894593562,2.3330695178472944,2.0885882403301914,2.4241360342821237,2.218465480667592,1.40154638829223,1.4266175600817452,1.9628366117124203,1.2514646343414741,1.570759705935991};
//    static double SCALER[] = {3.782295701971998,1.725827517783884,2.2942711256920245,2.842981404141293,2.625599958757049,2.2943713674936443,2.043022091758682,1.8315158257629063,2.0540177350398574,1.9826445502718606,1.709456554258989,1.581476907361939,1.4663404817974528};
//    static double COEFFICIENTS[] ={0.2051195430250711,0.3242896618568211,0.26196765342370887,-1.1708049953116633,-0.04055691918473101,-0.7649808339477223,0.24282352436351948,-0.012400564690278953,-0.25279759701314186,-0.14034758719427792,-0.0269701248024445,-0.09643715321261084,0.1251023018882137};
//    static double INTERCEPT = 0.671265;
*/

    //Test Data
    //static double features[] = {63.721202628,4.99866481534,2.77794165237,2.98494545958,0.812736913317,-3.20707603339,-3.14264703748,-3.17146770407,-2.91303470285,0.688112305874,1.12262817671};
    //static double features[]={64.5689032206,3.46285428373,-0.740694633751,3.46696759862,0.514523674617,-1.19792489186,-4.5110961056,-4.29012409966,-2.13877833635,-0.231528678552,-1.59896665806};

    //Perform classification
    public static boolean Classify(double[] features){
        boolean classification = false;
        double coeff, y;
        double wx = 0; //sum of coeefficient x features

        if (shouldNormalize == true){
            features =  normalizeFeatures (features);
        }

        //
        //Implement decision function of linear SVM: y = wx +b
        for (int i = 0; i < COEFFICIENTS.length; i++){
            coeff = COEFFICIENTS[i];
            wx += (coeff * features[i]) ;
        }

        y = wx+INTERCEPT;

        if (y > 0) {
            classification = true;
        }

        //Return result
        return classification;
    }


    public static double[]  normalizeFeatures (double[] features){
        double[] normFeatures = new double[features.length];

        for (int i = 0; i < features.length; i++) {
            normFeatures[i] = (features[i] - MEAN[i])/SCALER[i];
        }

        return normFeatures;
    }
}
