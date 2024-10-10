package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Utility

open class Activity : AppCompatActivity() {

    private val TAG = "BaseActivity"

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")
            Utility.resetActivityTitle(this)
        } catch (exp: ConcurrentModificationException) {}
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode: Int = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    protected var dialog: AlertDialog? = null
}