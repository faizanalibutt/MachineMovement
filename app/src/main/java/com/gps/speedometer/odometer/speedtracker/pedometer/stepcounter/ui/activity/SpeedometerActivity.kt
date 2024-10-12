package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayout
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.Database
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.callback.Callback
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.databinding.ActivitySpeedometerBinding
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.Distance
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.Speedo
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.ViewPagerAdapter
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.PedometerActivity.Companion.ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment.DigitalFragment
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.vm.SpeedViewModel
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils.getDefaultPreferences
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.BackgroundPlayService
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.CurrentLocation
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.TimeUtils
import java.util.Calendar
import kotlin.math.max

class SpeedometerActivity : Activity(), CurrentLocation.LocationResultListener {

    private var avgSpeedInDB: Double = 0.0
    private var maxSpeedInDB: Double = 0.0
    private var distanceInDB: Double = 0.0
    private var isStartStopShown: Boolean = false
    private var speed: Double = 0.0
    private var handler: Handler? = null
    private var updateTimerThread: Runnable? = null
    var totalTime: Long = 0
    var startTime: Long = 0
    var endTime: Long = 0
    var paused = false
    private var distance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var avgSpeed: Double = 0.0
    private var isStop: Boolean = false

    private var currentLocation: CurrentLocation? = null
    private var mCurrentLocation: Location? = null
    var lStart: Location? = null
    var lEnd: Location? = null
    var unitType = "km"

    var mViewModel: SpeedViewModel? = null
    var speedObj: Speedo? = null

    private var isOverlay = false

    private lateinit var binding: ActivitySpeedometerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedometerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!AppUtils.getBuildVersion())
            stopService(Intent(this, BackgroundPlayService::class.java))

        mViewModel = ViewModelProviders.of(this)
            .get(SpeedViewModel::class.java)//ViewModelProvider(this).get(SpeedViewModel::class.java)

        val adapter = ViewPagerAdapter(supportFragmentManager)

        adapter.addFragment(
            DigitalFragment(this@SpeedometerActivity),
            getString(R.string.text_digital)
        )

        binding.apply {
            viewPager.offscreenPageLimit = 2
            viewPager.adapter = adapter
            tabView.setupWithViewPager(viewPager)
            text4.isSelected = true
            actionBarText.isSelected = true

            navBack.setOnClickListener {
                onBackPressed()
            }

            currentLocation = CurrentLocation(this@SpeedometerActivity)

            startBtn.setOnClickListener(::startStopBtn)

            tabView.setupWithViewPager(viewPager)
            tabView.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> {
                            startBtnGroup.visibility = View.VISIBLE
                            actionBarText.text = getString(R.string.analog_meter)
                        }

                        1 -> {
                            startBtnGroup.visibility = View.VISIBLE
                            actionBarText.text = getString(R.string.digital_meter)

                        }

                        2 -> {
                            startBtnGroup.visibility = View.GONE
                            actionBarText.text = getString(R.string.text_map)

                        }

                        else -> {
                        }
                    }
                }

            })
            actionBarText.text = getString(R.string.analog_meter)
        }


        Callback.getMeterValue1().observe(this, {
            it.apply {
                unitType = it.unit
                speedObj = it
            }
        })
    }

    fun startStopBtn(v: View) {

        binding.apply {
            if (startBtnTxt.text == getString(R.string.text_start_now)) {

                isStop = false

                speedValue.text = "0"
                distanceValue.text = "0"
                speedObj?.let {
                    Callback.setMeterValue1(it)
                }
                getDefaultPreferences(this@SpeedometerActivity).edit()
                    .putBoolean("speedo_overlay", true).apply()

                if (ContextCompat.checkSelfPermission(
                        this@SpeedometerActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    //loading_layout.visibility = View.VISIBLE
                    val btnText = getString(R.string.text_stop)
                    startBtnTxt.text = btnText
                    mViewModel?.startStopBtnState?.postValue(btnText)
                    startBtn.background = ContextCompat.getDrawable(
                        this@SpeedometerActivity,
                        R.drawable.background_stop_btn
                    )
                    startTime = Calendar.getInstance().timeInMillis
                    timerThread()

                    currentLocation?.getLocation(this@SpeedometerActivity)

                }

            } else {

                val btnText = getString(R.string.text_start_now)
                startBtnTxt.text = btnText
                mViewModel?.startStopBtnState?.postValue(btnText)
                speedValue.text = "0"
                distanceValue.text = "0"
                timeValues.text = "00:00"
                getDefaultPreferences(this@SpeedometerActivity)
                    .edit().putBoolean("speedo_overlay", false).apply()

                startBtn.background = ContextCompat.getDrawable(
                    this@SpeedometerActivity, R.drawable.background_start_btn
                )

                updateTimerThread?.let { handler?.removeCallbacks(it) }
                currentLocation?.removeFusedLocationClient()
                val db = Database.getInstance(this@SpeedometerActivity)
                endTime = System.currentTimeMillis()
                val date = TimeUtils.getFormatDateTime(endTime, "date")
                val distanceObj =
                    Distance(startTime, endTime, avgSpeedInDB, distanceInDB, date, totalTime)
                db.saveInterval(distanceObj)
                isStop = true
                totalTime = 0
                endTime = 0
                paused = false
                mCurrentLocation = null
                lStart = null
                lEnd = null
                distance = 0.0
                distanceInDB = 0.0
                maxSpeedInDB = 0.0
                avgSpeedInDB = 0.0
                maxSpeed = 0.0
                avgSpeed = 0.0

                Callback.setDefaultSpeedo(true)
                speedObj?.let {
                    Callback.setMeterValue1(it)
                }

            }
        }

    }

    override fun gotLocation(locale: Location) {
        if (!isStop) {
            getSpeed(locale)
        }
    }

    override fun getGpsStrength(averageSnr: String) {
        binding.gpsBar.setText(averageSnr)
    }


    private fun timerThread() {

        handler = Handler()

        updateTimerThread = object : Runnable {

            override fun run() {
                totalTime = System.currentTimeMillis() - startTime
                binding.timeValues.text = TimeUtils.getDurationSpeedo(totalTime)
                handler!!.postDelayed(this, 1000)
            }
        }

        handler!!.postDelayed(updateTimerThread!!, 1000)

    }

    private fun getSpeed(it: Location) {

        Callback.setLocationValue(it)

        when (unitType) {
            "km" -> {
                speed = (it.speed * 3600) / 1000.toDouble()
            }

            "mph" -> {
                speed = it.speed * 2.2369
            }

            "knot" -> {
                speed = it.speed * 1.94384
            }
        }

        val speedDB = (it.speed * 3600) / 1000.toDouble()
        maxSpeedInDB = max(speedDB, maxSpeedInDB)
        avgSpeedInDB = speedDB + maxSpeedInDB / 2

        if (speed > maxSpeed) {
            maxSpeed = speed
        }

        mCurrentLocation = it

        if (lStart == null) {
            lStart = mCurrentLocation
            lStart?.latitude = mCurrentLocation!!.latitude
            lStart?.longitude = mCurrentLocation!!.longitude
            lEnd = mCurrentLocation
            lEnd?.latitude = mCurrentLocation!!.latitude
            lEnd?.longitude = mCurrentLocation!!.longitude
        } else {
            if (lStart?.latitude == lEnd?.latitude && lEnd?.latitude == mCurrentLocation?.latitude) {
                return
            }
            lStart = lEnd
            lStart?.latitude = lEnd!!.latitude
            lStart?.longitude = lEnd!!.longitude
            lEnd = mCurrentLocation
            lEnd?.latitude = mCurrentLocation!!.latitude
            lEnd?.longitude = mCurrentLocation!!.longitude
        }

        updateUi(it)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(location: Location) {
        if (lStart != null && lEnd != null) {
            distanceInDB += (lStart!!.distanceTo(lEnd!!).toDouble() / 1000.0)
            binding.apply {
                when (unitType) {
                    "km" -> {
                        distance += (lStart!!.distanceTo(lEnd!!).toDouble() / 1000.0)
                        avgSpeed = (speed + maxSpeed) / 2
                        speedValue.text = "${AppUtils.roundTwoDecimal(avgSpeed)} km"
                        distanceValue.text = "${AppUtils.roundTwoDecimal(distance)} km"
                        pipUnit.text = "kmh"
                        pipSpeed.text = "${speed.toInt()}"
                        Log.w("My-Speed", "Speed Details: " +
                                "distance ${speedValue.text} , average speed ${distanceValue.text}, pipSpeed ${pipSpeed.text}" +
                                " gps accuracy ${location.accuracy}")
                    }
                    "mph" -> {
                        distance += (lStart!!.distanceTo(lEnd!!).toDouble() / 1609.34)
                        avgSpeed = (speed + maxSpeed) / 2
                        speedValue.text = "${AppUtils.roundTwoDecimal(avgSpeed)} mph"
                        distanceValue.text = "${AppUtils.roundTwoDecimal(distance)} mph"
                        pipUnit.text = "mph"
                        pipSpeed.text = "${speed.toInt()}"
                    }
                    "knot" -> {
                        distance += (lStart!!.distanceTo(lEnd!!).toDouble() / 1852)
                        avgSpeed = (speed + maxSpeed) / 2
                        speedValue.text = "${AppUtils.roundTwoDecimal(avgSpeed)} knot"
                        distanceValue.text = "${AppUtils.roundTwoDecimal(distance)} knot"
                        pipUnit.text = "knot"
                        pipSpeed.text = "${speed.toInt()}"
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (getDefaultPreferences(this)
                .getBoolean("app_widget", true) && getDefaultPreferences(this)
                .getBoolean("speedo_overlay", false)
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
                        "Please enable picture in picture mode from settings",
                        Toast.LENGTH_SHORT
                    ).show()
            } else {
                if (!checkServiceRunning(BackgroundPlayService::class.java)) {
                    if (Settings.canDrawOverlays(this)) {
                        startService(
                            Intent(
                                this,
                                BackgroundPlayService::class.java
                            ).setAction("speedo")
                        )
                        isOverlay = true
                    } else if (!Settings.canDrawOverlays(this)) {
                      // stop service
                    } else {
                        startService(
                            Intent(
                                this,
                                BackgroundPlayService::class.java
                            ).setAction("speedo")
                        )
                        isOverlay = true
                    }
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        currentLocation?.removeFusedLocationClient()
        handler?.removeCallbacks(updateTimerThread!!)
        AppUtils.unit = "km"
        AppUtils.type = "cycle"
        if (checkServiceRunning())
            stopService(Intent(this, BackgroundPlayService::class.java))
        getDefaultPreferences(this).edit().putBoolean("speedo_overlay", false).apply()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CurrentLocation.REQUEST_LOCATION) {
            if (resultCode == android.app.Activity.RESULT_OK) {

                currentLocation?.getLocation(this@SpeedometerActivity)

            } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
                Toast.makeText(
                    this, getString(R.string.text_enable_gps), Toast.LENGTH_SHORT
                ).show()
                val btnText = getString(R.string.text_start_now)
                binding.startBtnTxt.text = btnText
                mViewModel?.startStopBtnState?.postValue(btnText)

                binding.startBtn.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.background_start_btn
                )

                updateTimerThread?.let { handler?.removeCallbacks(it) }
                currentLocation?.removeFusedLocationClient()
                Callback.getMeterValue1().removeObservers(this)
                Callback.getLocationData().removeObservers(this)
                isStop = true
                totalTime = 0
                endTime = 0
                paused = false
                mCurrentLocation = null
                lStart = null
                lEnd = null
                distance = 0.0
                maxSpeed = 0.0
                avgSpeed = 0.0
            }
        } else if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
                    isOverlay = false
                } else {
                    startService(Intent(this, BackgroundPlayService::class.java).setAction("pedo"))
                    isOverlay = true
                }
            } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
                // do nothing and wait
                val i = 0
            }
        }
    }

}