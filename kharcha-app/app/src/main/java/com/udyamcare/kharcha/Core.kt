package com.udyamcare.kharcha

import android.content.Context
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------- Language ----------
object L {
    var hindi = true
    fun load(ctx: Context) {
        hindi = ctx.getSharedPreferences("kharcha", Context.MODE_PRIVATE).getString("lang", "hi") == "hi"
    }
    fun set(ctx: Context, hi: Boolean) {
        hindi = hi
        ctx.getSharedPreferences("kharcha", Context.MODE_PRIVATE).edit().putString("lang", if (hi) "hi" else "en").apply()
    }
    val locale: Locale get() = if (hindi) Locale("hi", "IN") else Locale("en", "IN")
}

/** Remembers the name you typed for an account number / UPI id so it fills in next time. */
object Alias {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("kharcha_alias", Context.MODE_PRIVATE)
    fun get(ctx: Context, raw: String): String? =
        if (raw.isBlank()) null else prefs(ctx).getString(raw.trim().lowercase(), null)
    fun put(ctx: Context, raw: String, name: String) {
        if (raw.isBlank() || name.isBlank() || raw.trim().equals(name.trim(), true)) return
        prefs(ctx).edit().putString(raw.trim().lowercase(), name.trim()).apply()
    }
}

fun tr(hi: String, en: String): String = if (L.hindi) hi else en

// ---------- Categories ----------
enum class Cat(val key: String, val emoji: String, val hi: String, val en: String, val color: Int, val type: TxType) {
    FOOD("food", "🍵", "खाना-चाय", "Food", 0xFFF59E0B.toInt(), TxType.DEBIT),
    GROCERY("grocery", "🛒", "किराना", "Grocery", 0xFF16A34A.toInt(), TxType.DEBIT),
    STOCK("stock", "📦", "माल/सामान", "Stock", 0xFF2563EB.toInt(), TxType.DEBIT),
    TRAVEL("travel", "🚌", "सफ़र", "Travel", 0xFF7C3AED.toInt(), TxType.DEBIT),
    BILL("bill", "🧾", "बिल/EMI", "Bills/EMI", 0xFFDC2626.toInt(), TxType.DEBIT),
    MEDICAL("medical", "💊", "दवा", "Medical", 0xFFEC4899.toInt(), TxType.DEBIT),
    HOME("home", "🏠", "घर", "Home", 0xFF0891B2.toInt(), TxType.DEBIT),
    SHOP("shop", "🛍️", "ख़रीदारी", "Shopping", 0xFFEA580C.toInt(), TxType.DEBIT),
    LENT("lent", "🤝", "उधार दिया", "Lent", 0xFF9333EA.toInt(), TxType.DEBIT),
    REPAID("repaid", "↩️", "उधार चुकाया", "Repaid", 0xFF64748B.toInt(), TxType.DEBIT),
    OUT_OTHER("out_other", "➖", "अन्य", "Other", 0xFF94A3B8.toInt(), TxType.DEBIT),

    SALE("sale", "💰", "बिक्री", "Sale", 0xFF16A34A.toInt(), TxType.CREDIT),
    SALARY("salary", "💼", "सैलरी", "Salary", 0xFF2563EB.toInt(), TxType.CREDIT),
    BACK("back", "↩️", "उधार वापस मिला", "Got back", 0xFF0891B2.toInt(), TxType.CREDIT),
    BORROW("borrow", "🤝", "उधार लिया", "Borrowed", 0xFF9333EA.toInt(), TxType.CREDIT),
    IN_OTHER("in_other", "➕", "अन्य", "Other", 0xFF94A3B8.toInt(), TxType.CREDIT);

    val name2: String get() = tr(hi, en)
    val label: String get() = "$emoji $name2"

    companion object {
        fun byKey(k: String): Cat? = values().firstOrNull { it.key == k }
        fun forType(t: TxType): List<Cat> = values().filter { it.type == t }
        fun other(t: TxType): Cat = if (t == TxType.DEBIT) OUT_OTHER else IN_OTHER
    }
}

// ---------- Formatting / dates ----------
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun formatRupees(paise: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
        maximumFractionDigits = 2
    }
    return "₹" + nf.format(paise / 100.0)
}

fun compactRupees(paise: Long): String {
    val r = paise / 100.0
    return when {
        r >= 10_000_000 -> String.format(Locale.US, "%.1fCr", r / 10_000_000)
        r >= 100_000 -> String.format(Locale.US, "%.1fL", r / 100_000)
        r >= 1_000 -> String.format(Locale.US, "%.1fk", r / 1_000)
        else -> r.toLong().toString()
    }
}

fun monthRange(offset: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, offset)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val from = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    return from to cal.timeInMillis
}

fun monthLabel(offset: Int, short: Boolean = false): String {
    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
    return SimpleDateFormat(if (short) "MMM" else "MMMM yyyy", L.locale).format(cal.time)
}

fun timeLabel(t: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(t))

fun dayLabel(t: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val today = Calendar.getInstance()
    fun same(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (same(c, today)) return tr("आज", "Today")
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (same(c, today)) return tr("कल", "Yesterday")
    return SimpleDateFormat("d MMM, EEE", L.locale).format(Date(t))
}

fun dayKey(t: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}
