package ch.ethz.dymand;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import ch.ethz.dymand.Setup.MainActivity;

import static ch.ethz.dymand.Config.errorDates;
import static ch.ethz.dymand.Config.errorLogFile;
import static ch.ethz.dymand.Config.errorLogs;
import static ch.ethz.dymand.Config.getDateNow;
import static ch.ethz.dymand.Config.hasLogFileBeenCreated;
import static ch.ethz.dymand.Config.noOfExceptionsInHour;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Context contxt;
    private String LOG_TAG = "ExceptionHandler";

    public ExceptionHandler(Context context){
        contxt = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {

        Log.i(LOG_TAG, "Exception Caught");
        Toast.makeText(contxt, "Service Detroyed!", Toast.LENGTH_SHORT).show();

        noOfExceptionsInHour++; //increment number of exceptions
        errorDates += getDateNow();

        //Save the no of exceptoons
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(contxt);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("noOfExceptions", noOfExceptionsInHour);
        editor.putString("errorDates", errorDates);
        editor.apply();

        Intent intent = new Intent(contxt,MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(contxt, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) contxt.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 10000, pendingIntent);

        StringWriter stackTrace = new StringWriter();
        e.printStackTrace( new PrintWriter(stackTrace));
        StringBuilder log = new StringBuilder();
        log.append(getDateNow() + ", " + e.getMessage() + " , " + e.getCause() + " , " + e.toString() +  "\n" + stackTrace.toString() +  "\n");
        FileOutputStream errorLogStream = null;

        try {
            errorLogStream = new FileOutputStream(errorLogFile, true);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        try {
            errorLogStream.write(log.toString().getBytes());
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            try {
                errorLogStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        Intent mService = new Intent(contxt, FGService.class);
        contxt.stopService(mService);

        //contxt.startService(mService);
//
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(2);
    }
}
