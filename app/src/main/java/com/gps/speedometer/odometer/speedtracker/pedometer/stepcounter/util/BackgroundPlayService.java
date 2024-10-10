package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.Observer;

import com.github.anastr.speedviewlib.Speedometer;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.callback.Callback;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.Speedo;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.PedometerActivity;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.SpeedometerActivity;

import java.security.Permission;

import timber.log.Timber;

public class BackgroundPlayService extends LifecycleService implements CurrentLocationOverlay.LocationResultListener {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private String unitMain = "km";
    private CurrentLocationOverlay currentLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.e("service started");
    }


    @SuppressLint("InflateParams")
    public void floatingWidget(Intent intent, String intent_type) {

        if (intent_type.equals("pedo")) {

            mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget_pedo, null);
            Timber.e("view inflated Pedo");
            //Add the view to the window.
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    /*WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,*/
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            //Specify the view position
            params.gravity = Gravity.TOP | Gravity.START;       //Initially view will be added to top-left corner
            params.x = 0;
            params.y = 100;

            //Add the view to the window
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (mWindowManager != null) {
                mWindowManager.addView(mFloatingView, params);
            }

            //The root element of the expanded view layout
//        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);
            //Set the close button

            //Drag and move floating view using user's touch action.
            mFloatingView.findViewById(R.id.pedo_view).setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;


                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            //remember the initial position.
                            initialX = params.x;
                            initialY = params.y;
                            //get the touch location
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            int Xdiff = (int) (event.getRawX() - initialTouchX);
                            int Ydiff = (int) (event.getRawY() - initialTouchY);


                            //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                            //So that is click event.
                            if (Xdiff < 10 && Ydiff < 10) {

                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.

                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            //Calculate the X and Y coordinates of the view.
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);


                            //Update the layout with new X & Y coordinate
                            mWindowManager.updateViewLayout(mFloatingView, params);
                            return true;
                    }
                    return false;
                }
            });

            mFloatingView.findViewById(R.id.close_pedo_service).setOnClickListener(v -> stopSelf());
            mFloatingView.findViewById(R.id.open_peod_activity).setOnClickListener(v -> {
                startActivity(new Intent(this, PedometerActivity.class));
                stopSelf();
            });

            TextView pipSteps = mFloatingView.findViewById(R.id.pip_steps);
            AppUtils.INSTANCE.getDefaultPreferences(this)
                    .registerOnSharedPreferenceChangeListener(
                            (sharedPreferences, key) ->
                                    pipSteps.setText(String.valueOf(sharedPreferences.getInt("pedo_service_value", 0))));

            pipSteps.setText(String.valueOf(AppUtils.INSTANCE
                    .getDefaultPreferences(BackgroundPlayService.this)
                    .getInt("pedo_service_value", 0)));

        } else {
            mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget_speedo, null);
            Timber.e("view inflated Speedo");

            //Add the view to the window.
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    /*WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,*/
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            //Specify the view position
            params.gravity = Gravity.TOP | Gravity.START;       //Initially view will be added to top-left corner
            params.x = 0;
            params.y = 100;

            //Add the view to the window
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (mWindowManager != null) {
                mWindowManager.addView(mFloatingView, params);
            }

            //The root element of the expanded view layout
//        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);
            //Set the close button

            //Drag and move floating view using user's touch action.
            mFloatingView.findViewById(R.id.pedo_view).setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;


                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            //remember the initial position.
                            initialX = params.x;
                            initialY = params.y;
                            //get the touch location
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            int Xdiff = (int) (event.getRawX() - initialTouchX);
                            int Ydiff = (int) (event.getRawY() - initialTouchY);


                            //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                            //So that is click event.
                            if (Xdiff < 10 && Ydiff < 10) {

                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.

                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            //Calculate the X and Y coordinates of the view.
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);


                            //Update the layout with new X & Y coordinate
                            mWindowManager.updateViewLayout(mFloatingView, params);
                            return true;
                    }
                    return false;
                }
            });

            mFloatingView.findViewById(R.id.close_pedo_service).setOnClickListener(v -> stopSelf());
            mFloatingView.findViewById(R.id.open_peod_activity).setOnClickListener(v -> {
                startActivity(new Intent(this, SpeedometerActivity.class));
                stopSelf();
            });

            currentLocation = new CurrentLocationOverlay(this);
            if (currentLocation != null && currentLocation.isGPSEnabled(this)
            && AppUtils.INSTANCE.getDefaultPreferences(this).getBoolean("speedo_overlay", false))
                currentLocation.getLocation(this);

            /*TextView pipSpeedo = mFloatingView.findViewById(R.id.pip_speed_value);
            AppUtils.INSTANCE.getDefaultPreferences(this)
                    .registerOnSharedPreferenceChangeListener(
                            (sharedPreferences, key) ->
                                    pipSpeedo.setText(String.valueOf(sharedPreferences.getInt("pedo_service_value", 0))));

            pipSpeedo.setText(String.valueOf(AppUtils.INSTANCE
                    .getDefaultPreferences(BackgroundPlayService.this)
                    .getInt("pedo_service_value", 0)));



            Observer<Location> speedObserver = this::getSpeed;
            Observer<Speedo> meterObserver = it -> {
                unitMain = it.getUnit();
            };*/

            //Callback.INSTANCE.getLocationData().observe(this, speedObserver);
            //Callback.INSTANCE.getMeterValue1().observe(this, meterObserver);

        }
    }

    private void getSpeed(Location location) {
        switch(AppUtils.INSTANCE.getUnit()) {
            case "km":
                ((TextView)mFloatingView.findViewById(R.id.pip_speed_value))
                        .setText(String.valueOf((int)(
                                AppUtils.INSTANCE.roundTwoDecimal(
                                        (location.getSpeed() * 3600) / 1000)))
                        );
                ((TextView)mFloatingView.findViewById(R.id.pip_unit)).setText(R.string.km_h_c);
                break;
            case "mph":
                ((TextView)mFloatingView.findViewById(R.id.pip_speed_value))
                        .setText(String.valueOf((int)(
                                AppUtils.INSTANCE.roundTwoDecimal(
                                        (location.getSpeed() * 2.2369))))
                        );
                ((TextView)mFloatingView.findViewById(R.id.pip_unit)).setText(R.string.mph_c);
                break;
            case "knot":
                ((TextView)mFloatingView.findViewById(R.id.pip_speed_value))
                        .setText(String.valueOf((int)(
                                AppUtils.INSTANCE
                                        .roundTwoDecimal(location.getSpeed() * 1.94384)))
                        );
                ((TextView)mFloatingView.findViewById(R.id.pip_unit)).setText(R.string.knot_c);
                break;
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && intent.getAction().equals("speedo")) {
            floatingWidget(intent, "speedo");
        } else {
            floatingWidget(intent, "pedo");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("lifecycle", "serviceondestroycalled");
        if (mFloatingView != null) {
            mWindowManager.removeView(mFloatingView);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        Log.d("lifecycle", "binded");
        return null;
    }

    @Override
    public void gotLocation(Location location) {
        getSpeed(location);
    }
}