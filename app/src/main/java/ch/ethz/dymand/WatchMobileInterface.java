package ch.ethz.dymand;

import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: the current one is just a demo
public class WatchMobileInterface {
    private static boolean doIntervene = true;
    private static long end_time;

    public static long curTime(){
        return System.currentTimeMillis();
    }

    // The watch needs to be online for al 7 days.
    public static boolean online() {
        if(curTime()<end_time)  return true;
        else return false;
    }

    public static boolean recordThisHour() {
        return doIntervene;
    }
    public static boolean intervene() {
        return doIntervene;
    }

    public static void disableRecordingThisHour() {
        doIntervene = false;
    }

    public static void start(){
        end_time = curTime() + Config.SERVICE_LIFE;
    }

    public static String timeStamp() {
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        return timeStamp;
    }

    public static void setStartIntervention() {
        doIntervene = true;
    }
    public static void setStopIntervention() {
        doIntervene = false;
    }
}
