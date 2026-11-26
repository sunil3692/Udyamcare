package com.udyamcare.kharcha

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// =====================================================================
// HOME
// =====================================================================

fun MainActivity.buildHome(): View {
    val (from, to) = monthRange(monthOffset)
    val (out, inn) = db.totals(from, to)

    val scroll = ScrollView(this)
    val col = vertical()
    scroll.addView(col)

    // ---- Header ----
    val header = vertical().apply {
        background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR, intArrayOf(C.PRIMARY, C.DARK)
        ).apply {
            val r = dp(28).toFloat()
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
        }
        setPadding(dp(20), dp(18), dp(20), dp(22))
    }
    val top = horizontal()
    top.addView(label(tr("खर्चा", "Kharcha"), 22f, 0xFFFFFFFF.toInt(), true), lp(0, WRAP, 1f))
    top.addView(monthSwitcher(0xFFFFFFFF.toInt()) { render() })
    header.addView(top)

    val net = inn - out
    header.addView(label(tr("इस महीने की बचत", "Saved this month"), 12f, 0xB3FFFFFF.toInt()), lp(WRAP, WRAP, t = dp(14)))
    header.addView(label((if (net < 0) "−" else "") + formatRupees(abs(net)), 34f, 0xFFFFFFFF.toInt(), true))

    val pills = horizontal()
    pills.addView(summaryPill(tr("खर्च", "Spent"), "↑ " + formatRupees(out), C.RED), lp(0, WRAP, 1f, r = dp(6)))
    pills.addView(summaryPill(tr("आमदनी", "Earned"), "↓ " + formatRupees(inn), C.GREEN), lp(0, WRAP, 1f, l = dp(6)))
    header.addView(pills, lp(MATCH, WRAP, t = dp(14)))
    col.addView(header, lp(MATCH, WRAP))

    // ---- Banners ----
    val pending = db.pendingCount()
    if (pending > 0) {
        val b = horizontal().apply {
            background = rounded(C.AMBER_BG, 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener { db.nextPending()?.let { startActivity(PopupActivity.intent(this@buildHome, it.id)) } }
        }
        b.addView(label("⏳", 20f))
        b.addView(label(
            tr("$pending पेमेंट का जवाब बाकी है — टैप करो", "$pending payments waiting for your answer — tap"),
            13f, C.AMBER, true
        ), lp(0, WRAP, 1f, l = dp(10)))
        col.addView(b, lp(MATCH, WRAP, l = dp(16), t = dp(14), r = dp(16)))
    }
    if (!hasPermissions()) {
        val b = vertical().apply {
            background = rounded(C.RED_BG, 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        b.addView(label(tr("⚠️ SMS और Notification की अनुमति नहीं है, तब तक ऐप पेमेंट नहीं पकड़ पाएगा।",
            "⚠️ SMS & notification permission missing — payments can't be detected."), 13f, C.RED, true))
        b.addView(chip(tr("अनुमति दो", "Grant permission"), C.RED, 0xFFFFFFFF.toInt()) { askPermissions() },
            lp(WRAP, WRAP, t = dp(8)))
        col.addView(b, lp(MATCH, WRAP, l = dp(16), t = dp(14), r = dp(16)))
    }

    // ---- Search + filter ----
    val search = EditText(this).apply {
        hint = tr("🔍 नाम या नोट से खोजो", "🔍 Search name or note")
        setText(query)
        textSize = 14f
        setSingleLine()
        background = rounded(C.CARD, 14)
        setPadding(dp(14), dp(10), dp(14), dp(10))
    }
    col.addView(search, lp(MATCH, WRAP, l = dp(16), t = dp(14), r = dp(16)))

    val chips = horizontal()
    val filterNames = listOf(tr("सब", "All"), tr("खर्च", "Out"), tr("आमदनी", "In"))
    val listBox = vertical()
    fun paintChips() {
        chips.removeAllViews()
        filterNames.forEachIndexed { i, n ->
            val on = i == filter
            chips.addView(chip(n, if (on) C.PRIMARY else C.CARD, if (on) 0xFFFFFFFF.toInt() else C.SUB) {
                filter = i; paintChips(); fillList(listBox, from, to)
            }, lp(WRAP, WRAP, r = dp(8)))
        }
    }
    paintChips()
    col.addView(chips, lp(MATCH, WRAP, l = dp(16), t = dp(10), r = dp(16)))

    search.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            query = s?.toString() ?: ""; fillList(listBox, from, to)
        }
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
    })

    col.addView(listBox, lp(MATCH, WRAP, l = dp(16), t = dp(6), r = dp(16), b = dp(90)))
    fillList(listBox, from, to)
    return scroll
}

private fun MainActivity.summaryPill(title: String, value: String, color: Int): View {
    val v = vertical().apply {
        background = rounded(0x26FFFFFF, 14)
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }
    v.addView(label(title, 12f, 0xCCFFFFFF.toInt()))
    v.addView(label(value, 17f, 0xFFFFFFFF.toInt(), true))
    return v
}

fun MainActivity.monthSwitcher(color: Int, onChange: () -> Unit): View {
    val row = horizontal()
    row.addView(label("‹", 26f, color, true).apply {
        setPadding(dp(10), 0, dp(10), 0)
        setOnClickListener { monthOffset--; onChange() }
    })
    row.addView(label(monthLabel(monthOffset), 14f, color, true))
    row.addView(label("›", 26f, if (monthOffset >= 0) (color and 0x00FFFFFF) or 0x55000000 else color, true).apply {
        setPadding(dp(10), 0, dp(10), 0)
        setOnClickListener { if (monthOffset < 0) { monthOffset++; onChange() } }
    })
    return row
}

private fun MainActivity.fillList(box: LinearLayout, from: Long, to: Long) {
    box.removeAllViews()
    val items = db.list(from, to, filter, query)
    if (items.isEmpty()) {
        val e = vertical().apply { gravity = Gravity.CENTER; setPadding(0, dp(40), 0, dp(40)) }
        e.addView(label("🪙", 44f).apply { gravity = Gravity.CENTER })
        e.addView(label(
            tr("कोई एंट्री नहीं।\nपेमेंट होते ही यहाँ अपने-आप आ जाएगा,\nया नीचे ＋ से नकद खर्च जोड़ो।",
                "No entries yet.\nPayments appear here automatically,\nor tap ＋ to add a cash entry."),
            14f, C.SUB
        ).apply { gravity = Gravity.CENTER })
        box.addView(e, lp(MATCH, WRAP))
        return
    }
    var lastDay = -1L
    var dayCard: LinearLayout? = null
    for (t in items) {
        val dk = dayKey(t.time)
        if (dk != lastDay) {
            lastDay = dk
            box.addView(label(dayLabel(t.time), 13f, C.SUB, true), lp(WRAP, WRAP, t = dp(14), b = dp(6), l = dp(4)))
            dayCard = card().apply { setPadding(dp(14), dp(4), dp(14), dp(4)) }
            box.addView(dayCard, lp(MATCH, WRAP))
        }
        dayCard!!.addView(entryRow(t))
    }
}

private fun MainActivity.entryRow(t: Txn): View {
    val cat = Cat.byKey(t.category)
    val debit = t.type == TxType.DEBIT
    val row = horizontal().apply {
        setPadding(0, dp(10), 0, dp(10))
        setOnClickListener {
            if (t.status == 0) startActivity(PopupActivity.intent(this@entryRow, t.id)) else showEntryDialog(t)
        }
    }
    val icon = label(cat?.emoji ?: "❓", 20f).apply {
        gravity = Gravity.CENTER
        background = rounded(if (cat != null) (cat.color and 0x00FFFFFF) or 0x26000000 else C.AMBER_BG, 22)
    }
    row.addView(icon, lp(dp(44), dp(44)))

    val mid = vertical()
    mid.addView(label(t.party.ifBlank { tr("नाम नहीं", "No name") }, 15f, C.TEXT, true).apply {
        setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
    })
    val sub = if (t.status == 0) tr("जवाब दो — टैप करो", "Tap to answer")
    else (if (t.mode == 1) "💵 " else "") + (cat?.name2 ?: "") + (if (t.note.isNotBlank()) " · ${t.note}" else "") + " · " + timeLabel(t.time)
    mid.addView(label(sub, 12f, if (t.status == 0) C.AMBER else C.SUB).apply {
        setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
    })
    row.addView(mid, lp(0, WRAP, 1f, l = dp(12), r = dp(8)))

    row.addView(label((if (debit) "−" else "+") + formatRupees(t.amountPaise), 16f, if (debit) C.RED else C.GREEN, true))
    return row
}

// =====================================================================
// ENTRY DIALOG (add cash entry / edit)
// =====================================================================

fun MainActivity.showEntryDialog(t: Txn?) {
    var type = t?.type ?: TxType.DEBIT
    var sel: Cat? = t?.let { Cat.byKey(it.category) }
    var mode = t?.mode ?: 1

    val box = vertical().apply { setPadding(dp(20), dp(16), dp(20), dp(8)) }
    box.addView(label(if (t == null) tr("नई एंट्री (नकद / अन्य)", "New entry (cash / other)") else tr("एंट्री बदलो", "Edit entry"),
        18f, C.TEXT, true), lp(WRAP, WRAP, b = dp(10)))

    val chipsBox = FrameLayout(this)
    fun rebuild() {
        chipsBox.removeAllViews()
        chipsBox.addView(categoryChips(Cat.forType(type), sel, onAdd = {
            showAddCategoryDialog(type) { c -> sel = c; rebuild() }
        }) { c -> sel = c; rebuild() })
    }

    if (t == null) {
        val toggle = horizontal()
        fun paintToggle() {
            toggle.removeAllViews()
            toggle.addView(chip(tr("↑ खर्च", "↑ Spent"), if (type == TxType.DEBIT) C.RED else C.CHIP,
                if (type == TxType.DEBIT) 0xFFFFFFFF.toInt() else C.SUB) {
                type = TxType.DEBIT; sel = null; paintToggle(); rebuild()
            }, lp(0, WRAP, 1f, r = dp(6)))
            toggle.addView(chip(tr("↓ आमदनी", "↓ Earned"), if (type == TxType.CREDIT) C.GREEN else C.CHIP,
                if (type == TxType.CREDIT) 0xFFFFFFFF.toInt() else C.SUB) {
                type = TxType.CREDIT; sel = null; paintToggle(); rebuild()
            }, lp(0, WRAP, 1f, l = dp(6)))
        }
        paintToggle()
        box.addView(toggle, lp(MATCH, WRAP, b = dp(8)))
    }

    val modeRow = horizontal()
    fun paintMode() {
        modeRow.removeAllViews()
        modeRow.addView(chip("💵 " + tr("नकद", "Cash"), if (mode == 1) C.PRIMARY else C.CHIP,
            if (mode == 1) 0xFFFFFFFF.toInt() else C.SUB) { mode = 1; paintMode() }, lp(0, WRAP, 1f, r = dp(6)))
        modeRow.addView(chip("🏦 Bank/UPI", if (mode == 0) C.PRIMARY else C.CHIP,
            if (mode == 0) 0xFFFFFFFF.toInt() else C.SUB) { mode = 0; paintMode() }, lp(0, WRAP, 1f, l = dp(6)))
    }
    paintMode()
    box.addView(modeRow, lp(MATCH, WRAP, b = dp(8)))

    val amount = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        hint = tr("₹ रकम", "₹ Amount")
        textSize = 24f
        if (t != null) setText(String.format(Locale.US, if (t.amountPaise % 100 == 0L) "%.0f" else "%.2f", t.amountPaise / 100.0))
    }
    box.addView(amount)

    val party = AutoCompleteTextView(this).apply {
        hint = tr("नाम (किसको / किससे)", "Name (paid to / from)")
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        setSingleLine()
        threshold = 1
        setAdapter(ArrayAdapter(this@showEntryDialog, android.R.layout.simple_dropdown_item_1line, db.partyNames()))
        if (t != null) setText(t.party)
    }
    box.addView(party)

    val note = EditText(this).apply {
        hint = tr("नोट (वैकल्पिक)", "Note (optional)")
        setSingleLine()
        if (t != null) setText(t.note)
    }
    box.addView(note)

    box.addView(label(tr("किस काम का?", "Category"), 13f, C.SUB, true), lp(WRAP, WRAP, t = dp(10), b = dp(4)))
    box.addView(chipsBox)
    rebuild()

    val b = AlertDialog.Builder(this)
        .setView(ScrollView(this).apply { addView(box) })
        .setPositiveButton(tr("सेव", "Save"), null)
        .setNegativeButton(tr("रद्द", "Cancel"), null)
    if (t != null) b.setNeutralButton(tr("हटाओ", "Delete"), null)
    val dialog = b.create()
    dialog.show()

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val v = amount.text.toString().toDoubleOrNull()
        if (v == null || v <= 0) {
            amount.error = tr("सही रकम डालो", "Enter a valid amount"); return@setOnClickListener
        }
        val paise = Math.round(v * 100)
        val cat = (sel ?: Cat.other(type)).key
        if (t == null) db.insertManual(type, paise, party.text.toString().trim(), cat, note.text.toString().trim(), System.currentTimeMillis(), mode)
        else db.update(t.id, paise, party.text.toString().trim(), cat, note.text.toString().trim(), mode)
        dialog.dismiss(); render()
    }
    if (t != null) {
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(tr("यह एंट्री हटानी है?", "Delete this entry?"))
                .setPositiveButton(tr("हाँ", "Yes")) { _, _ -> db.delete(t.id); dialog.dismiss(); render() }
                .setNegativeButton(tr("नहीं", "No"), null)
                .show()
        }
    }
}

// =====================================================================
// REPORTS
// =====================================================================

fun MainActivity.buildReports(): View {
    val (from, to) = monthRange(monthOffset)
    val type = if (reportIn) TxType.CREDIT else TxType.DEBIT
    val scroll = ScrollView(this)
    val col = vertical().apply { setPadding(dp(16), dp(16), dp(16), dp(24)) }
    scroll.addView(col)

    val top = horizontal()
    top.addView(label(tr("रिपोर्ट", "Reports"), 22f, C.TEXT, true), lp(0, WRAP, 1f))
    top.addView(monthSwitcher(C.PRIMARY) { render() })
    col.addView(top, lp(MATCH, WRAP, b = dp(12)))
    col.addView(balanceSheetCard(from, to), lp(MATCH, WRAP, b = dp(14)))

    val toggle = horizontal()
    toggle.addView(chip(tr("↑ खर्च", "↑ Spent"), if (!reportIn) C.RED else C.CARD, if (!reportIn) 0xFFFFFFFF.toInt() else C.SUB) {
        reportIn = false; render()
    }, lp(0, WRAP, 1f, r = dp(6)))
    toggle.addView(chip(tr("↓ आमदनी", "↓ Earned"), if (reportIn) C.GREEN else C.CARD, if (reportIn) 0xFFFFFFFF.toInt() else C.SUB) {
        reportIn = true; render()
    }, lp(0, WRAP, 1f, l = dp(6)))
    col.addView(toggle, lp(MATCH, WRAP, b = dp(12)))

    // Donut + legend
    val data = db.byCategory(from, to, type)
    val total = data.sumOf { it.second }
    val c1 = card()
    c1.addView(label(tr("कहाँ गया / कहाँ से आया", "By category"), 15f, C.TEXT, true))
    val donut = DonutView(this).apply {
        slices = data.map { (Cat.byKey(it.first)?.color ?: 0xFF94A3B8.toInt()) to it.second }
        centerTop = if (reportIn) tr("कुल आमदनी", "Total earned") else tr("कुल खर्च", "Total spent")
        centerBottom = formatRupees(total)
    }
    c1.addView(donut, lp(MATCH, dp(210), t = dp(8), b = dp(8)))
    if (data.isEmpty()) {
        c1.addView(label(tr("इस महीने कोई सेव एंट्री नहीं।", "No saved entries this month."), 13f, C.SUB).apply { gravity = Gravity.CENTER })
    }
    for ((key, v) in data) {
        val cat = Cat.byKey(key) ?: Cat.other(type)
        val row = horizontal().apply { setPadding(0, dp(7), 0, dp(7)) }
        row.addView(View(this).apply { background = rounded(cat.color, 6) }, lp(dp(12), dp(12), r = dp(10)))
        row.addView(label(cat.label, 14f), lp(0, WRAP, 1f))
        row.addView(label(formatRupees(v), 14f, C.TEXT, true))
        row.addView(label("  ${(v * 100 / total.coerceAtLeast(1L))}%", 12f, C.SUB), lp(dp(44), WRAP))
        c1.addView(row)
    }
    col.addView(c1, lp(MATCH, WRAP, b = dp(14)))

    // Last 6 months bars
    val c2 = card()
    c2.addView(label(tr("पिछले 6 महीने", "Last 6 months"), 15f, C.TEXT, true))
    val legend = horizontal()
    legend.addView(label("■ ", 12f, C.RED)); legend.addView(label(tr("खर्च   ", "Spent   "), 12f, C.SUB))
    legend.addView(label("■ ", 12f, C.GREEN)); legend.addView(label(tr("आमदनी", "Earned"), 12f, C.SUB))
    c2.addView(legend, lp(WRAP, WRAP, t = dp(4)))
    val bars = BarsView(this).apply {
        months = (-5..0).map { o ->
            val (f, t2) = monthRange(o)
            val (out, inn) = db.totals(f, t2)
            Triple(monthLabel(o, true), out, inn)
        }
    }
    c2.addView(bars, lp(MATCH, dp(190), t = dp(8)))
    col.addView(c2, lp(MATCH, WRAP, b = dp(14)))

    // Top people
    val people = db.topParties(from, to, type, 5)
    val c3 = card()
    c3.addView(label(if (reportIn) tr("सबसे ज़्यादा किससे आया", "Top payers") else tr("सबसे ज़्यादा किसको दिया", "Top payees"), 15f, C.TEXT, true))
    if (people.isEmpty()) c3.addView(label(tr("अभी कुछ नहीं।", "Nothing yet."), 13f, C.SUB), lp(WRAP, WRAP, t = dp(6)))
    people.forEachIndexed { i, (name, v) ->
        val row = horizontal().apply { setPadding(0, dp(8), 0, dp(8)) }
        row.addView(label("${i + 1}", 13f, C.SUB, true), lp(dp(22), WRAP))
        row.addView(label(name, 14f), lp(0, WRAP, 1f))
        row.addView(label(formatRupees(v), 14f, if (reportIn) C.GREEN else C.RED, true))
        c3.addView(row)
    }
    col.addView(c3, lp(MATCH, WRAP))
    return scroll
}

// =====================================================================
// UDHAAR
// =====================================================================

fun MainActivity.buildUdhaar(): View {
    val scroll = ScrollView(this)
    val col = vertical().apply { setPadding(dp(16), dp(16), dp(16), dp(24)) }
    scroll.addView(col)

    col.addView(label(tr("उधार का हिसाब", "Udhaar ledger"), 22f, C.TEXT, true), lp(WRAP, WRAP, b = dp(12)))

    val list = db.udhaar()
    val toGet = list.filter { it.second > 0 }
    val toPay = list.filter { it.second < 0 }

    val sums = horizontal()
    sums.addView(udhaarTotal(tr("मुझे मिलने हैं", "To receive"), toGet.sumOf { it.second }, C.GREEN, C.GREEN_BG), lp(0, WRAP, 1f, r = dp(6)))
    sums.addView(udhaarTotal(tr("मुझे देने हैं", "To pay"), abs(toPay.sumOf { it.second }), C.RED, C.RED_BG), lp(0, WRAP, 1f, l = dp(6)))
    col.addView(sums, lp(MATCH, WRAP, b = dp(14)))

    if (list.isEmpty()) {
        val e = card().apply { gravity = Gravity.CENTER }
        e.addView(label("🤝", 40f).apply { gravity = Gravity.CENTER })
        e.addView(label(
            tr("अभी कोई उधार नहीं।\nपेमेंट सेव करते समय नाम लिखकर\n\"उधार दिया\" या \"उधार लिया\" चुनो —\nहिसाब यहाँ अपने-आप बनेगा।",
                "No udhaar yet.\nWhen saving a payment, enter the name and pick\n\"Lent\" or \"Borrowed\" —\nthe ledger builds here automatically."),
            13f, C.SUB
        ).apply { gravity = Gravity.CENTER })
        col.addView(e, lp(MATCH, WRAP))
        return scroll
    }

    fun section(title: String, rows: List<Pair<String, Long>>, color: Int) {
        if (rows.isEmpty()) return
        col.addView(label(title, 13f, C.SUB, true), lp(WRAP, WRAP, t = dp(4), b = dp(6), l = dp(4)))
        val c = card().apply { setPadding(dp(14), dp(4), dp(14), dp(4)) }
        for ((name, v) in rows) {
            val row = horizontal().apply {
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { showPartyHistory(name) }
            }
            row.addView(label(name.take(1).uppercase(), 16f, 0xFFFFFFFF.toInt(), true).apply {
                gravity = Gravity.CENTER; background = rounded(color, 20)
            }, lp(dp(40), dp(40)))
            row.addView(label(name, 15f, C.TEXT, true), lp(0, WRAP, 1f, l = dp(12)))
            row.addView(label(formatRupees(abs(v)), 16f, color, true))
            c.addView(row)
        }
        col.addView(c, lp(MATCH, WRAP, b = dp(10)))
    }
    section(tr("मुझे मिलने हैं", "They owe me"), toGet, C.GREEN)
    section(tr("मुझे देने हैं", "I owe them"), toPay, C.RED)
    return scroll
}

private fun MainActivity.udhaarTotal(title: String, paise: Long, color: Int, bg: Int): View {
    val v = vertical().apply { background = rounded(bg, 16); setPadding(dp(14), dp(12), dp(14), dp(12)) }
    v.addView(label(title, 12f, color))
    v.addView(label(formatRupees(paise), 20f, color, true))
    return v
}

private fun MainActivity.showPartyHistory(name: String) {
    val fmt = SimpleDateFormat("d MMM", L.locale)
    val lines = db.byParty(name).joinToString("\n") { t ->
        val sign = if (t.type == TxType.DEBIT) "−" else "+"
        "${fmt.format(Date(t.time))}   $sign${formatRupees(t.amountPaise)}   ${Cat.byKey(t.category)?.name2 ?: ""}"
    }
    AlertDialog.Builder(this).setTitle(name).setMessage(lines).setPositiveButton("OK", null).show()
}

// =====================================================================
// SETTINGS
// =====================================================================

fun MainActivity.buildSettings(): View {
    val scroll = ScrollView(this)
    val col = vertical().apply { setPadding(dp(16), dp(16), dp(16), dp(24)) }
    scroll.addView(col)
    col.addView(label(tr("सेटिंग", "Settings"), 22f, C.TEXT, true), lp(WRAP, WRAP, b = dp(12)))

    // Language
    val c1 = card()
    c1.addView(label(tr("भाषा", "Language"), 15f, C.TEXT, true))
    val langRow = horizontal()
    langRow.addView(chip("हिंदी", if (L.hindi) C.PRIMARY else C.CHIP, if (L.hindi) 0xFFFFFFFF.toInt() else C.SUB) {
        L.set(this, true); render()
    }, lp(0, WRAP, 1f, r = dp(6)))
    langRow.addView(chip("English", if (!L.hindi) C.PRIMARY else C.CHIP, if (!L.hindi) 0xFFFFFFFF.toInt() else C.SUB) {
        L.set(this, false); render()
    }, lp(0, WRAP, 1f, l = dp(6)))
    c1.addView(langRow, lp(MATCH, WRAP, t = dp(8)))
    col.addView(c1, lp(MATCH, WRAP, b = dp(12)))

    // My categories
    val cc = card()
    cc.addView(label(tr("मेरी category", "My categories"), 15f, C.TEXT, true))
    val mine = Cat.customList
    if (mine.isEmpty()) {
        cc.addView(label(tr("अभी कोई नहीं। \"किस काम का?\" में ➕ नई दबाकर भी जोड़ सकते हो।",
            "None yet. You can also tap ➕ New under Category."), 12f, C.SUB), lp(WRAP, WRAP, t = dp(6)))
    }
    for (c in mine) {
        val row = horizontal().apply { setPadding(0, dp(8), 0, dp(8)) }
        row.addView(label(c.emoji, 18f), lp(dp(34), WRAP))
        row.addView(label(c.name2 + if (c.type == TxType.DEBIT) tr("  (खर्च)", "  (spent)") else tr("  (आमदनी)", "  (earned)"), 14f), lp(0, WRAP, 1f))
        row.addView(label("✕", 18f, C.RED, true).apply {
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener {
                AlertDialog.Builder(this@buildSettings)
                    .setMessage(tr("\"${c.name2}\" हटानी है? इसकी एंट्री \"अन्य\" में चली जाएँगी।",
                        "Remove \"${c.name2}\"? Its entries move to \"Other\"."))
                    .setPositiveButton(tr("हाँ", "Yes")) { _, _ ->
                        db.reassignCategory(c.key, Cat.other(c.type).key)
                        Cat.removeCustom(this@buildSettings, c.key)
                        render()
                    }
                    .setNegativeButton(tr("नहीं", "No"), null)
                    .show()
            }
        })
        cc.addView(row)
    }
    val addRow = horizontal()
    addRow.addView(chip(tr("➕ खर्च category", "➕ Spent category"), C.RED_BG, C.RED) {
        showAddCategoryDialog(TxType.DEBIT) { render() }
    }, lp(WRAP, WRAP, r = dp(8)))
    addRow.addView(chip(tr("➕ आमदनी category", "➕ Earned category"), C.GREEN_BG, C.GREEN) {
        showAddCategoryDialog(TxType.CREDIT) { render() }
    }, lp(WRAP, WRAP))
    cc.addView(addRow, lp(WRAP, WRAP, t = dp(8)))
    col.addView(cc, lp(MATCH, WRAP, b = dp(12)))

    // Detection
    val c2 = card()
    c2.addView(label(tr("पेमेंट पकड़ना", "Payment detection"), 15f, C.TEXT, true))
    val ok = hasPermissions()
    c2.addView(label(
        if (ok) tr("✅ SMS और Notification की अनुमति मिली हुई है।", "✅ SMS & notification permission granted.")
        else tr("⚠️ अनुमति बाकी है।", "⚠️ Permission missing."),
        13f, if (ok) C.GREEN else C.RED
    ), lp(WRAP, WRAP, t = dp(6)))
    if (!ok) c2.addView(chip(tr("अनुमति दो", "Grant permission"), C.PRIMARY, 0xFFFFFFFF.toInt()) { askPermissions() }, lp(WRAP, WRAP, t = dp(8)))

    c2.addView(label(
        tr("popup ठीक से आए, इसके लिए:\n• Battery में ऐप को \"No restrictions\" करो\n• लॉक स्क्रीन पर popup के लिए \"Full-screen notifications\" की अनुमति दो",
            "For reliable popups:\n• Set battery to \"No restrictions\" for this app\n• Allow \"Full-screen notifications\" for lock-screen popups"),
        12f, C.SUB
    ), lp(WRAP, WRAP, t = dp(10)))
    val tipRow = horizontal()
    tipRow.addView(chip(tr("🔋 Battery", "🔋 Battery"), C.CHIP, C.TEXT) { openAppSettings() }, lp(WRAP, WRAP, r = dp(8)))
    tipRow.addView(chip(tr("🔔 Full-screen", "🔔 Full-screen"), C.CHIP, C.TEXT) { openFullScreenSettings() }, lp(WRAP, WRAP))
    c2.addView(tipRow, lp(WRAP, WRAP, t = dp(8)))
    col.addView(c2, lp(MATCH, WRAP, b = dp(12)))

    // Import old SMS
    val c3 = card()
    c3.addView(label(tr("पुराने SMS से एंट्री जोड़ो", "Import from old SMS"), 15f, C.TEXT, true))
    c3.addView(label(tr("इनबॉक्स के बैंक SMS पढ़कर \"बाकी\" एंट्री बनती हैं, जिनका जवाब आप होम से दे सकते हैं।",
        "Reads bank SMS in your inbox and creates pending entries you can answer from Home."), 12f, C.SUB), lp(WRAP, WRAP, t = dp(6)))
    val scanRow = horizontal()
    scanRow.addView(chip(tr("30 दिन", "30 days"), C.PRIMARY, 0xFFFFFFFF.toInt()) { scanInbox(30) }, lp(WRAP, WRAP, r = dp(8)))
    scanRow.addView(chip(tr("90 दिन", "90 days"), C.PRIMARY, 0xFFFFFFFF.toInt()) { scanInbox(90) }, lp(WRAP, WRAP))
    c3.addView(scanRow, lp(WRAP, WRAP, t = dp(8)))
    col.addView(c3, lp(MATCH, WRAP, b = dp(12)))

    // Export / data
    val c4 = card()
    c4.addView(label(tr("डेटा", "Data"), 15f, C.TEXT, true))
    val dataRow = horizontal()
    dataRow.addView(chip(tr("📤 CSV भेजो / सेव करो", "📤 Export CSV"), C.CHIP, C.TEXT) { exportCsv() }, lp(WRAP, WRAP, r = dp(8)))
    dataRow.addView(chip(tr("🗑 सब मिटाओ", "🗑 Erase all"), C.RED_BG, C.RED) {
        AlertDialog.Builder(this)
            .setMessage(tr("सारी एंट्री हमेशा के लिए मिट जाएँगी। पक्का?", "All entries will be erased forever. Sure?"))
            .setPositiveButton(tr("हाँ, मिटाओ", "Yes, erase")) { _, _ -> db.deleteAll(); render() }
            .setNegativeButton(tr("नहीं", "No"), null)
            .show()
    }, lp(WRAP, WRAP))
    c4.addView(dataRow, lp(WRAP, WRAP, t = dp(8)))
    c4.addView(label(tr("आपका सारा डेटा सिर्फ़ इसी फ़ोन में रहता है।", "All your data stays on this phone only."), 12f, C.SUB), lp(WRAP, WRAP, t = dp(8)))
    col.addView(c4, lp(MATCH, WRAP))
    return scroll
}

private fun MainActivity.scanInbox(days: Int) {
    if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        askPermissions(); return
    }
    val since = System.currentTimeMillis() - days * 24L * 3600 * 1000
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
    Toast.makeText(this, tr("$added नई एंट्री मिलीं", "$added new entries found"), Toast.LENGTH_LONG).show()
    tab = 0; render()
}

private fun MainActivity.exportCsv() {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val sb = StringBuilder("Date,Type,Amount,Name,Category,Note\n")
    fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
    for (t in db.all()) {
        sb.append(fmt.format(Date(t.time))).append(',')
            .append(if (t.type == TxType.DEBIT) "Out" else "In").append(',')
            .append(String.format(Locale.US, "%.2f", t.amountPaise / 100.0)).append(',')
            .append(q(t.party)).append(',')
            .append(q(Cat.byKey(t.category)?.en ?: "")).append(',')
            .append(q(t.note)).append('\n')
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Kharcha export")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    startActivity(Intent.createChooser(send, tr("CSV कहाँ भेजें?", "Share CSV via")))
}

private fun MainActivity.openAppSettings() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    } catch (e: Exception) {
        Toast.makeText(this, "Settings → Apps → Kharcha", Toast.LENGTH_LONG).show()
    }
}

private fun MainActivity.openFullScreenSettings() {
    try {
        startActivity(Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", Uri.parse("package:$packageName")))
    } catch (e: Exception) {
        openAppSettings()
    }
}


// =====================================================================
// BALANCE SHEET
// =====================================================================

private fun signedRupees(v: Long?): String = when {
    v == null -> "—"
    v < 0 -> "−" + formatRupees(-v)
    else -> formatRupees(v)
}

private fun MainActivity.balanceSheetCard(from: Long, to: Long): View {
    val f = db.flows(from, to)
    val o = Opening.get(this)
    val before = if (from >= o.from) db.flows(o.from, from) else null
    val ob = before?.let { o.bank + it.bankIn - it.bankOut }
    val oc = before?.let { o.cash + it.cashIn - it.cashOut }
    val ot = if (ob != null && oc != null) ob + oc else null
    val cb = ob?.let { it + f.bankIn - f.bankOut }
    val cc = oc?.let { it + f.cashIn - f.cashOut }
    val ct = if (cb != null && cc != null) cb + cc else null

    val card = card()
    card.addView(label("📒 " + tr("बैलेंस शीट", "Balance sheet") + " — " + monthLabel(monthOffset), 16f, C.TEXT, true))

    fun row(name: String, a: Long?, b: Long?, c: Long?, color: Int, bold: Boolean = false, top: Boolean = false): View {
        val r = horizontal().apply { setPadding(0, dp(7), 0, dp(7)) }
        if (top) r.background = android.graphics.drawable.GradientDrawable().apply { setStroke(dp(1), C.LINE) }
        r.addView(label(name, 12.5f, color, bold), lp(0, WRAP, 1.35f))
        for (v in listOf(a, b, c)) {
            r.addView(label(signedRupees(v), 12.5f, color, bold).apply { gravity = Gravity.END }, lp(0, WRAP, 1f))
        }
        return r
    }

    card.addView(row("", null, null, null, C.SUB).also {
        (it as LinearLayout).removeAllViews()
        it.addView(label("", 12f), lp(0, WRAP, 1.35f))
        for (h in listOf("🏦 Bank", "💵 " + tr("नकद", "Cash"), tr("कुल", "Total"))) {
            it.addView(label(h, 12f, C.SUB, true).apply { gravity = Gravity.END }, lp(0, WRAP, 1f))
        }
    }, lp(MATCH, WRAP, t = dp(6)))
    card.addView(row(tr("शुरुआत में", "Opening"), ob, oc, ot, C.SUB))
    card.addView(row("＋ " + tr("आए", "Came in"), f.bankIn, f.cashIn, f.inReal, C.GREEN))
    card.addView(row("－ " + tr("गए", "Went out"), f.bankOut, f.cashOut, f.outReal, C.RED))
    card.addView(row("＝ " + tr("बचना चाहिए", "Should be left"), cb, cc, ct, C.TEXT, bold = true, top = true))

    if (f.atm > 0) {
        card.addView(label(tr("🏧 ATM से ${formatRupees(f.atm)} निकाले: Bank के \"गए\" और नकद के \"आए\" में गिना है, कुल खर्च में नहीं।",
            "🏧 ATM withdrawals ${formatRupees(f.atm)}: counted as Bank out and Cash in, not as spending."), 11f, C.SUB), lp(WRAP, WRAP, t = dp(4)))
    }

    // Reality check against the balance printed in bank SMS
    val last = db.lastBalanceBefore(to)
    if (!o.isSet) {
        card.addView(label(tr("⚠️ शुरुआती बैलेंस अभी नहीं डाला, इसलिए \"बचना चाहिए\" पूरा सही नहीं है।",
            "⚠️ Opening balance not set yet, so \"Should be left\" is incomplete."), 12f, C.AMBER, true), lp(WRAP, WRAP, t = dp(8)))
    } else if (last != null && cb != null) {
        val diff = last.first - cb
        val fmt = SimpleDateFormat("d MMM, h:mm a", L.locale)
        card.addView(label(tr("असली Bank बैलेंस (आख़िरी SMS, ${fmt.format(Date(last.second))}): ${formatRupees(last.first)}",
            "Actual bank balance (last SMS, ${fmt.format(Date(last.second))}): ${formatRupees(last.first)}"), 12f, C.TEXT, true),
            lp(WRAP, WRAP, t = dp(8)))
        if (abs(diff) < 100) {
            card.addView(label(tr("✅ हिसाब मिल गया", "✅ Books match the bank"), 13f, C.GREEN, true), lp(WRAP, WRAP, t = dp(2)))
        } else {
            card.addView(label(
                tr("⚠️ ${signedRupees(diff)} का फ़र्क: कोई पेमेंट छूटा है, कोई एंट्री \"skip\" हुई है, या नकद/ATM सही नहीं डाला।",
                    "⚠️ Difference ${signedRupees(diff)}: a payment may be missing, skipped, or cash/ATM entered wrongly."),
                12f, C.AMBER, true
            ), lp(WRAP, WRAP, t = dp(2)))
        }
    }

    val btns = horizontal()
    btns.addView(chip("✏️ " + tr("शुरुआती बैलेंस", "Opening balance"), C.CHIP, C.TEXT) { showOpeningDialog(from) }, lp(WRAP, WRAP, r = dp(8)))
    btns.addView(chip("📤 " + tr("शेयर", "Share"), C.CHIP, C.TEXT) {
        val sb = StringBuilder()
        sb.append("Kharcha — ").append(monthLabel(monthOffset)).append("\n\n")
        sb.append("Bank: ").append(tr("शुरुआत", "Opening")).append(" ").append(signedRupees(ob))
            .append("  + ").append(formatRupees(f.bankIn)).append("  − ").append(formatRupees(f.bankOut))
            .append("  = ").append(signedRupees(cb)).append("\n")
        sb.append(tr("नकद", "Cash")).append(": ").append(tr("शुरुआत", "Opening")).append(" ").append(signedRupees(oc))
            .append("  + ").append(formatRupees(f.cashIn)).append("  − ").append(formatRupees(f.cashOut))
            .append("  = ").append(signedRupees(cc)).append("\n\n")
        sb.append(tr("कुल आए", "Total in")).append(": ").append(formatRupees(f.inReal)).append("\n")
        sb.append(tr("कुल गए", "Total out")).append(": ").append(formatRupees(f.outReal)).append("\n")
        sb.append(tr("बचना चाहिए", "Should be left")).append(": ").append(signedRupees(ct)).append("\n")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(send, tr("बैलेंस शीट भेजो", "Share balance sheet")))
    }, lp(WRAP, WRAP))
    card.addView(btns, lp(WRAP, WRAP, t = dp(10)))
    return card
}

private fun MainActivity.showOpeningDialog(from: Long) {
    val o = Opening.get(this)
    val last = db.lastBalanceBefore(Long.MAX_VALUE)
    val guess = if (last != null && last.second >= from) {
        val f = db.flows(from, last.second + 1)
        last.first - (f.bankIn - f.bankOut)
    } else null

    val box = vertical().apply { setPadding(dp(20), dp(16), dp(20), dp(8)) }
    box.addView(label(tr("शुरुआती बैलेंस", "Opening balance") + " — " + monthLabel(monthOffset), 17f, C.TEXT, true))
    box.addView(label(tr("इस महीने की 1 तारीख़ को आपके पास कितना था?", "How much did you have on the 1st of this month?"),
        12f, C.SUB), lp(WRAP, WRAP, t = dp(4), b = dp(8)))

    fun money(hintText: String, v: Long?) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        hint = hintText
        if (v != null) setText(String.format(Locale.US, "%.2f", v / 100.0))
    }
    val bank = money("🏦 Bank ₹", if (o.isSet) o.bank else guess)
    val cash = money("💵 " + tr("नकद ₹", "Cash ₹"), if (o.isSet) o.cash else null)
    box.addView(bank)
    box.addView(cash)
    if (!o.isSet && guess != null) {
        box.addView(label(tr("Bank का आँकड़ा आख़िरी SMS के बैलेंस से निकाला गया अंदाज़ा है। ठीक न लगे तो बदल दो।",
            "Bank figure is estimated from your last SMS balance. Edit if wrong."), 11f, C.SUB), lp(WRAP, WRAP, t = dp(6)))
    }

    val dialog = AlertDialog.Builder(this)
        .setView(box)
        .setPositiveButton(tr("सेव", "Save"), null)
        .setNegativeButton(tr("रद्द", "Cancel"), null)
        .create()
    dialog.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        fun paise(e: EditText) = Math.round((e.text.toString().toDoubleOrNull() ?: 0.0) * 100)
        Opening.set(this, paise(bank), paise(cash), from)
        dialog.dismiss(); render()
    }
}
