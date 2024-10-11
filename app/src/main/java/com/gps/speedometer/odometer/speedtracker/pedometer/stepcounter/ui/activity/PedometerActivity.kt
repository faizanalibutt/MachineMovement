package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app.App
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.databinding.ActivityPedometerBinding
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.ViewPagerAdapter
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment.PedoMeterFragmentNew
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils.getDefaultPreferences
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.BackgroundPlayService
import java.util.*

class PedometerActivity : Activity() {

    private var pedoMeterWorking: Boolean = false
    private var isStartStopShown: Boolean = false
    private var isOverlay = false


    private lateinit var binding: ActivityPedometerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedometerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stopService(Intent(this, BackgroundPlayService::class.java))

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(PedoMeterFragmentNew(), getString(R.string.text_today))


        binding.apply {
            viewPager.adapter = adapter
            tabView.setupWithViewPager(viewPager)

            navBack.setOnClickListener {
                onBackPressed()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                if (ContextCompat.checkSelfPermission(
                        this@PedometerActivity,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    pedoMeterWorking = true
                } else {
                    finish()
                }
            }
            textView.isSelected = true
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)
            ) {
                Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
                isOverlay = false
            } else {
                startService(Intent(this, BackgroundPlayService::class.java).setAction("pedo"))
                isOverlay = true

            }
        }
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        openPictureDialog()
    }

    private fun openPictureDialog() {
        if (getDefaultPreferences(this)
                .getBoolean("app_widget", true) && getDefaultPreferences(this)
                .getString("pedo_state", null) == "stop"
        )
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                if (!isInPictureInPictureMode
                    && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                ) {
                    val d: Display = windowManager.defaultDisplay
                    val p = Point()
                    d.getSize(p)
                    val width: Int = p.x
                    val height: Int = p.y

                    val ratio = Rational(width, height)
                    val pip_Builder: PictureInPictureParams.Builder =
                        PictureInPictureParams.Builder()
                    pip_Builder.setAspectRatio(ratio).build()
                    enterPictureInPictureMode(pip_Builder.build())
                } else
                    Toast.makeText(
                        this,
                        "Your device does not support picture-in-picture mode.",
                        Toast.LENGTH_SHORT
                    ).show()
            } else {
                if (!checkServiceRunning(BackgroundPlayService::class.java)) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
                        && Settings.canDrawOverlays(this)
                    ) {
                        startService(
                            Intent(
                                this,
                                BackgroundPlayService::class.java
                            ).setAction("pedo")
                        )
                        isOverlay = true
                    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && !Settings.canDrawOverlays(
                            this
                        )
                    )

                    else {
                        startService(
                            Intent(
                                this,
                                BackgroundPlayService::class.java
                            ).setAction("pedo")
                        )
                        isOverlay = true
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlay = false
        if (checkServiceRunning())
            stopService(Intent(this, BackgroundPlayService::class.java))
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()

    }

    fun checkServiceRunning(serviceClass: Class<*> = BackgroundPlayService::class.java): Boolean {
        val manager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        var ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1010

        @JvmField
        var LOCATION_SERVICE_RESULT: Int = 2
    }

}