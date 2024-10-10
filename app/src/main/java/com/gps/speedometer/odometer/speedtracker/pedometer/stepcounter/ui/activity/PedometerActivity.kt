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
import androidx.appcompat.app.AppCompatActivity
import com.dev.bytes.adsmanager.ADUnitPlacements
import com.dev.bytes.adsmanager.InterAdPair
import com.dev.bytes.adsmanager.TinyDB
import com.dev.bytes.adsmanager.billing.purchaseRemoveAds
import com.dev.bytes.adsmanager.loadInterstitialAd
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app.App
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.ViewPagerAdapter
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment.PedoMeterFragmentNew
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment.ReportFragment
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils.getDefaultPreferences
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.BackgroundPlayService
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import kotlinx.android.synthetic.main.activity_pedometer.*
import timber.log.Timber
import java.util.*

class PedometerActivity : Activity() {

    private var pedoMeterWorking: Boolean = false
    var startStopInterstitialAd: InterAdPair? = null
    var backInterstitialAd: InterAdPair? = null
    private var isStartStopShown: Boolean = false
    private var isOverlay = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedometer)

        stopService(Intent(this, BackgroundPlayService::class.java))

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(PedoMeterFragmentNew(), getString(R.string.text_today))
        adapter.addFragment(ReportFragment(), getString(R.string.text_report))
        viewPager.adapter = adapter
        tabView.setupWithViewPager(viewPager)

        premium_services.setOnClickListener {
            App.bp?.purchaseRemoveAds(this)
        }

        nav_back.setOnClickListener {
            onBackPressed()
        }

        if (TinyDB.getInstance(this).getBoolean(getString(com.dev.bytes.R.string.is_premium)))
            premium_services.visibility = View.GONE
        else
            AppUtils.animateProButton(this, premium_services)

        loadInterstitialAd(
            ADUnitPlacements.PEDO_START_STOP_INTERSTITIAL,
            onLoaded = { startStopInterstitialAd = it },
            reloadOnClosed = true
        )

        loadInterstitialAd(
            ADUnitPlacements.PEDO_BACK_INTERSTITIAL,
            onLoaded = { backInterstitialAd = it },
            onClosed = { finish() }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissions = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
            val rationale =
                getString(R.string.text_recognition_permission)
            val options = Permissions.Options().setRationaleDialogTitle("Info")
                .setSettingsDialogTitle(getString(R.string.text_warning))

            Permissions.check(
                this,
                permissions,
                rationale,
                options,
                object : PermissionHandler() {
                    override fun onGranted() {
                        pedoMeterWorking = true
                    }

                    override fun onDenied(
                        context: Context?,
                        deniedPermissions: ArrayList<String>?
                    ) {
                        super.onDenied(context, deniedPermissions)
                        finish()
                    }
                })
        }
        textView.isSelected = true
    }

    fun showStartStopInter() {
        startStopInterstitialAd?.apply {
            if (this.isLoaded()) this.showAd(this@PedometerActivity, true)
                .let { isStartStopShown = it }
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        showProcessDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((App.bp?.handleActivityResult(requestCode, resultCode, data)) != true)
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
                mOpenPermDialog?.dismiss()
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
                        showOpenPermDialog()
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

    var mOpenPermDialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showOpenPermDialog() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog))
        builder.setTitle(R.string.dialog_title_overlay)
        builder.setMessage(R.string.dialog_desc_overlay)
        builder.setPositiveButton(R.string.butn_start,
            DialogInterface.OnClickListener { dialogInterface, i ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
                isOverlay = false
                mOpenPermDialog?.dismiss()
            })
        builder.setNegativeButton(R.string.butn_cancel, null)
        if (mOpenPermDialog == null)
            mOpenPermDialog = builder.create()
        if (mOpenPermDialog != null && mOpenPermDialog?.isShowing == false)
            mOpenPermDialog?.show()
    }

    var mProcessDialog: AlertDialog? = null

    private fun showProcessDialog() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert))
        builder.setTitle(R.string.dialog_title_exit)
        builder.setMessage(R.string.dialog_desc_exit)
        builder.setPositiveButton(R.string.close,
            DialogInterface.OnClickListener { dialogInterface, i ->
                mProcessDialog?.dismiss()
                finish()
                backInterstitialAd?.apply {
                    if (this.isLoaded() && !isStartStopShown)
                        this.showAd(this@PedometerActivity)
                }
                if (!AppUtils.getBuildVersion())
                    stopService(Intent(this, BackgroundPlayService::class.java))
            })
        builder.setNegativeButton(R.string.butn_cancel, null)
        if (mProcessDialog == null)
            mProcessDialog = builder.create()
        if (mProcessDialog != null && mProcessDialog?.isShowing == false)
            mProcessDialog?.show()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            tabView.visibility = View.GONE
            app_bar_group.visibility = View.GONE
        } else {
            tabView.visibility = View.VISIBLE
            app_bar_group.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlay = false
        mOpenPermDialog = null
        mProcessDialog = null
        if (checkServiceRunning())
            stopService(Intent(this, BackgroundPlayService::class.java))
    }

    override fun onPause() {
        super.onPause()
        Timber.e("onPause Called")
    }

    override fun onStop() {
        super.onStop()
        Timber.e("onStop Called")
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