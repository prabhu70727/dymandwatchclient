package ch.ethz.dymand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.ethz.dymand.Setup.MainActivity;

public class StartActivityAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
