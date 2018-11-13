package ch.ethz.dymand;

import android.content.Context;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import OldCode.LocalTimer;
import OldCode.WatchMobileInterface;

import static android.content.Context.VIBRATOR_SERVICE;
import static ch.ethz.dymand.Callbacks.MessageCallback;
import static ch.ethz.dymand.Config.DEBUG_MODE;

// Initialize and get instance: mWatchPhoneCommunication = WatchPhoneCommunication.getInstance(this);
// Callbacks.WatchPhoneCommCallback.signalPhone is the callback

public class WatchPhoneCommunication implements DataClient.OnDataChangedListener, Callbacks.WatchPhoneCommCallback{

    private static Context context;

    //------Receiving signals-START----------

    // self report has started signal (key not required as message content not relevant)
    private static final String SELF_REPORT_STARTED_PATH = "/hasStartedSelfReport";

    // self report has-completed signal (key not required as message content not relevant)
    private static final String SELF_REPORT_COMPLETED_PATH = "/hasCompletedSelfReport";

    // get config signal
    private static final String GET_CONFIG_PATH = "/getconfig";
    private static final String GET_CONFIG_KEY = "dymand.get.config.key";

    //------Receiving signals-END----------


    //------Sending signals-START----------

    // user intent signal recording done
    private static final String RECORDING_DONE_PATH = "/recording_done";
    private static final String RECORDING_DONE_KEY = "ch.ethz.dymand.recording_done";
    private static final String RECORDING_DONE_MESSAGE = "RD";

    // self report has-completed ACK signal
    private static final String SELF_REPORT_COMPLETED_ACK_PATH = "/hasCompletedSelfReportACK";
    private static final String SELF_REPORT_COMPLETED_ACK_KEY = "ch.ethz.dymand.hasCompletedSelfReportACK";
    private static final String SELF_REPORT_COMPLETED_ACK_MESSAGE = "SelfReportCompletedACK";

    //------Sending signals-END----------

    private static final String LOG_TAG = "Logs: WatchPhoneCommunication";
    private static WatchPhoneCommunication instance = null;
    private static MessageCallback msg;

    public static WatchPhoneCommunication getInstance(Context contxt){
        Log.i(LOG_TAG, "getInstance is called...");
        if (instance == null) {
            Log.i(LOG_TAG, "Instance is null...");
            instance = new WatchPhoneCommunication();
            context = contxt;

            Wearable.getDataClient(context).addListener(WatchPhoneCommunication.instance).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(LOG_TAG, "Success while adding listener");
                }
            });

        }
        context = contxt;
        return instance;
    }

    public void subscribeMessageCallback(MessageCallback msgInput){
        msg = msgInput;
    }

    private void sendIntention(String path, String key, final String message) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        final String toSend = message + (System.currentTimeMillis()%100000);
        putDataMapReq.getDataMap().putString(key, toSend);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                //messageSent[0] = true;
                Log.i(LOG_TAG, "Sending intent was successful: " + toSend+ " " + dataItem);
            }
        });
    }

    //this functions contents is for the "user intent" callback
    public void sendRecordingDoneUserIntention(){
        sendIntention(RECORDING_DONE_PATH, RECORDING_DONE_KEY, RECORDING_DONE_MESSAGE);
    }


    // The most important function of this class, when there is any change in the buffer that
    // connects watch and the phone.
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.i(LOG_TAG, "onDataChanged called in watch.");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                Log.i(LOG_TAG, "onDataChanged data event - changed.");
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(GET_CONFIG_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    setConfig(dataMap.getString(GET_CONFIG_KEY));
                    Wearable.getDataClient(context).deleteDataItems(item.getUri());
                }
                if (item.getUri().getPath().compareTo(SELF_REPORT_STARTED_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    Log.i(LOG_TAG, "Self report started signal received:" + item.getUri());
                    Wearable.getDataClient(context).deleteDataItems(item.getUri());
                    setHasStartedSelfReport();
                }

                if (item.getUri().getPath().compareTo(SELF_REPORT_COMPLETED_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    Log.i(LOG_TAG, "Self report completed signal received:" + item.getUri());
                    Wearable.getDataClient(context).deleteDataItems(item.getUri());
                    setHasCompletedSelfReport();
                }


            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.i(LOG_TAG, "onDataChanged data event - deleted.");
                // DataItem deleted
            }
        }
    }

    private void setHasStartedSelfReport() {
        Log.i(LOG_TAG, "Setting variable hasStartedSelfReport...");
        Config.hasSelfReportBeenStarted = true;


        if (DEBUG_MODE == true){
            msg.triggerMsg("Self Report Started");
            Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            v.vibrate(500); // Vibrate for 500 milliseconds
        }

    }

    private void setHasCompletedSelfReport() {
        Log.i(LOG_TAG, "Setting variable hasCompletedSelfReport...");
        sendIntention(SELF_REPORT_COMPLETED_ACK_PATH, SELF_REPORT_COMPLETED_ACK_KEY, SELF_REPORT_COMPLETED_ACK_MESSAGE);
        Config.isSelfReportCompleted = true;


        if (DEBUG_MODE == true){
            msg.triggerMsg("Self Report Completed");
            Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            v.vibrate(500); // Vibrate for 500 milliseconds
        }
    }


    // break and set the configuration to variables.
    private void setConfig(String configuration) {
        Log.i(LOG_TAG, "Configuration is " + configuration);
        String [] tokens = configuration.split(" ");
        String [] configTokens = tokens[1].split("-");
        Config.morningStartHourWeekday = Integer.parseInt(configTokens[0]);
        Config.morningEndHourWeekday = Integer.parseInt(configTokens[1]);
        Config.eveningStartHourWeekday = Integer.parseInt(configTokens[2]);
        Config.eveningEndHourWeekday = Integer.parseInt(configTokens[3]);
        Config.startHourWeekend = Integer.parseInt(configTokens[4]);
        Config.endHourWeekend = Integer.parseInt(configTokens[5]);
        Config.configReceived = true;
    }


    @Override
    public void signalPhone() {
        sendIntention(RECORDING_DONE_PATH, RECORDING_DONE_KEY, RECORDING_DONE_MESSAGE);
    }
}
