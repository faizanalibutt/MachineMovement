package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model

data class DistanceTotal (
    val startTime: Long, val endTime: Long, val speed: Double,
    val distance: Double, val date: String, val totalTime: Long,
    val sumTime: Long, val avgTime: Long, val sumDistance: Double, val avgDistance: Double,
    val sumSpeed: Double, val avgSpeed: Double
)