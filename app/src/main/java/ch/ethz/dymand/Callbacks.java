package ch.ethz.dymand;

public interface Callbacks {

    interface DataCollectionCallback{
         void collectDataCallBack();

         void triggerEndOfDayDiary();
    }

    interface MessageCallback{
        void triggerMsg(String msg);
    }

    interface BleCallback{
        void startBleCallback();

        void stopBleCallback();

        void reStartBleCallback();
    }
}
