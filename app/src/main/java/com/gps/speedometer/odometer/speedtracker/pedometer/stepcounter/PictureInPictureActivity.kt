package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Display
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_picture_in_picture.*

@RequiresApi(Build.VERSION_CODES.O)
class PictureInPictureActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_in_picture)

        pip_btn.setOnClickListener {
            val d: Display = windowManager
                .defaultDisplay
            val p = Point()
            d.getSize(p)
            val width: Int = p.x
            val height: Int = p.y

            val ratio = Rational(width, height)
            val pip_Builder: PictureInPictureParams.Builder = PictureInPictureParams.Builder()
            pip_Builder.setAspectRatio(ratio).build()
            enterPictureInPictureMode(pip_Builder.build())
        }

    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            simple_mode.visibility = View.GONE
            pip_text_view.visibility = View.VISIBLE
        } else {
            simple_mode.visibility = View.VISIBLE
            pip_text_view.visibility = View.GONE
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isInPictureInPictureMode) {
            val d: Display = windowManager
                .defaultDisplay
            val p = Point()
            d.getSize(p)
            val width: Int = p.x
            val height: Int = p.y

            val ratio = Rational(width, height)
            val pip_Builder: PictureInPictureParams.Builder = PictureInPictureParams.Builder()
            pip_Builder.setAspectRatio(ratio).build()
            enterPictureInPictureMode(pip_Builder.build())
        }
    }


    // handle views some will show up and some will hide.
    // aspect ratio will change and it will be handled runtime.

}