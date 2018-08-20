package ch.ethz.dymand;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by prabhu on 09.02.18.
 */

public class LocalTimer {

    private static boolean isStarted = false;
    private static long end_time;
    private static long next_hour;
    private static boolean isRecordingDoneThisHour;


    public static void start(){
        isStarted = true;
        end_time = curTime() + Config.SERVICE_LIFE;
        next_hour = curTime() + Config.HOUR;
        isRecordingDoneThisHour = false;
    }

    public static long curTime(){
        return System.currentTimeMillis();
    }

    public static boolean online() {

        if (isStarted != true) throw new AssertionError();

        if(curTime()>next_hour){
            isRecordingDoneThisHour = false;
            next_hour = next_hour + Config.HOUR;
        }

        if(curTime()<end_time){
            return true;
        }
        else {
            return false;
        }
    }

    // TODO: currently a temp one
    public static boolean recordThisHour() {
        if (isStarted != true) throw new AssertionError();

        if (isRecordingDoneThisHour) return false;
        else return true;
    }

    public static void disableRecordingThishour() {
        isRecordingDoneThisHour = true;
    }

    public static void blockingLoop(int recordTime) {
        long endLoop = curTime() + recordTime;
        while(curTime()<endLoop);
    }

    public static void synchronize() {
    }

    public static String timeStamp() {
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        return timeStamp;
    }
}
