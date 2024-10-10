package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.Database
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.Distance
/*import com.code4rox.adsmanager.**/
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.CurrentLocation
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Logger
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.TimeUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions.check
import io.github.inflationx.viewpump.ViewPumpContextWrapper

import kotlinx.android.synthetic.main.activity_speedometer_main.*
import kotlinx.android.synthetic.main.toolbar_layout_meter.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


open class MainSpeedometerActivity : AppCompatActivity(), /*OnMapReadyCallback,*/
    CurrentLocation.LocationResultListener
/*, FacebookAdsUtils.FacebookInterstitialListner, AdmobUtils.AdmobInterstitialListener*/ {


    private var speed: Double = 0.0
    private var vehicle: String = "car"
    private var unitMain: String = "km"
    private lateinit var typeMain: String
    private var handler: Handler? = null
    private var updateTimerThread: Runnable? = null
    var totalTime: Long = 0
    var startTime: Long = 0
    var endTime: Long = 0
    var paused = false
    private var mCurrentLocation: Location? = null
    var lStart: Location? = null
    var lEnd: Location? = null
    private var distance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var avgSpeed: Double = 0.0
    private var isStop: Boolean = false
    private var isDigiMeter: Boolean = false
    private var isMap: Boolean = false
    private var mMap: GoogleMap? = null
    private var currentLocation: CurrentLocation? = null
//    private lateinit var admobUtils: AdmobUtils
//    private lateinit var facebookAdsUtils: FacebookAdsUtils

    //private var mopubUtils: MopubUtils? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speedometer_main)
        defaultSettings()

        title_toolbar.setOnClickListener {
            startActivity(Intent(this, SpeedoGraphActivity::class.java))
        }

//        admobUtils = AdmobUtils(this, this, InterAdsIdType.INTER_AM)
//        facebookAdsUtils = FacebookAdsUtils(this, this, InterAdsIdType.INTER_FB)

        /*mopubUtils = MopubUtils(this)
        mopubUtils?.loadInterstitial(object : MopubUtils.MopubInterstitialListener {
            override fun onInterstitialAdLoaded() {

            }

            override fun onInterstitialAdClose() {
                finish()

            }

            override fun onInterstitialAdFailed() {

            }

        }, InterAdsIdType.INTER_MP)*/




        popup_units.setOnClickListener {
            showPopup()
        }


        ivToolbarIcon.setOnClickListener {
            /*if (TinyDB.getInstance(this).getBoolean(Constants.IS_FB_AD_LOW_PRIORITY)) {

                if (!admobUtils.showInterstitialAd()) {
                    if (!facebookAdsUtils.showFbInterstitialAd()) {
                        finish()
                    }
                }
            } else {
                if (!facebookAdsUtils.showFbInterstitialAd()) {
                    if (!admobUtils.showInterstitialAd()) {
                        finish()
                    }
                }

            }*/


            /*if(!mopubUtils?.showInterstitialAd()!!){
                finish()
            }*/
        }


//        admobUtils.loadBannerAd(banner_ad_view)
//        mopubUtils?.loadBannerAd(banner_ad_view)
        cycle_btn.setOnClickListener {

            cycle_btn.background = ContextCompat.getDrawable(this,
                R.drawable.selected_bg
            )
            car_btn.background = ContextCompat.getDrawable(this,
                R.drawable.non_selected_bg
            )
            train_btn.background = ContextCompat.getDrawable(this,
                R.drawable.non_selected_bg
            )

            cycle_img.setColorFilter(ContextCompat.getColor(this,
                R.color.white
            ));
            car_img.setColorFilter(ContextCompat.getColor(this,
                R.color.icon_gray
            ));
            train_img.setColorFilter(ContextCompat.getColor(this,
                R.color.icon_gray
            ));

            cycle_txt.setTextColor(Color.WHITE)
            car_txt.setTextColor(ContextCompat.getColor(this,
                R.color.icon_gray
            ))
            train_txt.setTextColor(ContextCompat.getColor(this,
                R.color.icon_gray
            ))


            vehicle = "cycle"
            setValues(vehicle, unitMain)
        }
        car_btn.setOnClickListener {

            cycle_btn.background = ContextCompat.getDrawable(this,
                R.drawable.non_selected_bg
            )
            car_btn.background = ContextCompat.getDrawable(this,
                R.drawable.selected_bg
            )
            train_btn.background = ContextCompat.getDrawable(this,
                R.drawable.non_selected_bg
            )

            cycle_txt.setTextColor(ContextCompat.getColor(this,
                R.color.icon_gray
            ))
            car_txt.setTextColor(Color.WHITE)
            train_txt.setTextColor(ContextCompat.getColor(this,
                R.color.icon_gray
            ))

            cycle_img.setColorFilter(ContextCompat.getColor(this,
                R.color.icon_gray
            ));
            car_img.setColorFilter(ContextCompat.getColor(this,
                R.color.white
            ));
            train_img.setColorFilter(ContextCompat.getColor(this,
                R.color.icon_gray
            ));


            vehicle = "car"
            setValues(vehicle, unitMain)

        }
        train_btn.setOnClickListener {

            cycle_btn.background = ContextCompat.getDrawable(this,
                R.drawable.non_selected_bg
            )
            car_btn.background = ContextCompat.getDrawable(this,
                R.drawable.non_selected_bg
            )
            train_btn.background = ContextCompat.getDrawable(this,
                R.drawable.selected_bg
            )

            cycle_img.setColorFilter(ContextCompat.getColor(this,
                R.color.icon_gray
            ));
            car_img.setColorFilter(ContextCompat.getColor(this,
                R.color.icon_gray
            ));
            train_img.setColorFilter(ContextCompat.getColor(this,
                R.color.white
            ));

            cycle_txt.setTextColor(ContextCompat.getColor(this,
                R.color.icon_gray
            ))
            car_txt.setTextColor(ContextCompat.getColor(this,
                R.color.icon_gray
            ))
            train_txt.setTextColor(Color.WHITE)

            vehicle = "train"
            setValues(vehicle, unitMain)

        }
        analog_txt.setOnClickListener {

            analog_txt.setTextColor(ContextCompat.getColor(this,
                R.color.white
            ))
            digi_txt.setTextColor(ContextCompat.getColor(this,
                R.color.main_bottom_nav_txt
            ))
            map_txt.setTextColor(ContextCompat.getColor(this,
                R.color.main_bottom_nav_txt
            ))
            title_toolbar.text = "Speedometer (Analog)"

            digi_speed_meter.visibility = View.GONE
            speedometer.visibility = View.VISIBLE
            meter_layout.visibility = View.VISIBLE
            unit_layout.visibility = View.VISIBLE

            isDigiMeter = false

        }
        digi_txt.setOnClickListener {

            analog_txt.setTextColor(ContextCompat.getColor(this,
                R.color.main_bottom_nav_txt
            ))
            digi_txt.setTextColor(ContextCompat.getColor(this,
                R.color.white
            ))
            map_txt.setTextColor(ContextCompat.getColor(this,
                R.color.main_bottom_nav_txt
            ))
            digi_speed_meter.visibility = View.VISIBLE
            title_toolbar.text = "Speedometer (Digital)"

            speedometer.visibility = View.GONE
            meter_layout.visibility = View.VISIBLE
            unit_layout.visibility = View.VISIBLE

            isDigiMeter = true

        }
        map_txt.setOnClickListener {
            analog_txt.setTextColor(ContextCompat.getColor(this,
                R.color.main_bottom_nav_txt
            ))
            digi_txt.setTextColor(ContextCompat.getColor(this,
                R.color.main_bottom_nav_txt
            ))
            map_txt.setTextColor(ContextCompat.getColor(this,
                R.color.white
            ))
            /*val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)*/
            title_toolbar.text = "Speedometer (Map)"

            map_layout.visibility = View.VISIBLE
            meter_layout.visibility = View.GONE
            unit_layout.visibility = View.GONE

            isMap = true
        }

        currentLocation = CurrentLocation(this)

        start_btn.setOnClickListener {

            if (start_btn.text == getString(R.string.start_tracking)) {

                isStop = false

                avg_txt.text = "0"

                distance_txt.text = "0"

                speed_txt.text = "0"

                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                val rationale = "Please provide location permission..."
                val options = com.nabinbhandari.android.permissions.Permissions.Options().setRationaleDialogTitle("Info")
                    .setSettingsDialogTitle("Warning")

                check(
                    this@MainSpeedometerActivity,
                    permissions,
                    rationale,
                    options,
                    object : PermissionHandler() {

                        override fun onGranted() {

                            loading_layout.visibility = View.VISIBLE
                            start_btn.text = getString(R.string.stop_tracing)
                            start_btn.background = ContextCompat.getDrawable(
                                this@MainSpeedometerActivity,
                                R.drawable.stop_selected_bg
                            )
                            startTime = Calendar.getInstance().timeInMillis
                            Logger.log(startTime.toString())
                            timerThread()

                            currentLocation?.getLocation(this@MainSpeedometerActivity)

                        }
                    })

            } else {
                start_btn.text = getString(R.string.start_tracking)
                start_btn.background = ContextCompat.getDrawable(this,
                    R.drawable.selected_bg
                )

                handler?.removeCallbacks(updateTimerThread)
                currentLocation?.removeFusedLocationClient()
                val db = Database.getInstance(this)
                endTime = System.currentTimeMillis()
                val date = TimeUtils.getFormatDateTime(endTime, "date")
                val distanceObj = Distance(startTime, endTime, maxSpeed, distance, date, totalTime)
                db.saveInterval(distanceObj)
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

        }

    }

    open fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    open fun convertDate(
        dateInMilliseconds: String,
        dateFormat: String?
    ): String? {
        return DateFormat.format(dateFormat, dateInMilliseconds.toLong()).toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CurrentLocation.REQUEST_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {

                currentLocation?.getLocation(this@MainSpeedometerActivity)

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(
                    this, "Please enable Gps for get current location.", Toast.LENGTH_SHORT
                ).show()
            }

        }


    }


    override fun gotLocation(it: Location) {
        loading_layout.visibility = View.GONE
        if (!isStop) {
            getSpeed(it)
            val sydney = LatLng(it.latitude, it.longitude)
            val cameraPosition = CameraPosition.Builder().target(sydney).zoom(15f).build()
            mMap?.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    cameraPosition
                )
            )
        }
    }

    /*override fun onMapReady(p0: GoogleMap?) {

        mMap = p0
        mMap?.isMyLocationEnabled = true

    }*/

    private fun defaultSettings() {

        speedometer.setStartDegree(150)
        speedometer.setEndDegree(360)
        speedometer.setMaxSpeed(350f)
        speedometer.setMinSpeed(0f)

        val typeface = Typeface.createFromAsset(
            this.assets, "fonts/digital.ttf"
        )
        digi_speed_txt.typeface = typeface
        digi_type_txt.typeface = typeface

    }


    private fun getSpeed(it: Location) {

        when (unitMain) {

            "km" -> {
                speedometer.setSpeedAt((it.speed * 18) / 5.toFloat())
                digi_speed_txt.text = roundTwoDecimal(((it.speed * 18) / 5.toDouble())).toString()

            }
            "mph" -> {
                speedometer.setSpeedAt((it.speed * 2.2369).toFloat())
                digi_speed_txt.text = roundTwoDecimal(((it.speed * 2.2369))).toString()

            }
            "knot" -> {
                speedometer.setSpeedAt((it.speed * 1.94384).toFloat())
//                val f =
                digi_speed_txt.text = roundTwoDecimal(((it.speed * 1.94384))).toString()

            }
        }

        speed = (it.speed * 18) / 5.toDouble()
        if (speed > maxSpeed) {
            maxSpeed = speed
        }

        mCurrentLocation = it

        if (lStart == null) {
            lStart = mCurrentLocation
            lEnd = mCurrentLocation
        } else {
            lEnd = mCurrentLocation
        }

        updateUi()
    }


    private fun timerThread() {

        handler = Handler()

        updateTimerThread = object : Runnable {

            override fun run() {
                totalTime = System.currentTimeMillis() - startTime
                val timeInHours = TimeUnit.MILLISECONDS.toHours(totalTime)
                duration_txt.text = TimeUtils.getFormatedTimeMHS(totalTime)
                handler!!.postDelayed(this, 1000)
            }
        }

        handler!!.postDelayed(updateTimerThread, 1000)

    }

    private fun roundTwoDecimal(d: Double): Double {
        return try {
            (d * 100.0).roundToInt() / 100.0
        } catch (e: Exception) {
            0.0
        } catch (e: IllegalArgumentException) {
            0.0
        }
    }

    private fun updateUi() {

        if (lStart != null && lEnd != null) {
//            distance += lStart!!.distanceTo(lEnd) / 1000.00
            val mileDis = lStart!!.distanceTo(lEnd).toDouble()
            distance = lStart!!.distanceTo(lEnd).toDouble() / 1000
//            avgSpeed = mileDis/TimeUnit.MILLISECONDS.toSeconds(totalTime)
            avgSpeed = maxSpeed / 2
            avg_txt.text = roundTwoDecimal(avgSpeed).toString()
//            lStart = lEnd
            distance_txt.text = roundTwoDecimal(distance).toString()
        }
        speed_txt.text = roundTwoDecimal(maxSpeed).toString()

    }


    private fun showPopup() {
        val popup = PopupMenu(this@MainSpeedometerActivity, popup_units)
        popup.menuInflater.inflate(R.menu.units_popup_menu, popup.menu)
        popup.setOnMenuItemClickListener {

            when (it.title.toString()) {
                "KM/H" -> {
                    unitMain = "km"
                    setValues(vehicle, unitMain)
                    popup_units.text = resources.getString(R.string.km_h_c)
                    digi_type_txt.text = resources.getString(R.string.km_h_c)
                }
                "MPH" -> {
                    unitMain = "mph"
                    setValues(vehicle, unitMain)
                    popup_units.text = resources.getString(R.string.mph_c)
                    digi_type_txt.text = resources.getString(R.string.mph_c)
                }
                "KNOT" -> {
                    unitMain = "knot"
                    setValues(vehicle, unitMain)
                    popup_units.text = resources.getString(R.string.knot_c)
                    digi_type_txt.text = resources.getString(R.string.knot_c)
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }


    private fun setValues(type: String, unit: String) {

        when (type) {
            "car" -> {
                if (speedometer == null) return

                when (unit) {
                    "km" -> {
                        speedometer.setUnit("Km/h")
                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(240f)
                        speedometer.setTicks(0F, 20F, 40F, 60F, 80F, 100F, 120F, 140F, 160F, 180F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.kmph
                            )
                        )
                    }
                    "mph" -> {
                        speedometer.setUnit("MPH")
                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(90f)
//                        speedometer.setTicks(0F, 10F, 20F, 30F, 40F, 50F, 60F, 70F, 80F, 90F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.mph
                            )
                        )

                    }
                    "knot" -> {
                        speedometer.setUnit("KNOT")
                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(63f)
//                        speedometer.setTicks(0F, 7F, 14F, 21F, 28F, 35F, 42F, 49F, 56F, 63F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.knot
                            )
                        )

                    }

                }
            }
            "cycle" -> {
                if (speedometer == null) return

                when (unit) {
                    "km" -> {
                        speedometer.setUnit("Km/h")
                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(72f)
//                        speedometer.setTicks(0F, 8F, 16F, 24F, 32F, 40F, 48F, 56F, 64F, 72F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.c_kmph
                            )
                        )


                    }
                    "mph" -> {
                        speedometer.setUnit("MPH")
                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(36f)
//                        speedometer.setTicks(0F, 4F, 8F, 12F, 16F, 20F, 24F, 28F, 32F, 36F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.c_mph
                            )
                        )

                    }
                    "knot" -> {
                        speedometer.setUnit("KNOT")

                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(27f)
//                        speedometer.setTicks(0F, 3F, 6F, 9F, 12F, 15F, 18F, 21F, 24F, 27F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.c_knot
                            )
                        )

                    }
                }
            }
            "train" -> {
                if (speedometer == null) return
                when (unit) {
                    "km" -> {
                        speedometer.setUnit("Km/h")
                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(360f)
//                        speedometer.setTicks(0F, 40F, 80F, 120F, 160F, 200F, 240F, 280F, 320F, 360F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.t_kmph
                            )
                        )

                    }
                    "mph" -> {
                        speedometer.setUnit("MPH")

                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(180f)
//                        speedometer.setTicks(0F, 20F, 40F, 60F, 80F, 100F, 120F, 140F, 160F, 180F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.t_mph
                            )
                        )

                    }
                    "knot" -> {
                        speedometer.setUnit("KNOT")

                        speedometer.setMinSpeed(0f)
                        speedometer.setMaxSpeed(90f)
//                        speedometer.setTicks(0F, 10F, 20F, 30F, 40F, 50F, 60F, 70F, 80F, 90F)
                        speedometer.setImageSpeedometer(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.t_knot
                            )
                        )

                    }
                }
            }
        }


    }


    /*override fun onFbInterstitialAdClose() {
        finish()
    }

    override fun onFbInterstitialAdLoaded() {
    }

    override fun onFbInterstitialAdFailed() {
    }

    override fun onFbInterstitialAdClick() {
    }

    override fun onFbInterstitialAdImpression() {
    }

    override fun onInterstitialAdClose() {
        finish()
    }

    override fun onInterstitialAdLoaded() {
    }

    override fun onInterstitialAdFailed() {
    }*/


    override fun onDestroy() {
        super.onDestroy()
        currentLocation?.removeFusedLocationClient()
        handler?.removeCallbacks(updateTimerThread)

    }


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onBackPressed() {

        /* if (TinyDB.getInstance(this).getBoolean(Constants.IS_FB_AD_LOW_PRIORITY)) {
             if (!admobUtils.showInterstitialAd()) {
                 if (!facebookAdsUtils.showFbInterstitialAd()) {
                     finish()
                 }
             }
         } else {
             if (!facebookAdsUtils.showFbInterstitialAd()) {
                 if (!admobUtils.showInterstitialAd()) {
                     finish()
                 }
             }

         }*/
        /*if (!mopubUtils?.showInterstitialAd()!!) {
            finish()
        }*/
        super.onBackPressed();

    }

}
