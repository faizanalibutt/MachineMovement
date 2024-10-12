package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.Contract;

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
        registerGnssStatusCallback();
    }

    public boolean getLocation(LocationResultListener result) {

        locationResultListener = result;
        if (isGPSEnabled(context)) {
            requestLocation();
        } else {
            gpsLocationSetting();
        }

        return true;
    }

    private void registerGnssStatusCallback() {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback);
        }
    }


    public interface LocationResultListener {
        void gotLocation(Location location);
        void getGpsStrength(String signalStrength);
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
        if (NetworkUtils.isOnline(context))
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        else
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);


        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResults) {
                if (locationResults == null) {
                    return;
                }
                List<Location> locationList = locationResults.getLocations();
                if(!locationList.isEmpty()){
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

    private final GnssStatus.Callback gnssStatusCallback = new GnssStatus.Callback() {
        @Override
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            int satelliteCount = status.getSatelliteCount();
            float totalSnr = 0;
            int validSatellites = 0;

            // Iterate through each satellite and calculate the average SNR
            for (int i = 0; i < satelliteCount; i++) {
                float snr = status.getCn0DbHz(i); // Signal-to-Noise Ratio (SNR)

                if (snr > 0) { // Ignore satellites with no signal
                    totalSnr += snr;
                    validSatellites++;
                }
            }

            if (validSatellites > 0) {
                float averageSnr = totalSnr / validSatellites; // Calculate average SNR
                String gpsStrength = getGpsStrength(averageSnr);
                locationResultListener.getGpsStrength(gpsStrength);
                // Display GPS signal strength
                Toast.makeText(context, "GPS Signal Strength: " +
                        gpsStrength + "\nAverage SNR: " + averageSnr, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "No valid GPS signals detected", Toast.LENGTH_LONG).show();
            }
        }
    };

    // Categorize GPS signal strength based on average SNR
    @NonNull
    @Contract(pure = true)
    private String getGpsStrength(float averageSnr) {
        if (averageSnr >= 30) {
            return "Strong";
        } else if (averageSnr >= 20) {
            return "Medium";
        } else {
            return "weak";
        }
    }

}