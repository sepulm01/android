package org.tensorflow.demo.env;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.tensorflow.demo.DetectorActivity;

public class AlarmAdapter extends BroadcastReceiver {
    private static final String TAG = "Alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        // MediaPlayer mediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI);
        // mediaPlayer.start();
        Log.i(TAG, "Se gatilló");

        Process proc = null;
        try {



            //proc = Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","reboot now"});
            //proc.waitFor();
            Log.i(TAG, "paso el try");


        } catch (Exception ex) {
            Log.i(TAG, "Could not reboot", ex);
        }


    }

}
