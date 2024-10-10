/*
 * Copyright 2013 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.PedometerActivity;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.vm.SpeedViewModel;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.API23Wrapper;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.API26Wrapper;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Logger;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Util;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Locale;

import timber.log.Timber;

/**
 * Background service which keeps the step-sensor listener alive to always get
 * the number of steps since boot.
 * <p/>
 * This service won't be needed any more if there is a way to read the
 * step-value without waiting for a sensor event
 */
public class SensorListener extends Service implements SensorEventListener {

    public final static int NOTIFICATION_ID = 1;
    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private final static long SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HOUR;
    private final static int SAVE_OFFSET_STEPS = 500;
    public static final String BROADCAST_ACTION_STEPS_DETECTED =
            "com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.STEPS_DETECTED";
    public static final String EXTENDED_DATA_NEW_STEPS =
            "com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.NEW_STEPS";
    public static final String EXTENDED_DATA_TOTAL_STEPS =
            "com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.TOTAL_STEPS";
    public static boolean SENSOR_ACCEL = false;

    private static int steps;
    private static int lastSaveSteps;
    private static long lastSaveTime;
    private SpeedViewModel pedoValue;

    private final BroadcastReceiver shutdownReceiver = new ShutdownRecevier();

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // nobody knows what happens here: step value might magically decrease
        // when this method is called...
        if (BuildConfig.DEBUG) Logger.log(sensor.getName() + " accuracy changed: " + accuracy);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            goToAccelerometer(event);
            updateIfNecessary();
            //showNotification(); // update notification
        } else {
            if (event.values[0] > Integer.MAX_VALUE) {
                if (BuildConfig.DEBUG)
                    Logger.log("probably not a real value: " + event.values[0]);
            } else {
                steps = (int) event.values[0];
                updateIfNecessary();
                showNotification(); // update notification
            }
        }
    }

    /**
     * @return true, if notification was updated
     */
    private boolean updateIfNecessary() {
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + 2000)) {
            if (BuildConfig.DEBUG) Logger.log(
                    "saving steps: steps=" + steps + " lastSave=" + lastSaveSteps +
                            " lastSaveTime=" + new Date(lastSaveTime));
            Database db = Database.getInstance(this);
            if (db.getSteps(Util.getToday()) == Integer.MIN_VALUE) {
                int pauseDifference = steps -
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                .getInt("pauseCount", steps);
                db.insertNewDay(Util.getToday(), steps - pauseDifference);
                if (pauseDifference > 0) {
                    // update pauseCount for the new day
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("pauseCount", steps).apply();
                }
            }
            db.saveCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();
            showNotification(); // update notification
            return true;
        } else {
            return false;
        }
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NOTIFICATION_ID, getNotification(this));
        } else if (getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                .getBoolean("notification", true)) {
            /*((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_ID, getNotification(this));*/
            startForeground(NOTIFICATION_ID, getNotification(this));
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        reRegisterSensor();
        registerBroadcastReceiver();
        if (!updateIfNecessary()) {
            showNotification();
        }

        // restart service every hour to save the current step count
        long nextUpdate = Math.min(Util.getTomorrow(),
                System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR);
        if (BuildConfig.DEBUG) Logger.log("next update: " + new Date(nextUpdate).toLocaleString());
        AlarmManager am =
                (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent
                .getService(getApplicationContext(), 2, new Intent(this, SensorListener.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23) {
            API23Wrapper.setAlarmWhileIdle(am, AlarmManager.RTC, nextUpdate, pi);
        } else {
            am.set(AlarmManager.RTC, nextUpdate, pi);
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) Logger.log("SensorListener onCreate");
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (BuildConfig.DEBUG) Logger.log("sensor service task removed");
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, SensorListener.class), 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) Logger.log("SensorListener onDestroy");
        try {
            unregisterReceiver(shutdownReceiver);
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Logger.log(e);
            e.printStackTrace();
        }
    }

    @SuppressLint("StringFormatInvalid")
    public Notification getNotification(final Context context) {
        //if (BuildConfig.DEBUG) Logger.log("getNotification");
        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        int goal = prefs.getInt("goal", 10000);
        Database db = Database.getInstance(context);
        int today_offset = db.getSteps(Util.getToday());
        if (steps == 0)
            steps = db.getCurrentSteps(); // use saved value if we haven't anything better
        db.close();
        Notification.Builder notificationBuilder =
                Build.VERSION.SDK_INT >= 26 ? API26Wrapper.getNotificationBuilder(context) :
                        new Notification.Builder(context);
        if (steps > 0) {
            try {
                if (SENSOR_ACCEL) {
                    if (today_offset == Integer.MIN_VALUE) today_offset = 0;
                    NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
                    notificationBuilder.setProgress(goal, today_offset + steps, false).setContentText(
                            today_offset + steps >= goal ?
                                    context.getString(R.string.goal_reached_notification,
                                            format.format((today_offset + steps))) :
                                    context.getString(R.string.notification_text,
                                            format.format((goal - today_offset - steps)))).setContentTitle(
                            format.format(today_offset + steps) + " " + context.getString(R.string.steps));
                    AppUtils.INSTANCE.getDefaultPreferences(SensorListener.this).edit()
                            .putInt("pedo_service_value", today_offset + steps).apply();
                    Timber.e("sensor is acdel");
                } else {
                    if (today_offset == Integer.MIN_VALUE) today_offset = -steps;
                    NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
                    notificationBuilder.setProgress(goal, today_offset + steps, false).setContentText(
                            today_offset + steps >= goal ?
                                    context.getString(R.string.goal_reached_notification,
                                            format.format((today_offset + steps))) :
                                    context.getString(R.string.notification_text,
                                            format.format((goal - today_offset - steps)))).setContentTitle(
                            format.format(today_offset + steps) + " " + context.getString(R.string.steps));
                    AppUtils.INSTANCE.getDefaultPreferences(SensorListener.this).edit()
                            .putInt("pedo_service_value", today_offset + steps).apply();
                    Timber.e("sensor is step");
                }
            } catch (NumberFormatException | FormatFlagsConversionMismatchException | IllegalStateException e) {
                e.printStackTrace();
            }

        } else { // still no step value?
            notificationBuilder.setContentText(
                    context.getString(R.string.your_progress_will_be_shown_here_soon))
                    .setContentTitle(context.getString(R.string.notification_title));
        }
        notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                .setContentIntent(PendingIntent
                        .getActivity(context, 0, new Intent(context, PedometerActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_notification).setOngoing(true);
        return notificationBuilder.build();
    }

    private void registerBroadcastReceiver() {
        if (BuildConfig.DEBUG) Logger.log("register broadcastreceiver");
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SHUTDOWN);
            registerReceiver(shutdownReceiver, filter);
        } catch (Exception exp) {
        }
    }

    private void reRegisterSensor() {

        if (BuildConfig.DEBUG) Logger.log("re-register sensor listener");

        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Logger.log(e);
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            Logger.log("step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1) {
                if (sm.getSensorList(Sensor.TYPE_ACCELEROMETER).size() < 1)
                    return; // emulator
            }
            //Logger.log("default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).getName());
        }

        // enable batching with delay of max 5 min
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) {
            SENSOR_ACCEL = true;
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        sm.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_NORMAL, 0);

    }

    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private float last_sign;
    private float last_acceleration_diff;
    float accelerometerThreshold = 0.75f;
    private float[] last_extrema = new float[2];
    private long last_step_time;
    private int valid_steps = 0;
    int validStepsThreshold = 0;
    private float last_acceleration_value;
    private int mLastStepDeltasIndex = 0;
    private float[] mLastStepAccelerationDeltas = {-1, -1, -1, -1, -1, -1};
    private long[] mLastStepDeltas = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    private int mLastStepAccelerationDeltasIndex = 0;

    // #Accel
    private void goToAccelerometer(SensorEvent event) {
        if (event.values.length != 3) {
            Logger.log("Invalid sensor values.");
        }

        SENSOR_ACCEL = true;
        // the following part will add some basic low/high-pass filter
        // to ignore earth acceleration
        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        float acceleration = linear_acceleration[0] + linear_acceleration[1] + linear_acceleration[2];
        float current_sign = Math.signum(acceleration);

        if (current_sign == last_sign) {
            // the maximum is not reached yet, keep on waiting
            return;
        }

        if (!isSignificantValue(acceleration)) {
            // not significant (acceleration delta is too small)
            return;
        }

        float acceleration_diff = Math.abs(last_extrema[current_sign < 0 ? 1 : 0] /* the opposite */ - acceleration);
        if (!isAlmostAsLargeAsPreviousOne(acceleration_diff)) {
            if (BuildConfig.DEBUG) Logger.log("Not as large as previous");
            last_acceleration_diff = acceleration_diff;
            return;
        }

        if (!wasPreviousLargeEnough(acceleration_diff)) {
            if (BuildConfig.DEBUG) Logger.log("Previous not large enough");
            last_acceleration_diff = acceleration_diff;
            return;
        }

        long current_step_time = System.currentTimeMillis();

        if (last_step_time > 0) {
            long step_time_delta = current_step_time - last_step_time;

            // Ignore steps with more than 180bpm and less than 20bpm
            if (step_time_delta < 60 * 1000 / 180) {
                if (BuildConfig.DEBUG) Logger.log("Too fast.");
                return;
            } else if (step_time_delta > 60 * 1000 / 20) {
                if (BuildConfig.DEBUG) Logger.log("Too slow.");
                last_step_time = current_step_time;
                valid_steps = 0;
                return;
            }

            // check if this occurrence is regular with regard to the step frequency data
            if (!isRegularlyOverTime(step_time_delta)) {
                last_step_time = current_step_time;
                if (BuildConfig.DEBUG) Logger.log("Not regularly over time.");
                return;
            }
            last_step_time = current_step_time;

            // check if this occurrence is regular with regard to the acceleration data
            if (!isRegularlyOverAcceleration(acceleration_diff)) {
                last_acceleration_value = acceleration;
                last_acceleration_diff = acceleration_diff;
                if (BuildConfig.DEBUG)
                    Logger.log("Not regularly over acceleration" + Arrays.toString(mLastStepAccelerationDeltas));
                valid_steps = 0;
                return;
            }
            last_acceleration_value = acceleration;
            last_acceleration_diff = acceleration_diff;
            // okay, finally this has to be a step
            valid_steps++;
            if (BuildConfig.DEBUG)
                Logger.log("Detected step. Valid steps = " + valid_steps);
            // count it only if we got more than validStepsThreshold steps
            if (valid_steps > validStepsThreshold) {
                steps++;
                Intent localIntent = new Intent(BROADCAST_ACTION_STEPS_DETECTED)
                        // Add new step count
                        .putExtra(EXTENDED_DATA_TOTAL_STEPS, steps);
                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            }
        }

        last_step_time = current_step_time;
        last_acceleration_value = acceleration;
        last_acceleration_diff = acceleration_diff;
        last_sign = current_sign;
        last_extrema[current_sign < 0 ? 0 : 1] = acceleration;

    }

    private boolean isSignificantValue(float val) {
        return Math.abs(val) > accelerometerThreshold;
    }

    private boolean isAlmostAsLargeAsPreviousOne(float diff) {
        return diff > last_acceleration_diff * 0.5;
    }

    private boolean wasPreviousLargeEnough(float diff) {
        return last_acceleration_diff > diff / 3;
    }

    private boolean isRegularlyOverTime(long delta) {
        mLastStepDeltas[mLastStepDeltasIndex] = delta;
        mLastStepDeltasIndex = (mLastStepDeltasIndex + 1) % mLastStepDeltas.length;

        int numIrregularValues = 0;
        for (long mLastStepDelta : mLastStepDeltas) {
            if (Math.abs(mLastStepDelta - delta) > 200) {
                numIrregularValues++;
                break;
            }
        }

        return numIrregularValues < 1;//mLastStepDeltas.length*0.2;
    }

    private boolean isRegularlyOverAcceleration(float diff) {
        mLastStepAccelerationDeltas[mLastStepAccelerationDeltasIndex] = diff;
        mLastStepAccelerationDeltasIndex = (mLastStepAccelerationDeltasIndex + 1) % mLastStepAccelerationDeltas.length;
        int numIrregularAccelerationValues = 0;
        for (float mLastStepAccelerationDelta : mLastStepAccelerationDeltas) {
            if (Math.abs(mLastStepAccelerationDelta - last_acceleration_diff) > 0.5) {
                numIrregularAccelerationValues++;
                break;
            }
        }
        return numIrregularAccelerationValues < mLastStepAccelerationDeltas.length * 0.2;
    }
}
