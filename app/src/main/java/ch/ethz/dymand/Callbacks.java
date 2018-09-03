package ch.ethz.dymand;

import java.io.FileNotFoundException;

public interface Callbacks {

    interface DataCollectionCallback{
         void collectDataCallBack() throws FileNotFoundException;
    }

    interface MessageCallback{
        void triggerMsg(String msg);
    }

    interface BleCallback{
        void startBleCallback();

        void stopBleCallback();

        void reStartBleCallback();
    }

    interface WatchPhoneCommCallback{
        void signalPhone();
    }
}
