package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.fragment.app.FragmentActivity;

import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.PedometerActivity;

public class CurrentLocationOverlay implements LocationListener {

    private LocationResultListener locationResultListener;
    private final Context mContext;

    /* location manager settings */
    private LocationManager mLocationManager;

    public CurrentLocationOverlay(Context context) {
        this.mContext = context;
        if (mContext != null) {
            mLocationManager = (LocationManager) mContext.getApplicationContext()
                    .getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
    }

    public void getLocation(LocationResultListener result) {
        getLocation(result, true);
    }

    public boolean getLocation(LocationResultListener result, boolean isFirst) {

        locationResultListener = result;
        if (locationResultListener != null) {
            if (isGPSEnabled(mContext)) {
                requestLocation();
            } else if (isFirst) {
                //gpsLocationSetting();
            }
        }


        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        locationResultListener.gotLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public interface LocationResultListener {
        void gotLocation(Location location);
    }

    private void gpsLocationSetting() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.mesg_locationDisabledSelfHotspot)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_locationSettings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((FragmentActivity) mContext).startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PedometerActivity.LOCATION_SERVICE_RESULT);
                    }
                })
                .show();
    }

    public boolean isGPSEnabled(Context mContext) {
        return mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    @SuppressLint("MissingPermission")
    private void requestLocation() {
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, this);
    }

}
