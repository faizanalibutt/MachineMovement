package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.PictureInPictureActivity
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app.App
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.databinding.ActivityMainBinding
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Utility


class MainActivity :
    Activity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        binding.apply {
            binding.contents.speedoImg.postDelayed({ setSpeedoRippleEffect() }, 200)

            AppUtils.animateProButton(this@MainActivity, binding.contents.premiumServices)

            navView.setNavigationItemSelectedListener(this@MainActivity)
            binding.contents.navMenu.setOnClickListener(this@MainActivity)
            binding.contents.actionSettings.setOnClickListener(this@MainActivity)
            binding.contents.actionRateUs.setOnClickListener(this@MainActivity)
            binding.contents.actionShare.setOnClickListener(this@MainActivity)
            binding.contents.actionPro.setOnClickListener(this@MainActivity)
            binding.contents.premiumServices.setOnClickListener(this@MainActivity)

            setMarqee()
            updateMenuItem()
        }


    }

    private fun updateMenuItem() {
        val menuView = navView.menu
        val menuViewItemLan: MenuItem = menuView.getItem(1)
        val menuViewItemPro: MenuItem = menuView.getItem(0)
        val selectLan = menuViewItemLan.actionView?.findViewById<AppCompatTextView>(R.id.select_languague)
        selectLan?.text = Utility.setLanguageLocale()
        selectLan?.isSelected = true
        menuViewItemLan.actionView?.findViewById<AppCompatTextView>(R.id.nav_language_text)
            ?.isSelected = true
        menuViewItemPro.actionView?.findViewById<TextView>(R.id.menu_pro_text)?.isSelected = true
    }

    val xPivot: Float
        get() = binding.contents.speedoView.pivotX ?: 0f
    val yPivot: Float
        get() = (binding.contents.speedoImg.pivotY ?: 0f) + resources.getDimension(R.dimen.dp_12)

    private fun setSpeedoRippleEffect() {

        binding.contents.apply {
            speedoViewEffect.setOnRippleCompleteListener {
                speedoViewEffect.animateRipple(xPivot, yPivot)
            }
            speedoViewEffect.rippleColor = R.color.white
            speedoViewEffect.zoomScale = 2.0f
            speedoViewEffect.animateRipple(xPivot, yPivot)

            speedoViewEffect2.setOnRippleCompleteListener {
                speedoViewEffect2.animateRipple(xPivot, yPivot)
            }
            speedoViewEffect2.rippleColor = R.color.white_tab
            speedoViewEffect2.animateRipple(xPivot, yPivot)

            speedoViewEffect2.setOnClickListener {
                openSpeedo(it)
            }
        }

    }

    private fun setMarqee() {
        binding.contents.apply {
            textView31.isSelected = true
            textView3.isSelected = true
            textSpeedo.isSelected = true
            textStep.isSelected = true
            settings.isSelected = true
            rateUs.isSelected = true
            share.isSelected = true
            premium.isSelected = true
            textView.isSelected = true
        }

    }

    private fun hideItem() {
        val nav_Menu: Menu = navView.menu
        nav_Menu.findItem(R.id.nav_pro).isVisible = false
    }

    fun openSpeedo(view: View) {
        startActivity(Intent(this@MainActivity, SpeedometerActivity::class.java))
    }

    fun openPedoMeter(view: View) {
        startActivity(Intent(this@MainActivity, PedometerActivity::class.java))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_share -> shareIntent()
            R.id.nav_privacy -> {
                val url = getString(R.string.privacy_policy_link_text)
                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this, Uri.parse(url))
            }
            R.id.nav_language -> {
                Utility.showLanguageDialog(this@MainActivity)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.nav_menu -> drawerLayout.openDrawer(GravityCompat.START, true)
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_share -> shareIntent()
            R.id.premium_services ->
                startActivity(Intent(this, PictureInPictureActivity::class.java))
        }
    }

    private fun shareIntent() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        val shareSubText = this.resources.getString(R.string.great_app)
        // TODO: 7/24/2020 get app link ""DONE""
        val shareBodyText =
            this.resources.getString(R.string.share_desc) + " https://play.google.com/store/apps/developer?id=" + packageName
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubText)
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText)
        startActivity(
            Intent.createChooser(
                shareIntent,
                this.resources.getString(R.string.share_with)
            )
        )
    }

    fun Context.showMainLoadingAdDialog(loadingAdDialog: (() -> Unit)?) {
        val dialog: Dialog? = initCustomDialog(R.layout.splash_ad_loading_screen)
        dialog?.findViewById<View?>(R.id.main_loading_pb)?.visibility = View.VISIBLE
        dialog?.show()
        val handler = Handler()
        handler.postDelayed({
            try {
                dialog?.dismiss()
                loadingAdDialog?.invoke()
            } catch (e: Exception) {
            }
        }, 3000)
    }

    fun Context.initCustomDialog(layout: Int): Dialog? {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(layout)
        if (dialog.window != null) {
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.window!!.setBackgroundDrawable(
                resources.getDrawable(android.R.color.transparent)
            )
        }
        dialog.setCancelable(false)
        return dialog
    }


}