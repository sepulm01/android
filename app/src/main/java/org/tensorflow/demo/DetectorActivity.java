/*
 * Copyright 2018 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.AlarmAdapter;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.Contadores;
import org.tensorflow.demo.env.GPSTracker;
import org.tensorflow.demo.env.GrabaLog;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.env.SensorService;
import org.tensorflow.demo.env.senddata;
import org.tensorflow.demo.tracking.MultiBoxTracker;
import org.tensorflow.lite.demo.R; // Explicit import needed for internal Google builds.

import static org.tensorflow.demo.env.Contadores.GPSIDactual;
import static org.tensorflow.demo.env.Contadores.frec_fotos;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final senddata SENDDATA = new senddata();
  private static final GrabaLog GRABA_LOG = new GrabaLog();
  GPSTracker gps;

  long MIN_TIME = 5000;
  float MIN_DISTANCE = 10;
  String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;
  LocationManager mLocationManager;
  LocationListener mLocationListener;

  int FOTO_CONT = 1;
  //Martin Servicio
  Intent mServiceIntent;
  private SensorService mSensorService;
  Context ctx;
  public Context getCtx() {
    return ctx;
  }

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list_SP.txt";
  
  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  private static final DetectorMode MODE = DetectorMode.TF_OD_API;

  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

  private static final boolean MAINTAIN_ASPECT = false;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;

  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private byte[] luminanceCopy;

  private BorderedText borderedText;

  //todo Martin: Comienza la intervención de código.
  static boolean calledAlready = false;
  protected void onCreate(Bundle savedInstanceState) {

    //graba datos sin conección a Firebase
    if (!calledAlready)
    {
      FirebaseDatabase.getInstance().setPersistenceEnabled(true);
      calledAlready = true;
    }
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    super.onCreate(savedInstanceState);


    // TODO fija la alarma a una hora fija
/*    boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 0,
            new Intent("org.tensorflow.demo.env.AlarmAdapter"),
            PendingIntent.FLAG_NO_CREATE) != null);

    if (alarmUp)
    {
      Log.d("myTag", "Alarm is already active");
    } else {horaFija();}*/


    //

    //frecuencia para tomar fotos
    frec_fotos = 1000;

    //martin Servicio

    ctx = this;
    //setContentView(R.layout.activity_main);
    mSensorService = new SensorService(getCtx());
    mServiceIntent = new Intent(getCtx(), mSensorService.getClass());
    if (!isMyServiceRunning(mSensorService.getClass())) {
      startService(mServiceIntent);
      LOGGER.i("My paso x aqui");
    }


    //Loop para no cerrar la aplicación.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);


    // FIJA GPS MANUAL
    //Contadores.lat = "-33@420";
    //Contadores.lon = "-70@620";

    // create class object GPS
      //getWeatherforCurrentlocation();

      
      
  //  gps = new GPSTracker(DetectorActivity.this);
  //  if (gps.canGetLocation()) {LOGGER.i("GPS can get location");}

      gps = new GPSTracker(DetectorActivity.this);
     // check if GPS enabled
     if (gps.canGetLocation()) {
     try {
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();
        Contadores.lat = String.valueOf(latitude).replace(".", "@").substring(0, 7);
        Contadores.lon = String.valueOf(longitude).replace(".", "@").substring(0, 7);
     //Contadores.lat = "-33@426";
     //Contadores.lon = "-70@622";
          }catch (Exception e) {
          e.printStackTrace();
          LOGGER.i("SetGPSID error");

         // martin rutina para re iniciar la app al momento de partir y tener los datos de GPS
         Intent mStartActivity = new Intent(this, DetectorActivity.class);
         int mPendingIntentId = 123456;
         PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
         AlarmManager mgr = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
         mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
         System.exit(0);
         }

     } else {
     // can't get location
     // GPS or Network is not enabled
     // Ask user to enable GPS/network in settings
     //gps.showSettingsAlert();
     }
    SENDDATA.SetGPSID();

    //Fija la fecha de hoy al inicio
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Contadores.hoy = sdf.format(new Date());

    LOGGER.i("SetGPSID aqui");
    if(Contadores.ListaObjetos.isEmpty()) {
      Contadores.ContRevisados = false;
      SENDDATA.CheckContadores();
    }
    // LOGGER.i(CheckContadores aqui");
  }

    private void getWeatherforCurrentlocation() {
      mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      mLocationListener = new LocationListener() {
          @Override
          public void onLocationChanged(Location location) {
              LOGGER.i("GPS cambió call back recivido");
              String Longitude = String.valueOf(location.getLongitude());
              String Latitude = String.valueOf(location.getLatitude());
              LOGGER.i("GPS Laitud: " + Latitude+" Longitud: "+Longitude);
              Contadores.lat = String.valueOf(Latitude).replace(".", "@").substring(0, 7);
              Contadores.lon = String.valueOf(Longitude).replace(".", "@").substring(0, 7);
              SENDDATA.SetGPSID();
          }

          @Override
          public void onStatusChanged(String s, int i, Bundle bundle) {

          }

          @Override
          public void onProviderEnabled(String s) {

          }

          @Override
          public void onProviderDisabled(String s) {
              LOGGER.i("GPS no disponible");

          }
      };
      mLocationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, mLocationListener);
    }


    private boolean isMyServiceRunning(Class<? extends SensorService> serviceClass) {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        Log.i ("isMyServiceRunning?", true+"");
        return true;
      }
    }
    Log.i ("isMyServiceRunning?", false+"");
    return false;
  }

  
  
  // **** martin metodos incluidos para el Servicio
  @Override
  public void onPause() {
    //stopService(mServiceIntent);
    Log.i("MAINACT", "onPause!");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");
    String currentDateandTime = sdf.format(new Date());
    GRABA_LOG.appendLog("onPause! at:"+currentDateandTime);
    super.onPause();

    // manage other components that need to respond
    // to the activity lifecycle
  }
  @Override
  public void onStop() {
    //stopService(mServiceIntent);
    Log.i("MAINACT", "onStop!");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");
    String currentDateandTime = sdf.format(new Date());
    GRABA_LOG.appendLog("onStop! at:"+currentDateandTime);
    super.onStop();

    // manage other components that need to respond
    // to the activity lifecycleadba
  }

  @Override
  public void onDestroy() {
    stopService(mServiceIntent);

    Log.i("MAINACT", "onDestroy!");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");
    String currentDateandTime = sdf.format(new Date());
    GRABA_LOG.appendLog("onDestroy! at:"+currentDateandTime);
    super.onDestroy();

  }
  //**** fin metodos para el servicio

  //martin sección para no evitar que la aplicación pierda el foco.
 /* @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (!hasFocus) {
      Intent i = new Intent(getBaseContext(), DetectorActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
      startActivity(i);
    }
  }*/

  // Fija alarma para reboot el sistema.
  private void setAlarm(long timeInMillis, Calendar c) {
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

    Intent intent = new Intent(DetectorActivity.this, AlarmAdapter.class);

    PendingIntent pendingIntent = PendingIntent.getBroadcast(DetectorActivity.this, 0, intent, 0);

    alarmManager.setRepeating(AlarmManager.RTC, timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent);


    int hour = c.get(Calendar.HOUR_OF_DAY);
    int minute = c.get(Calendar.MINUTE);
    int ampm = c.get(Calendar.AM_PM);
    String day = "";
    if (ampm == Calendar.AM) {
      day = "AM";
    } else if (ampm == Calendar.PM) {
      day = "PM";
    }
    String timeText = "Reinicio a las: ";
    timeText += hour + ": " + minute + " " + day;
    //tv_display.setText(timeText);
    Toast.makeText(DetectorActivity.this, timeText, Toast.LENGTH_LONG).show();

  }

    private void horaFija(){
    Calendar calendar = Calendar.getInstance();

    if(Build.VERSION.SDK_INT >= 23) {
      calendar.set(
              calendar.get(Calendar.YEAR),
              calendar.get(Calendar.MONTH),
              calendar.get(Calendar.DAY_OF_MONTH),
              12,
              33,
              0
        );
      }        else{
      calendar.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            12,
            33,
            0
            );
      }
    setAlarm(calendar.getTimeInMillis(), calendar);
  }




  //todo Martin: Finaliza intervención de código.
  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      LOGGER.e("Exception initializing classifier!", e);
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }


    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            if (!isDebug()) {
              return;
            }
            final Bitmap copy = cropCopyBitmap;
            if (copy == null) {
              return;
            }

            final int backgroundColor = Color.argb(100, 0, 0, 0);
            canvas.drawColor(backgroundColor);

            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                canvas.getWidth() - copy.getWidth() * scaleFactor,
                canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<String>();
            if (detector != null) {
              final String statString = detector.getStatString();
              final String[] statLines = statString.split("\n");
              for (final String line : statLines) {
                lines.add(line);
              }
            }
            lines.add("");

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Rotation: " + sensorOrientation);
            lines.add("Inference time: " + lastProcessingTimeMs + "ms");

            borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
          }
        });
  }

  OverlayView trackingOverlay;

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    byte[] originalLuminance = getLuminance();
    tracker.onFrame(
        previewWidth,
        previewHeight,
        getLuminanceStride(),
        sensorOrientation,
        originalLuminance,
        timestamp);
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    if (luminanceCopy == null) {
      luminanceCopy = new byte[originalLuminance.length];
    }
    System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }
      //TODO martin comenté este if por el de más abajo para que saque una foto cada 1000 imagenes

      if (FOTO_CONT < currTimestamp){
          // if(frec_fotos == 1){
          ImageUtils.saveBitmap(croppedBitmap);
          FOTO_CONT = FOTO_CONT + frec_fotos;
          LOGGER.i("imagen grabada curr:"+ currTimestamp+" cont:"+FOTO_CONT+ " Frecuencia de fotos:"+frec_fotos);
          SENDDATA.uploadImage(GPSIDactual);
         // SENDDATA.estado();


      }



    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }

            final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }

            tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
            trackingOverlay.postInvalidate();

            requestRender();
            computingDetection = false;
          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onSetDebug(final boolean debug) {
    detector.enableStatLogging(debug);
  }
}
