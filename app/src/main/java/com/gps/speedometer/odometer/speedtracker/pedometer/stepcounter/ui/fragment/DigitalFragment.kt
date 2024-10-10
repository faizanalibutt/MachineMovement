package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.callback.Callback
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.Speedo
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import kotlinx.android.synthetic.main.fragment_digital.*
import kotlinx.android.synthetic.main.fragment_digital.units_text
import kotlinx.android.synthetic.main.fragment_digital.view.*
import kotlinx.android.synthetic.main.fragment_digital.view.car_view
import kotlinx.android.synthetic.main.fragment_digital.view.cycle_view
import kotlinx.android.synthetic.main.fragment_digital.view.digi_type_txt
import kotlinx.android.synthetic.main.fragment_digital.view.popup_units
import kotlinx.android.synthetic.main.fragment_digital.view.train_view

class DigitalFragment() : Fragment() {

    private var mContext: Context? = null
    private lateinit var mView: View

    constructor(context: Context) : this() {
        this.mContext = context
    }

    private var vehicle: String = "car"
    private var unitMain: String = "km"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_digital, container, false)

        view.let {
            mView = view
        }

        defaultSettings(view)

        view.cycle_view.setOnClickListener {

            vehicle = "cycle"
            Callback.setMeterValue1(Speedo(vehicle, unitMain, units_text.text.toString(), ""))
        }

        view.car_view.setOnClickListener {

            vehicle = "car"
            Callback.setMeterValue1(Speedo(vehicle, unitMain, units_text.text.toString(), ""))

        }

        view.train_view.setOnClickListener {

            vehicle = "train"
            Callback.setMeterValue1(Speedo(vehicle, unitMain, units_text.text.toString(), ""))

        }

        view.popup_units.setOnClickListener {
            showPopup(mView)
        }

        Callback.setDefaultSpeedo(true)

        return view
    }

    private fun changeVehicleView(view: ImageView, color: Int) {
        if (mContext != null) {
            ImageViewCompat.setImageTintList(
                view, ColorStateList.valueOf(
                    ContextCompat.getColor(mContext!!, color)
                )
            )
        }
    }

    private fun defaultSettings(view: View) {

        // for digi text in digital meter set font type to look digital
        val typeface = Typeface.createFromAsset(
            view.context.assets, "fonts/digital.ttf"
        )
        view.digi_speed_txt.typeface = typeface
        view.digi_type_txt.typeface = typeface

        val speedObserver = Observer<Location> {
            getSpeed(it)
        }

        val meterObserver = Observer<Speedo> {
            setValues(it, view)
        }

        val defaultObserver = Observer<Boolean> {
            if (it) {
                view.digi_speed_txt.text = "00"
                digi_type_txt.text = view.resources.getString(R.string.km_h_c)
                units_text.text = view.resources.getString(R.string.km_h_c)
            }
        }

        Callback.getLocationData().observe(viewLifecycleOwner, speedObserver)
        Callback.getMeterValue1().observe(viewLifecycleOwner, meterObserver)
        Callback.getDefaultSpeedoValues().observe(viewLifecycleOwner, defaultObserver)
        view.units_text.isSelected = true

    }

    private fun setValues(speedo: Speedo, view: View) {

        vehicle = speedo.type
        unitMain = speedo.unit
        digi_type_txt.text = speedo.unit_text
        units_text.text = speedo.unit_text

        when (speedo.type) {
            "cycle" -> {
                changeVehicleView(view.cycle_view, R.color.colorPrimary)
                changeVehicleView(view.car_view, R.color.black_dark)
                changeVehicleView(view.train_view, R.color.black_dark)
            }
            "car" -> {
                changeVehicleView(view.cycle_view, R.color.black_dark)
                changeVehicleView(view.car_view, R.color.colorPrimary)
                changeVehicleView(view.train_view, R.color.black_dark)
            }
            "train" -> {
                changeVehicleView(view.cycle_view, R.color.black_dark)
                changeVehicleView(view.car_view, R.color.black_dark)
                changeVehicleView(view.train_view, R.color.colorPrimary)
            }
        }

    }

    private fun getSpeed(it: Location) {

        AppUtils.unit = unitMain
        AppUtils.type = vehicle

        when (unitMain) {

            "km" -> {
                digi_speed_txt.text = AppUtils.roundOneDecimal(((it.speed * 3600 ) / 1000).toDouble()).toString()
            }

            "mph" -> {
                digi_speed_txt.text = AppUtils.roundOneDecimal(((it.speed * 2.2369))).toString()
            }

            "knot" -> {
                digi_speed_txt.text = AppUtils.roundOneDecimal(((it.speed * 1.94384))).toString()
            }

        }
    }

    private fun showPopup(view: View) {

        mContext?.let {
            val popup = PopupMenu(it, view.popup_units)
            popup.menuInflater.inflate(R.menu.units_popup_menu, popup.menu)

            popup.setOnMenuItemClickListener {
                when (it.title.toString()) {
                    resources.getString(R.string.km_h_c) -> {
                        unitMain = "km"
                        Callback.setMeterValue1(Speedo(vehicle, unitMain, resources.getString(R.string.km_h_c), ""))
                    }
                    resources.getString(R.string.mph_c) -> {
                        unitMain = "mph"
                        Callback.setMeterValue1(Speedo(vehicle, unitMain, resources.getString(R.string.mph_c), ""))
                    }
                    resources.getString(R.string.knot_c) -> {
                        unitMain = "knot"
                        Callback.setMeterValue1(Speedo(vehicle, unitMain, resources.getString(R.string.knot_c), ""))
                    }
                }
                return@setOnMenuItemClickListener true
            }

            popup.show()
        }
    }

}