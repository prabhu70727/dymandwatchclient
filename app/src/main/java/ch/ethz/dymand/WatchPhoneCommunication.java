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

public class WatchPhoneCommunication implements DataClient.OnDataChangedListener{

    private static Context context;

    private static final String INTERVENE_KEY = "com.example.key.intervention";
    private static final String INTERVENTION = "/intervention";
    private static final String START_INTERVENTION_MESSAGE = "start_intervention";
    private static final String INTERVENTION_ACK = "intervention_ack";
    private static final String SEND_INTENT_MESSAGE = "send_intent";
    private static final String INTENT_ACK = "intent_ack";
    public static boolean intentSent = false;
    private static final String LOG_TAG = "WatchPhoneCommunication";
    private static WatchPhoneCommunication instance = null;

    public static WatchPhoneCommunication getInstance(Context contxt){
        if (instance == null) {
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
    private void sendIntention() {
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

    private void sendIntentMessage(String message) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(INTERVENTION);
        putDataMapReq.getDataMap().putString(INTERVENE_KEY, message + (System.currentTimeMillis()%100000));
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.isUrgent();
        Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.i("Data Collection", "Sending intent was successful: " + dataItem);
            }
        });
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.i(LOG_TAG, "onDataChanged in watch.");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(INTERVENTION) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    update(dataMap.getString(INTERVENE_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    //todo use (`prabhu) and remove
    private void update(String string) {
        if(string.contains(START_INTERVENTION_MESSAGE)){
            Log.i(LOG_TAG, "Start Intervention is received.");
            WatchMobileInterface.setStartIntervention();
            sendAckToMobile();
        }
        else if (string.contains(INTENT_ACK)){
            Log.i(LOG_TAG, "Intent ACK is received.");
            intentSent = true;
        }
    }

    //todo use (`prabhu) and remove
    private void sendAckToMobile() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(INTERVENTION);
        putDataMapReq.getDataMap().putString(INTERVENE_KEY, INTERVENTION_ACK +  (System.currentTimeMillis()%100000));
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.isUrgent();
        Wearable.getDataClient(context).putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.i(LOG_TAG, "Sending intervention ack was successful: " + dataItem);
            }
        });
    }
}
