package org.tensorflow.demo.env;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

//import com.kosalgeek.asynctask.AsyncResponse;
//import com.kosalgeek.asynctask.PostResponseAsyncTask;

import org.tensorflow.demo.CameraActivity;
import org.tensorflow.demo.DetectorActivity;
import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


/**
 * Created by martin on 25-12-17.
 */

public class GrabaLog extends Activity  {
    private static final String TAG = "GrabaLog" ;
    private static final DetectorActivity DETECTOR_ACTIVITY = new DetectorActivity();

    public void appendLog(String text) {
            File logFile = new File("sdcard/DCIM/logdetector.txt");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(text);
                buf.newLine();
                buf.close();
                HashMap postData = new HashMap();
                postData.put("editText", text);
                postData.put("editText2", text);
                postData.put("editText3", text);
                //PostResponseAsyncTask task = new PostResponseAsyncTask(this, postData);
                //task.execute("https://martinsepulveda.000webhostapp.com/test.php");
                Log.i(TAG, "appendlog" + text);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    public void SetLog(String text) {
        File logFile = new File("sdcard/DCIM/logdetector.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {    //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.write(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        }


    //@Override
    public void processFinish(String result) {
        Log.i(TAG, "processFinish: ");

    }
//screenshot
//screenshot ends

}

