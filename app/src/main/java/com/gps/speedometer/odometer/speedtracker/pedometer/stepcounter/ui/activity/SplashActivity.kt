package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.databinding.ActivitySplashBinding
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getSplashViews()
    }

    private fun getSplashViews() {
        val isFirstLaunch =
            AppUtils.getDefaultPreferences(this@SplashActivity)
                .getBoolean("is_First_Launch", false)
        if (!isFirstLaunch) {
            showGdpView()
        } else {
            showSplashView()
        }
    }

    private fun showGdpView() {

        binding.apply {
            appIconsplash.visibility = View.GONE
            progressBar.visibility = View.GONE
            binding.layoutGdpInner.layoutGdp.visibility = View.VISIBLE

            @Suppress("DEPRECATION")
            //this.tv_privacy.text = Html.fromHtml("<u>"+resources.getString(R.string.privacy_policy_translate)+"</u>")

            val colorPrimary = ContextCompat.getColor(this@SplashActivity, R.color.black)
            val spanString: SpannableString? = SpannableString(binding.layoutGdpInner.appname.text)
            val appColor = ForegroundColorSpan(colorPrimary)
            spanString!!.setSpan(appColor, 6, binding.layoutGdpInner.appname.text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.layoutGdpInner.appname.text = spanString

            try {
                val text = SpannableString(getString(R.string.privacy_policy_desc))
                text.setSpan(UnderlineSpan(), 27, binding.layoutGdpInner.privacytext.length(), 0)
                text.setSpan(appColor, 27, binding.layoutGdpInner.privacytext.length(), 0)
                binding.layoutGdpInner.privacytext.text = text
            } catch (exp: Exception) {
                binding.layoutGdpInner.privacytext.text = getString(R.string.privacy_policy_desc)
            }

            binding.layoutGdpInner.privacytext.setOnClickListener {
                val url =
                    getString(R.string.privacy_policy_link_text)
                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this@SplashActivity, Uri.parse(url))
            }

            AppUtils.getDefaultPreferences(this@SplashActivity).edit().putBoolean(
                "show_rating_dialog", true
            ).apply()

            binding.layoutGdpInner.startbutton.setOnClickListener {
                AppUtils.getDefaultPreferences(this@SplashActivity).edit().putBoolean(
                    "is_First_Launch", true
                ).apply()
                showSplashView()
            }
        }

    }

    private fun showSplashView() {

        binding.apply {

            appIconsplash.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            binding.layoutGdpInner.layoutGdp.visibility = View.GONE

            showMain()
        }

    }

    private fun showMain(stuckLimit: Long = 3000) {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, stuckLimit)
    }

}
