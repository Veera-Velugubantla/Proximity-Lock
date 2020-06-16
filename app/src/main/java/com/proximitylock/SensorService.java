package com.proximitylock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

public class SensorService extends Service implements SensorEventListener {

    private Sensor proximitySensor;
    private SensorManager sensorManager;
    private SharedPreferences preferences;
    int count = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        final long start = System.currentTimeMillis();

        count = count+1;


        this.preferences = ProximityApp.getAppContext().getSharedPreferences(Strings.SHARED_PREF_NAME, MODE_PRIVATE);
        final int timeout = Integer.parseInt(preferences.getString(Strings.TIMEOUT_KEY, "300"));
        final float calibration = Float.parseFloat(preferences.getString(Strings.CALIBRATION_KEY, "123.4"));


        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km.inKeyguardRestrictedInputMode();

        PowerManager pm1 = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm1.isScreenOn();



        if (count == 2) {
            if (!isScreenOn) {

                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "CHESS");
                wl.acquire();
                try {
                    Thread.sleep(4 * 1000); // 30 seconds
                } catch (Exception e) {
                } finally {
                    count = 0;
                    wl.release();

                }

            } else {

                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        count = 0;

                        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        devicePolicyManager.lockNow();

                    }
                });
                t.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.proximitySensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        this.sensorManager.registerListener(this, this.proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        this.preferences = ProximityApp.getAppContext().getSharedPreferences(Strings.SHARED_PREF_NAME, MODE_PRIVATE);
        boolean persistent = this.preferences.getBoolean(Strings.PERSISTENT_KEY, true);

        if (persistent)
        {
            Intent temp = new Intent(SensorService.this, MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(SensorService.this, 0, temp, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Proximity Lock")
                    .setContentText("Touch proximity sensor to lock")
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setContentIntent(pIntent);
            Notification barNotif = builder.build();
            this.startForeground(1, barNotif);
        }


        return Service.START_STICKY;
    }
}
