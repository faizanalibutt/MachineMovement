package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SpeedViewModel : ViewModel() {

    var startStopBtnState = MutableLiveData<String>()
    var updateStepsValue = MutableLiveData<Int>()
}