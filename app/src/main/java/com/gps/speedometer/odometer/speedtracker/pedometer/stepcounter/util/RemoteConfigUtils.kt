package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util

import com.dev.bytes.adsmanager.InterDelayTimer.INTERSTITIAL_DELAY_TIME
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigUtils {


        private const val FETCH_TIME_INTERVAL = 1   *60   *60L   //  hours   minutes    seconds

        fun createConfigSettings(): FirebaseRemoteConfig? {
            var remoteConfig: FirebaseRemoteConfig? = null
            kotlin.runCatching {
                remoteConfig = FirebaseRemoteConfig.getInstance()
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    //.setDeveloperModeEnabled(!BuildConfig.DEBUG)
                    .setMinimumFetchIntervalInSeconds(FETCH_TIME_INTERVAL)
                    .build()
                remoteConfig?.setConfigSettingsAsync(configSettings)
                remoteConfig?.setDefaultsAsync(
                    mapOf(
//                    IS_BOTTOM_BANNER_SHOW to false,
                        INTERSTITIAL_DELAY_TIME to 30
//                    IS_SPLASH_NATIVE to false,
//                    IS_SPLASH_INTER to false
                    )
                )
                return remoteConfig
            }.onFailure {
                return remoteConfig
            }
            return remoteConfig
        }


}