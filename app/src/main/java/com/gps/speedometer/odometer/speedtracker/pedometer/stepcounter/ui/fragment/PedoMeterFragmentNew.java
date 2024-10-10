/*
 * Copyright 2014 Thomas Hoffmann
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
package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dev.bytes.adsmanager.ADUnitPlacements;
import com.dev.bytes.adsmanager.NativeAdsManagerKt;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.BuildConfig;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.Database;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.SensorListener;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.PedometerActivity;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Logger;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.TimeUtils;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Util;
import com.natasa.progressviews.CircleSegmentBar;
import com.natasa.progressviews.utils.ProgressStartPoint;

import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class PedoMeterFragmentNew extends Fragment implements SensorEventListener {

    private TextView stepsView, totalView, miles;
    private PieModel sliceGoal, sliceCurrent;
    //    private PieChart pg;
    private View start_btn;
    ImageView step_btn_img;
    TextView step_btn_txt, timeValue;
    private View mView;
    private CircleSegmentBar segmentBar;
    private String pedoState;

    private int todayOffset, total_start, goal, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

//    SpeedViewModel mViewModel = null;

    private void initSegmentProgressBar() {
        segmentBar = mView.findViewById(R.id.pedo_process_graph1);
        segmentBar.setCircleViewPadding(1);
        segmentBar.setSegmentWidth(1);
        segmentBar.setStartPositionInDegrees(ProgressStartPoint.BOTTOM);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_pedometer_new, null);

        mView = v;

        stepsView = v.findViewById(R.id.steps);
        miles = v.findViewById(R.id.distance_value);
        timeValue = v.findViewById(R.id.time_value);

        start_btn = v.findViewById(R.id.start_btn);
        step_btn_img = v.findViewById(R.id.step_btn_img);
        step_btn_txt = v.findViewById(R.id.step_btn_txt);

        // slice for the steps taken today
        sliceCurrent = new PieModel("", 0, Color.parseColor("#99CC00"));

        // slice for the "missing" steps until reaching the goal
        sliceGoal = new PieModel("", Fragment_Settings.DEFAULT_GOAL, Color.parseColor("#CC0000"));

        initSegmentProgressBar();


        start_btn.setOnClickListener(v1 -> {

            if (step_btn_txt.getText() == v.getContext().getString(R.string.text_resume)) {

                AppUtils.INSTANCE.getDefaultPreferences
                        (
                                (AppCompatActivity) requireActivity()
                        ).edit().putString("pedo_state", "stop").apply();

                startService(v, true);
                setUpListener(true);
                startPedometer();
                if (getActivity() instanceof PedometerActivity)
                    ((PedometerActivity) getActivity()).showStartStopInter();

                step_btn_txt.setText(v.getContext().getString(R.string.text_stop));
                step_btn_img.setVisibility(View.GONE);
                start_btn.setBackground(v.getContext().getDrawable(R.drawable.background_stop_btn));

            } else if (step_btn_txt.getText() == v.getContext().getString(R.string.text_stop)) {

                AppUtils.INSTANCE.getDefaultPreferences
                        (
                                (AppCompatActivity) requireActivity()
                        ).edit().putString("pedo_state", "resume").apply();

                startService(v, false);
                setUpListener(false);
                if (getActivity() instanceof PedometerActivity)
                    ((PedometerActivity) getActivity()).showStartStopInter();

                step_btn_txt.setText(v.getContext().getString(R.string.text_resume));
                step_btn_img.setVisibility(View.VISIBLE);
                start_btn.setBackground(v.getContext().getDrawable(R.drawable.background_start_btn));

            } else {

                AppUtils.INSTANCE.getDefaultPreferences
                        (
                                (AppCompatActivity) requireActivity()
                        ).edit().putString("pedo_state", "stop").apply();

                startService(v, true);
                setUpListener(true);
                startPedometer();
                if (getActivity() instanceof PedometerActivity)
                    ((PedometerActivity) getActivity()).showStartStopInter();

                step_btn_txt.setText(v.getContext().getString(R.string.text_stop));
                step_btn_img.setVisibility(View.GONE);
                start_btn.setBackground(v.getContext().getDrawable(R.drawable.background_stop_btn));

            }

        });

        pedoState = AppUtils.INSTANCE.getDefaultPreferences(
                (AppCompatActivity) requireActivity()
        ).getString("pedo_state", null);

        if (pedoState != null && pedoState.equals("stop")) {

            setUpListener(true);
            startService(v, true);
            startPedometer();

            step_btn_txt.setText(v.getContext().getString(R.string.text_stop));
            step_btn_img.setVisibility(View.GONE);
            start_btn.setBackground(v.getContext().getDrawable(R.drawable.background_stop_btn));

        } else if (pedoState != null && pedoState.equals("resume")) {

            setUpListener(false);
            startService(v, false);

            step_btn_txt.setText(v.getContext().getString(R.string.text_resume));
            step_btn_img.setVisibility(View.VISIBLE);
            start_btn.setBackground(v.getContext().getDrawable(R.drawable.background_start_btn));

        }

        step_btn_txt.setSelected(true);
        timeValue.setSelected(true);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isStepSensorAvailable();
        FrameLayout ad_container = view.findViewById(R.id.ad_container_pedo);
        NativeAdsManagerKt.loadNativeAd(view.getContext(), ad_container,
                R.layout.ad_unified_common,
                ADUnitPlacements.COMMON_NATIVE_AD, true, null,
                null, null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        pedoState = AppUtils.INSTANCE.getDefaultPreferences(
                (AppCompatActivity) requireActivity()
        ).getString("pedo_state", null);
        if (pedoState != null && pedoState.equals("stop")) {
            setUpListener(true);
            if (SensorListener.SENSOR_ACCEL) {
                todayOffset = Database.getInstance(mView.getContext()).getSteps(Util.getToday());

                SharedPreferences prefs =
                        mView.getContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

                goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL);
                since_boot = Database.getInstance(mView.getContext()).getCurrentSteps();
                int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

                since_boot -= pauseDifference;
                updatePie();
            }
            registerReceivers();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pedoState = AppUtils.INSTANCE.getDefaultPreferences(
                (AppCompatActivity) requireActivity()
        ).getString("pedo_state", null);
        if (pedoState != null && pedoState.equals("stop")) {
            setUpListener(false);
            Database db = Database.getInstance(getActivity());
            db.saveCurrentSteps(since_boot);
            db.close();
            unregisterReceivers();
        }
    }

    private void startService(View v, boolean start) {
        if (start)
            v.getContext().startService(new Intent(getActivity(), SensorListener.class));
        else {
            v.getContext().stopService(new Intent(getActivity(), SensorListener.class));
        }
    }

    public void setUpListener(boolean option) {
        if (option) {
            SensorManager sm = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            /*if (sensor == null) {
                sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);*/
            if (sensor == null) {
                sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (sensor == null)
                    // dialog
                    new AlertDialog.Builder(getActivity()).setTitle(R.string.no_sensor)
                            .setMessage(R.string.no_sensor_explain)
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(final DialogInterface dialogInterface) {
                                    requireActivity().finish();
                                }
                            }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).create().show();

            }
            //}
            if (sensor != null)
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        } else {
            try {
                SensorManager sm =
                        (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
                sm.unregisterListener(this);
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
        }
    }

    private void isStepSensorAvailable() {
        SensorManager sm = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) {
            /*sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (sensor == null) {*/
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensor == null)
                new AlertDialog.Builder(getActivity()).setTitle(R.string.no_sensor)
                        .setMessage(R.string.no_sensor_explain)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(final DialogInterface dialogInterface) {
                                requireActivity().finish();
                            }
                        }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
            //}
        }
    }

    private void startPedometer() {

        Database db = Database.getInstance(getActivity());

        //if (BuildConfig.DEBUG) db.logState();
        // read todays offset
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL);
        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        since_boot -= pauseDifference;
        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();

        stepsDistanceChanged();
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private void stepsDistanceChanged() {
        ((TextView) mView.findViewById(R.id.unit)).setText(getString(R.string.text_steps));
        updatePie();
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // won't happen
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            goToAccelerometer(event);
        } /*else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
            goToStepCounter(event);*/ else
            goToStepCounter(event);

    }

    private void goToStepCounter(SensorEvent event) {
        if (BuildConfig.DEBUG) Logger.log(
                "UI - sensorChanged | todayOffset: " + todayOffset + " since boot: " +
                        event.values[0]);
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) event.values[0];
            Database db = Database.getInstance(getActivity());
            db.insertNewDay(Util.getToday(), (int) event.values[0]);
            db.close();
        }
        since_boot = (int) event.values[0];
        updatePie();
    }

    private void goToStepDetector(SensorEvent event) {

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

        float acceleration_diff = Math.abs(last_extrema[current_sign < 0 ? 1 : 0]  /*the opposite*/ - acceleration);
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
            if (valid_steps == validStepsThreshold) {
                Logger.log("valid steps == threshold");
                if (todayOffset == Integer.MIN_VALUE) {
                    Logger.log("came to set new day");
                    todayOffset = 0;
                    Database db = Database.getInstance(getActivity());
                    db.insertNewDay(Util.getToday(), valid_steps);
                    db.close();
                    since_boot = 0;
                }
                //since_boot = valid_steps;
                //updatePie();
            }
            valid_steps++;
            if (BuildConfig.DEBUG)
                Logger.log("Detected step. Valid steps = " + valid_steps);
            // count it only if we got more than validStepsThreshold steps
            if (valid_steps > validStepsThreshold) {
                since_boot++;
                updatePie();
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

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private void updatePie() {
        if (BuildConfig.DEBUG) Logger.log("UI - update steps: " + since_boot);
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + since_boot, 0);
        sliceCurrent.setValue(steps_today);
        segmentBar.setProgress((float) steps_today);
        if (goal - steps_today > 0) {
            // goal not reached yet
            /*if (pg.getData().size() == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pg.addPieSlice(sliceGoal);
            }*/
            sliceGoal.setValue(goal - steps_today);
        } else {
            // goal reached
            /*pg.clearChart();
            pg.addPieSlice(sliceCurrent);*/
        }
//        pg.update();

        stepsView.setText("" + steps_today);
        SharedPreferences prefs =
                mView.getContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        float stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE);
        float distance_today = steps_today * stepsize;
        if (Objects.equals(prefs.getString
                ("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT), "cm")) {
            distance_today /= 160934;
        } else {
            distance_today /= 5280;
        }
        miles.setText("" + AppUtils.INSTANCE.roundTwoDecimal(distance_today));
        AppUtils.INSTANCE.getDefaultPreferences(mView.getContext())
                .registerOnSharedPreferenceChangeListener(
                        (sharedPreferences, key) -> {
                            ((TextView)mView.findViewById(R.id.pip_steps)).setText(String.valueOf(sharedPreferences.getInt("pedo_service_value", 0)));
        });
        ((TextView)mView.findViewById(R.id.pip_steps)).setText(String.format("%s", steps_today));
        // TODO: 7/15/2020 increase step count to 150
        timeValue.setText(TimeUtils.INSTANCE.getDurationSpeedo(BuildConfig.DEBUG ?
                (steps_today / 10) * 60000 : (steps_today / 150) * 60000));
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver();

    public class BroadcastReceiver extends android.content.BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Logger.log("Received intent which is null.");
                return;
            }
            switch (intent.getAction()) {
                case SensorListener.BROADCAST_ACTION_STEPS_DETECTED:
                    //todayOffset = 0;
                    since_boot = 0;
                    since_boot = intent.getIntExtra(SensorListener.EXTENDED_DATA_TOTAL_STEPS, 0);
                    updatePie();
                    break;
                default:
            }
        }
    }

    private void registerReceivers() {
        // subscribe to onStepsSaved and onStepsDetected broadcasts and onSpeedChanged
        IntentFilter filterRefreshUpdate = new IntentFilter();
        filterRefreshUpdate.addAction(SensorListener.BROADCAST_ACTION_STEPS_DETECTED);
        LocalBroadcastManager.getInstance(mView.getContext()).registerReceiver(broadcastReceiver, filterRefreshUpdate);
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(mView.getContext()).unregisterReceiver(broadcastReceiver);
    }


    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            mView.findViewById(R.id.pip_mode).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.app_mode).setVisibility(View.GONE);
        } else {
            mView.findViewById(R.id.pip_mode).setVisibility(View.GONE);
            mView.findViewById(R.id.app_mode).setVisibility(View.VISIBLE);
        }
    }

}
