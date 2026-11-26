package com.udyamcare.kharcha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    lateinit var db: Db
    var tab = 0
    var monthOffset = 0
    var filter = 0
    var query = ""
    var reportIn = false

    private lateinit var content: FrameLayout
    private lateinit var navHolder: FrameLayout
    private lateinit var fab: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L.load(this)
        db = Db(this)
        window.statusBarColor = C.DARK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C.BG)
        }
        val body = FrameLayout(this)
        content = FrameLayout(this)
        body.addView(content, FrameLayout.LayoutParams(MATCH, MATCH))

        fab = TextView(this).apply {
            text = "＋"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            background = rounded(C.PRIMARY, 30)
            elevation = dp(6).toFloat()
            setOnClickListener { showEntryDialog(null) }
        }
        body.addView(fab, FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, dp(18), dp(18))
        })

        navHolder = FrameLayout(this)
        root.addView(body, lp(MATCH, 0, 1f))
        root.addView(navHolder, lp(MATCH, WRAP))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        L.load(this)
        render()
    }

    /** Rebuild navigation + the current tab. */
    fun render() {
        navHolder.removeAllViews()
        navHolder.addView(buildNav())
        content.removeAllViews()
        val view = when (tab) {
            0 -> buildHome()
            1 -> buildReports()
            2 -> buildUdhaar()
            else -> buildSettings()
        }
        content.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
        fab.visibility = if (tab == 0) View.VISIBLE else View.GONE
    }

    private fun buildNav(): View {
        val bar = horizontal().apply {
            setBackgroundColor(C.CARD)
            elevation = dp(8).toFloat()
        }
        val tabs = listOf(
            "🏠" to tr("होम", "Home"),
            "📊" to tr("रिपोर्ट", "Reports"),
            "🤝" to tr("उधार", "Udhaar"),
            "⚙️" to tr("सेटिंग", "Settings"),
        )
        tabs.forEachIndexed { i, (emoji, name) ->
            val on = i == tab
            val item = vertical().apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener { tab = i; render() }
            }
            item.addView(label(emoji, 20f).apply { gravity = Gravity.CENTER; alpha = if (on) 1f else 0.5f })
            item.addView(label(name, 11f, if (on) C.PRIMARY else C.SUB, on).apply { gravity = Gravity.CENTER })
            bar.addView(item, lp(0, WRAP, 1f))
        }
        return bar
    }

    // ----- permissions -----

    fun hasPermissions(): Boolean {
        val sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val notif = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return sms && notif
    }

    fun askPermissions() {
        val p = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= 33) p += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(p.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }
}
