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
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.databinding.FragmentDigitalBinding
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.Speedo
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils

class DigitalFragment() : Fragment() {

    private var mContext: Context? = null

    constructor(context: Context) : this() {
        this.mContext = context
    }

    private var vehicle: String = "car"
    private var unitMain: String = "km"

    private var _binding: FragmentDigitalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDigitalBinding.inflate(inflater, container, false)

        defaultSettings(binding.root)

        binding.apply {
            cycleView.setOnClickListener {

                vehicle = "cycle"
                Callback.setMeterValue1(Speedo(vehicle, unitMain, unitsText.text.toString(), ""))
            }

            cycleView.setOnClickListener {

                vehicle = "car"
                Callback.setMeterValue1(Speedo(vehicle, unitMain, unitsText.text.toString(), ""))

            }

            trainView.setOnClickListener {
                vehicle = "train"
                Callback.setMeterValue1(Speedo(vehicle, unitMain, unitsText.text.toString(), ""))
            }

            popupUnits.setOnClickListener {
                showPopup(binding.root)
            }

            Callback.setDefaultSpeedo(true)
        }
        

        return binding.root
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

        binding.apply {
            digiSpeedTxt.typeface = typeface
            digiTypeTxt.typeface = typeface

            val speedObserver = Observer<Location> {
                getSpeed(it)
            }

            val meterObserver = Observer<Speedo> {
                setValues(it, view)
            }

            val defaultObserver = Observer<Boolean> {
                if (it) {
                    digiSpeedTxt.text = "00"
                    digiTypeTxt.text = view.resources.getString(R.string.km_h_c)
                    unitsText.text = view.resources.getString(R.string.km_h_c)
                }
            }

            Callback.getLocationData().observe(viewLifecycleOwner, speedObserver)
            Callback.getMeterValue1().observe(viewLifecycleOwner, meterObserver)
            Callback.getDefaultSpeedoValues().observe(viewLifecycleOwner, defaultObserver)
            unitsText.isSelected = true
        }

    }

    private fun setValues(speedo: Speedo, view: View) {

        binding.apply {
            vehicle = speedo.type
            unitMain = speedo.unit
            digiTypeTxt.text = speedo.unit_text
            unitsText.text = speedo.unit_text

            when (speedo.type) {
                "cycle" -> {
                    changeVehicleView(cycleView, R.color.colorPrimary)
                    changeVehicleView(carView, R.color.black_dark)
                    changeVehicleView(trainView, R.color.black_dark)
                }
                "car" -> {
                    changeVehicleView(cycleView, R.color.black_dark)
                    changeVehicleView(carView, R.color.colorPrimary)
                    changeVehicleView(trainView, R.color.black_dark)
                }
                "train" -> {
                    changeVehicleView(cycleView, R.color.black_dark)
                    changeVehicleView(carView, R.color.black_dark)
                    changeVehicleView(trainView, R.color.colorPrimary)
                }
            }
        }



    }

    private fun getSpeed(it: Location) {

        AppUtils.unit = unitMain
        AppUtils.type = vehicle

        when (unitMain) {

            "km" -> {
                binding.digiSpeedTxt.text = AppUtils.roundOneDecimal(((it.speed * 3600 ) / 1000).toDouble()).toString()
            }

            "mph" -> {
                binding.digiSpeedTxt.text = AppUtils.roundOneDecimal(((it.speed * 2.2369))).toString()
            }

            "knot" -> {
                binding.digiSpeedTxt.text = AppUtils.roundOneDecimal(((it.speed * 1.94384))).toString()
            }

        }
    }

    private fun showPopup(view: View) {

        mContext?.let {
            val popup = PopupMenu(it, binding.popupUnits)
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