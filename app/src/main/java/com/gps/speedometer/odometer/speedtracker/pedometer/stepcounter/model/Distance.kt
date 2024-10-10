package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model

import java.text.SimpleDateFormat
import java.util.*

data class Distance(
    val startTime: Long, val endTime: Long, val speed: Double,
    val distance: Double, val date: String, val totalTime: Long
) {

    /**
     * "dd/MM/yyyy"
     *
     */
    val startTimeFormatted: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startTime))

    val endTimeFormatted: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(endTime))

//    val totalTimeFormatted: String
//        get() = SimpleDateFormat("dd-mm-yyyy", Locale.getDefault()).format(Date(endTime))

    /*constructor(distance: Double) : this(date) {

    }

    constructor(speed: Double, distance: Double) : this(date) {

    }

    constructor(startTime: Long, endTime: Long) : this(date) {

    }*//*

    constructor(startTime: Long, endTime: Long, speed: Double, distance: Double, date: Long) : this(date) {

    }*/

}