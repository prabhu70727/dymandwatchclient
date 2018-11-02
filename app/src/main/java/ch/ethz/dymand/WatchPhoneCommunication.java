package ch.ethz.dymand;

import android.content.Context;
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


// Initialize and get instance: mWatchPhoneCommunication = WatchPhoneCommunication.getInstance(this);
// Callbacks.WatchPhoneCommCallback.signalPhone is the callback

public class WatchPhoneCommunication implements DataClient.OnDataChangedListener, Callbacks.WatchPhoneCommCallback{

    private static Context context;

    // user intent signal recording done
    private static final String RECORDING_DONE_PATH = "/recording_done";
    private static final String RECORDING_DONE_KEY = "ch.ethz.dymand.recording_done";
    private static final String RECORDING_DONE_MESSAGE = "RD";

    // self report has started signal
    private static final String SELF_REPORT_STARTED_PATH = "/hasStartedSelfReport";
    private static final String SELF_REPORT_STARTED_KEY = "ch.ethz.dymand.hasStartedSelfReport";
    private static final String SELF_REPORT_STARTED_MESSAGE = "SelfReportStarted";

    // self report has-completed signal
    private static final String SELF_REPORT_COMPLETED_PATH = "/hasCompletedSelfReport";
    private static final String SELF_REPORT_COMPLETED_KEY = "ch.ethz.dymand.hasCompletedSelfReport";
    private static final String SELF_REPORT_COMPLETED_MESSAGE = "SelfReportCompleted";

    // self report has-completed ACK signal
    private static final String SELF_REPORT_COMPLETED_ACK_PATH = "/hasCompletedSelfReportACK";
    private static final String SELF_REPORT_COMPLETED_ACK_KEY = "ch.ethz.dymand.hasCompletedSelfReportACK";
    private static final String SELF_REPORT_COMPLETED_ACK_MESSAGE = "SelfReportCompletedACK";

    private static final String GET_CONFIG = "/getconfig";
    private static final String GET_CONFIG_KEY = "DYMAND_GET_CONFIG_KEY";
    private static final String GET_CONFIG_ACK = "DYMAND_GET_CONFIG_ACK";
    private static boolean configAckSent = false;

    private static final String INTERVENE_KEY = "com.example.key.intervention";
    private static final String INTERVENTION = "/intervention";
    private static final String START_INTERVENTION_MESSAGE = "start_intervention";
    private static final String INTERVENTION_ACK = "intervention_ack";
    private static final String SEND_INTENT_MESSAGE = "send_intent";
    private static final String INTENT_ACK = "intent_ack";
    public static boolean intentSent = false;
    private static final String LOG_TAG = "Logs: WatchPhoneCommunication";
    private static WatchPhoneCommunication instance = null;

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


    // TODO: what if connection failed. Two while loops not good
    private void sendRecordingDoneIntention() {
        long end_time_intention = WatchMobileInterface.curTime() + Config.INTENT_EXPIRY;
        while (WatchMobileInterface.curTime() < end_time_intention){
            LocalTimer.blockingLoop(2000);
            if(!intentSent) {
                sendIntentMessage(SEND_INTENT_MESSAGE);
            }
            else {
                intentSent = false;
                break;
            }
        }
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


    private void sendIntentMessage(String message) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(INTERVENTION);
        putDataMapReq.getDataMap().putString(INTERVENE_KEY, message + (System.currentTimeMillis()%100000));
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent(); // this is important
        Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.i("Data Collection", "Sending intent was successful: " + dataItem);
            }
        });
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.i(LOG_TAG, "onDataChanged called in watch.");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                Log.i(LOG_TAG, "onDataChanged data event - changed.");
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(GET_CONFIG) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    sendConfigurationReceivedACK();
                    setConfig(dataMap.getString(GET_CONFIG_KEY));
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
    }

    private void setHasCompletedSelfReport() {
        Log.i(LOG_TAG, "Setting variable hasCompletedSelfReport...");
        sendIntention(SELF_REPORT_COMPLETED_ACK_PATH, SELF_REPORT_COMPLETED_ACK_KEY, SELF_REPORT_COMPLETED_ACK_MESSAGE);
        Config.isSelfReportCompleted = true;
    }

    private void sendConfigurationReceivedACK() {
        Log.i(LOG_TAG, "Sending ACK for configuration...");
        configAckSent = false;
        while (!configAckSent) {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(GET_CONFIG);
            putDataMapReq.getDataMap().putString(GET_CONFIG_KEY, GET_CONFIG_ACK);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.isUrgent();
            Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    configAckSent = true;
                    Log.i(LOG_TAG, "Sending ACK for configuration was successful: " + dataItem);
                }
            });
        }
    }

    // todo break and set the configuration to variables.
    private void setConfig(String configuration) {
        Log.i(LOG_TAG, "Configuration is " + configuration);
    }


    @Override
    public void signalPhone() {
        sendIntention(RECORDING_DONE_PATH, RECORDING_DONE_KEY, RECORDING_DONE_MESSAGE);
    }
}
