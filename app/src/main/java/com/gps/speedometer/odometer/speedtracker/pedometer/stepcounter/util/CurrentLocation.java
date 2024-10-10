package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class CurrentLocation {

    private LocationResultListener locationResultListener;
    private LocationRequest mLocationRequest;
    private Context context;
    public static final int REQUEST_LOCATION = 101;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationSettingsRequest.Builder locationSettingsRequest;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public CurrentLocation(Context context) {
        this.context = context;
        fusedLocationProviderClient = new FusedLocationProviderClient(context);
    }

    public boolean getLocation(LocationResultListener result) {

        locationResultListener = result;
        if (isGPSEnabled(context)) {
           /* fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && locationResultListener != null) {
                    locationResultListener.gotLocation(location);
                }else{
                    requestLocation();
                }
            });*/
            requestLocation();
        } else {
            gpsLocationSetting();
        }


        return true;
    }

    public interface LocationResultListener {
        void gotLocation(Location location);
    }

    private void gpsLocationSetting() {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(context).checkLocationSettings(locationSettingsRequest.build());


        task.addOnSuccessListener((Activity) context, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
        });

        task.addOnFailureListener((Activity) context, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult((Activity) context,
                            REQUEST_LOCATION);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    private boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            return false;
        }
    }


    @SuppressLint("MissingPermission")
    private void requestLocation() {

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        /*if (NetworkUtils.isOnline(context))
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        else
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);*/
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);


        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResults) {
                if (locationResults == null) {
                    return;
                }
                List<Location> locationList = locationResults.getLocations();
                if(locationList.size() > 0){
                    Location location = locationList.get(locationList.size() - 1);
                    if (location != null && locationResultListener != null) {
                        locationResultListener.gotLocation(location);
                    }
                }
            }

        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }


    public void removeFusedLocationClient(){

        if(fusedLocationProviderClient != null && locationCallback != null){
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

    }


}