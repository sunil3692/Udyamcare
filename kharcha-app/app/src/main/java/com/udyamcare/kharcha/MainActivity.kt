package com.udyamcare.kharcha

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var db: Db
    private lateinit var summary: TextView
    private lateinit var list: ListView
    private lateinit var permBtn: Button
    private var items: List<Txn> = emptyList()
    private val dateFmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        summary = TextView(this).apply { textSize = 18f; setPadding(0, 0, 0, dp(8)) }
        permBtn = Button(this).apply {
            text = "अनुमति दो (SMS + Notification) / Grant permission"
            isAllCaps = false
            setOnClickListener { askPermissions() }
        }
        val scanBtn = Button(this).apply {
            text = "पिछले 30 दिन के SMS स्कैन करो / Scan last 30 days"
            isAllCaps = false
            setOnClickListener { scanInbox() }
        }
        list = ListView(this)

        root.addView(summary)
        root.addView(permBtn)
        root.addView(scanBtn)
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ ->
            val t = items[pos]
            if (t.status == 0) startActivity(PopupActivity.intent(this, t.id))
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            val t = items[pos]
            AlertDialog.Builder(this)
                .setMessage("हटाएँ? / Delete this entry?")
                .setPositiveButton("हाँ / Yes") { _, _ -> db.delete(t.id); refresh() }
                .setNegativeButton("नहीं / No", null)
                .show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        permBtn.visibility = if (hasPermissions()) View.GONE else View.VISIBLE
        refresh()
    }

    private fun refresh() {
        val (out, inn) = db.monthTotals()
        summary.text = "इस महीने / This month\nखर्च Out: ${formatRupees(out)}\nआमदनी In: ${formatRupees(inn)}"
        items = db.recent()
        adapter.notifyDataSetChanged()
    }

    private fun hasPermissions(): Boolean {
        val sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val notif = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return sms && notif
    }

    private fun askPermissions() {
        val p = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= 33) p += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(p.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permBtn.visibility = if (hasPermissions()) View.GONE else View.VISIBLE
    }

    /** Imports old bank SMS from the inbox as pending entries (no notification spam). */
    private fun scanInbox() {
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            askPermissions(); return
        }
        val since = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        var added = 0
        contentResolver.query(
            Uri.parse("content://sms/inbox"), arrayOf("body", "date"),
            "date>=?", arrayOf(since.toString()), "date DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                val body = c.getString(0) ?: continue
                val p = SmsParser.parse(body) ?: continue
                if (db.insertIfNew(p, body, c.getLong(1)) > 0) added++
            }
        }
        Toast.makeText(this, "$added नई एंट्री मिलीं / new entries found", Toast.LENGTH_LONG).show()
        refresh()
    }

    private val adapter = object : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tv = (convertView as? TextView) ?: TextView(this@MainActivity).apply {
                textSize = 16f
                setPadding(0, dp(8), 0, dp(8))
            }
            val t = items[position]
            val sign = if (t.type == TxType.DEBIT) "−" else "+"
            val who = t.party.ifBlank { "—" }
            val tag = if (t.status == 0) "⏳ बाकी / pending — टैप करो" else t.category
            tv.text = "$sign${formatRupees(t.amountPaise)}  $who\n$tag · ${dateFmt.format(Date(t.time))}"
            tv.setTextColor(if (t.type == TxType.DEBIT) 0xFFC62828.toInt() else 0xFF2E7D32.toInt())
            return tv
        }
    }
}
