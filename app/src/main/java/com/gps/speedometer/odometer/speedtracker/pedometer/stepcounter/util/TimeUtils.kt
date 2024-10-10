package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util

import android.content.Context
import android.text.format.DateUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app.App
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatDateTime(context: Context, millis: Long): CharSequence {
        return DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
        )
    }

    fun getFormatDateTime(millis: Long, type: String): String {
        return if (type == "time")
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
        else
            SimpleDateFormat("yyyy-MM-dd", Locale(App.localeManager!!.language)).format(Date(millis))
    }

    fun getFormatDate(millis: Long): String {

        return SimpleDateFormat("E", Locale.getDefault()).format(Date(millis)).toString()
    }

    @JvmStatic
    fun getFormatStringDate(stringDate: String) : String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale(App.localeManager!!.language))
        var date: Date? = null
        try {
            date = sdf.parse(stringDate)
        } catch (parseExp: ParseException) {
            Logger.log(parseExp.localizedMessage)
        }
        sdf.applyPattern("EEE")
        return sdf.format(date!!).toString()
    }

    fun getFormatedTimeMH(millis: Long): String = String.format(
        "%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                millis
            )
        )
    )

    fun getFormatedTimeMH2(millis: Long): String = String.format(
        "%02dh%02dm", TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                millis
            )
        )
    )

    fun getFormatedTimeMHS(millis: Long): String = String.format(
        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                millis
            )
        ),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                millis
            )
        )
    )

    fun getFormatedTimeMHS2(millis: Long): String = String.format(
        "%02dh%02dm%02ds", TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                millis
            )
        ),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                millis
            )
        )
    )

    fun getDuration(milliseconds: Long): String {
        val string = StringBuilder()

        val calculator = ElapsedTime.ElapsedTimeCalculator(milliseconds / 1000L)

        val hours = calculator.crop(3600L)
        val minutes = calculator.crop(60L)
        val seconds = calculator.leftTime

        if (hours > 0L) {

            string.append(hours)
            if (hours > 0L && minutes > 0L && seconds > 0L)
                string.append("h ")
            else if (hours > 0L && minutes > 0L && seconds == 0L)
                string.append("h ")
            else if (hours > 0L && minutes == 0L && seconds > 0L)
                string.append("h ")
            else
                string.append(" h")
        }

        if (minutes > 0L) {

            string.append(minutes)
            if (hours > 0L && minutes > 0L && seconds > 0L)
                string.append("m ")
            else if (hours > 0L && minutes > 0L && seconds == 0L)
                string.append("m ")
            else if (hours == 0L && minutes > 0L && seconds > 0L)
                string.append("m ")
            else
                string.append(" m")
        }

        if (seconds > 0L) {

            string.append(seconds)
            if (hours > 0L && minutes > 0L && seconds > 0L)
                string.append("s")
            else if (hours == 0L && minutes > 0L && seconds > 0L)
                string.append("s ")
            else if (hours > 0L && minutes == 0L && seconds > 0L)
                string.append("s ")
            else
                string.append(" s")
        } else {
            if (hours != 0L || minutes != 0L)
                return string.toString()
        }

        return string.toString()
    }

    fun getDurationSpeedo(milliseconds: Long): String {
        val string = StringBuilder()

        val calculator = ElapsedTime.ElapsedTimeCalculator(milliseconds / 1000L)

        val hours = calculator.crop(3600L)
        val minutes = calculator.crop(60L)
        val seconds = calculator.leftTime

        // 00:00 min -> 01:00
        if (hours > 0L) {

            if (hours < 10)
                string.append("0")

            string.append(hours)

        } else {
            if (hours == 60L)
                string.append("01")
            else
                string.append("00")
        }

        string.append("hr ")

        if (minutes > 0L) {
            if (minutes < 10)
                string.append("0")

            string.append(minutes)
        } else {
            if (seconds == 60L)
                string.append("01")
            else
                string.append("00")
        }

        /*if (hours > 0)
            string.append(" hr")
        else*/
        string.append("min")

        return string.toString()
    }

}