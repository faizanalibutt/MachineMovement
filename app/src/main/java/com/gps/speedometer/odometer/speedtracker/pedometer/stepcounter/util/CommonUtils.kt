package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

import android.util.TypedValue
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.model.LocationObject

import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * Created by SSS on 11/21/2017.
 */
class CommonUtils {
    companion object {

        private val seed = AtomicInteger()

        fun getNextInt(): Int {
            return seed.incrementAndGet()
        }

        fun getDecimalString(number: Double): String = DecimalFormat("#.##").format(number)

        fun getAverageSpeed(locationList: List<LocationObject>): String? {
            var totalSpeed = 0f
            for (location in locationList)
                totalSpeed += location.speed
            return getDecimalString(
                (totalSpeed / locationList.size).toDouble()
            )
        }

        fun getTopSpeed(locationList: List<LocationObject>): String? {
            var speed = 0f
            for (location in locationList)
                if (speed < location.speed) speed = location.speed
            return getDecimalString(
                speed.toDouble()
            )
        }


        fun ShareText(context: Context, text: String) {
            val smsIntent = Intent(Intent.ACTION_SEND)
            smsIntent.type = "text/plain"
            smsIntent.putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(smsIntent, "Share via"))
        }

        fun getActionBarSize(context: Context): Int {
            val tv = TypedValue()
            return if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
            } else
                0
        }

        fun startRecognizeIntent(activity: Activity, promptToUse: String, maxResultsToReturn: Int, requestCode: Int) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResultsToReturn)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, promptToUse)
            activity.startActivityForResult(intent, requestCode)

        }


        fun complain(context: Context, message: String) {
            alert(
                context,
                "Error: " + message
            )
        }

        fun alert(context: Context, message: String) {
            val bld = AlertDialog.Builder(context)
            bld.setMessage(message)
            bld.setNeutralButton("OK", null)
            bld.create().show()
        }

    }


}