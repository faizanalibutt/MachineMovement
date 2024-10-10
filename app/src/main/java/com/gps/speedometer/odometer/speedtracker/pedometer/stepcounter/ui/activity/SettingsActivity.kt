package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import com.dev.bytes.adsmanager.BannerPlacements
import com.dev.bytes.adsmanager.TinyDB
import com.dev.bytes.adsmanager.billing.purchaseRemoveAds
import com.dev.bytes.adsmanager.loadBannerAd
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.PictureInPictureActivity
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.SensorListener
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app.App
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Utility
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : Activity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        nav_back.setOnClickListener {
            finish()
        }

        if (isStepSensorAvailable(false))
            auto_count_switch.isChecked = AppUtils.getDefaultPreferences(
                this
            ).getString("pedo_state", null) == "stop"

        auto_count_switch.setOnCheckedChangeListener { buttonView, isChecked ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val permissions = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
                val rationale =
                    getString(R.string.text_recognition_permission)
                val options = Permissions.Options().setRationaleDialogTitle("Info")
                    .setSettingsDialogTitle(getString(R.string.text_warning))

                Permissions.check(this, permissions, rationale, options,
                    object : PermissionHandler() {
                        override fun onGranted() {
                            if (isStepSensorAvailable(true)) {
                                if (isChecked) {
                                    AppUtils.getDefaultPreferences(
                                        this@SettingsActivity
                                    ).edit().putString("pedo_state", "stop").apply()
                                    startService(
                                        Intent(
                                            this@SettingsActivity,
                                            SensorListener::class.java
                                        )
                                    )
                                } else {
                                    AppUtils.getDefaultPreferences(
                                        this@SettingsActivity
                                    ).edit().putString("pedo_state", "resume").apply()
                                    stopService(
                                        Intent(
                                            this@SettingsActivity,
                                            SensorListener::class.java
                                        )
                                    )
                                }
                            }
                        }

                        override fun onDenied(
                            context: Context?,
                            deniedPermissions: ArrayList<String>?
                        ) {
                            auto_count_switch.isChecked = false
                        }
                    })
            } else {
                if (isStepSensorAvailable(true)) {
                    if (isChecked) {
                        AppUtils.getDefaultPreferences(
                            this@SettingsActivity
                        ).edit().putString("pedo_state", "stop").apply()
                        startService(
                            Intent(
                                this@SettingsActivity,
                                SensorListener::class.java
                            )
                        )
                    } else {
                        AppUtils.getDefaultPreferences(
                            this@SettingsActivity
                        ).edit().putString("pedo_state", "resume").apply()
                        stopService(
                            Intent(
                                this@SettingsActivity,
                                SensorListener::class.java
                            )
                        )
                    }
                }
            }

        }

        app_widget_switch.isChecked =
            AppUtils.getDefaultPreferences(this@SettingsActivity)
                .getBoolean("app_widget", true)

        app_widget_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)
                AppUtils.getDefaultPreferences(
                    this@SettingsActivity
                ).edit().putBoolean("app_widget", true).apply()
            else
                AppUtils.getDefaultPreferences(
                    this@SettingsActivity
                ).edit().putBoolean("app_widget", false).apply()
        }

        change_language.setOnClickListener(this)
        privacy_policy.setOnClickListener(this)
        rate_us.setOnClickListener(this)
        share.setOnClickListener(this)
        premium.setOnClickListener(this)
        premium_services.setOnClickListener(this)

        ad_container_settings.loadBannerAd(BannerPlacements.BANNER_AD)

        if (TinyDB.getInstance(this).getBoolean(getString(com.dev.bytes.R.string.is_premium))) {
            premium_services.visibility = View.GONE
            premium_group.visibility = View.GONE
        } else
            AppUtils.animateProButton(this, premium_services)

        setLanguageName()
    }

    private fun setLanguageName() {
        language_name.text = Utility.setLanguageLocale()
    }

    private fun isStepSensorAvailable(check: Boolean): Boolean {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var sensor =
            sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (sensor == null) {
                if (check) {
                    AlertDialog.Builder(this).setTitle(R.string.no_sensor)
                        .setMessage(R.string.no_sensor_explain)
                        .setOnDismissListener { it.dismiss() }
                        .setPositiveButton(
                            R.string.okay
                        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                        .create()
                        .show()
                    if (auto_count_switch.isChecked)
                        auto_count_switch.isChecked = false
                    auto_count_switch.isEnabled = false
                }
                return false
            }
            return true
        }
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.change_language -> {
                Utility.showLanguageDialog(this@SettingsActivity)
            }
            R.id.privacy_policy -> {
                val url = getString(R.string.privacy_policy_link_text)
                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this, Uri.parse(url))
            }
            R.id.rate_us -> {
                dialog = null
                dialog = showRateExitDialogue(this@SettingsActivity, true)
                dialog!!.show()
            }
            R.id.premium -> App.bp?.purchaseRemoveAds(this)
            R.id.share -> {
                shareIntent()
            }
            R.id.premium_services -> App.bp?.purchaseRemoveAds(this)
        }
    }

    private fun shareIntent() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        val shareSubText = this.resources.getString(R.string.great_app)
        // TODO: 7/24/2020 get app link ""DONE""
        val shareBodyText =
            this.resources.getString(R.string.share_desc) + " https://play.google.com/store/apps/developer?id=" + packageName
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubText)
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText)
        startActivity(
            Intent.createChooser(
                shareIntent,
                this.resources.getString(R.string.share_with)
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!(App.bp!!.handleActivityResult(requestCode, resultCode, data)))
            super.onActivityResult(requestCode, resultCode, data)
    }
}