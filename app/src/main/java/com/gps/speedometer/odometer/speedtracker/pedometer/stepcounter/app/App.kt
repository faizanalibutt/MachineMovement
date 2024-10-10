package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.RemoteConfigUtils


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        RemoteConfigUtils.createConfigSettings()?.fetchAndActivate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }


}