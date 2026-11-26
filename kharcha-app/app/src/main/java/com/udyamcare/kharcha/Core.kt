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
        Cat.loadCustom(ctx)
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

data class OpeningBal(val bank: Long, val cash: Long, val from: Long, val isSet: Boolean)

/** Opening balance (bank + cash, in paise) as on the 1st of the month the user set it. */
object Opening {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("kharcha_open", Context.MODE_PRIVATE)
    fun get(ctx: Context): OpeningBal {
        val p = prefs(ctx)
        return OpeningBal(p.getLong("bank", 0), p.getLong("cash", 0), p.getLong("from", 0), p.getBoolean("set", false))
    }
    fun set(ctx: Context, bank: Long, cash: Long, from: Long) {
        prefs(ctx).edit().putLong("bank", bank).putLong("cash", cash).putLong("from", from).putBoolean("set", true).apply()
    }
}

fun tr(hi: String, en: String): String = if (L.hindi) hi else en

// ---------- Categories ----------
data class Cat(val key: String, val emoji: String, val hi: String, val en: String, val color: Int, val type: TxType) {

    val name2: String get() = tr(hi, en)
    val label: String get() = "$emoji $name2"
    val isCustom: Boolean get() = key.startsWith("c_")

    companion object {
        val FOOD = Cat("food", "🍵", "खाना-चाय", "Food", 0xFFF59E0B.toInt(), TxType.DEBIT)
        val GROCERY = Cat("grocery", "🛒", "किराना", "Grocery", 0xFF16A34A.toInt(), TxType.DEBIT)
        val STOCK = Cat("stock", "📦", "माल/सामान", "Stock", 0xFF2563EB.toInt(), TxType.DEBIT)
        val TRAVEL = Cat("travel", "🚌", "सफ़र", "Travel", 0xFF7C3AED.toInt(), TxType.DEBIT)
        val BILL = Cat("bill", "🧾", "बिल/EMI", "Bills/EMI", 0xFFDC2626.toInt(), TxType.DEBIT)
        val MEDICAL = Cat("medical", "💊", "दवा", "Medical", 0xFFEC4899.toInt(), TxType.DEBIT)
        val HOME = Cat("home", "🏠", "घर", "Home", 0xFF0891B2.toInt(), TxType.DEBIT)
        val SHOP = Cat("shop", "🛍️", "ख़रीदारी", "Shopping", 0xFFEA580C.toInt(), TxType.DEBIT)
        val LENT = Cat("lent", "🤝", "उधार दिया", "Lent", 0xFF9333EA.toInt(), TxType.DEBIT)
        val REPAID = Cat("repaid", "↩️", "उधार चुकाया", "Repaid", 0xFF64748B.toInt(), TxType.DEBIT)
        val ATM = Cat("atm", "🏧", "ATM/नकद निकाला", "ATM cash out", 0xFF475569.toInt(), TxType.DEBIT)
        val OUT_OTHER = Cat("out_other", "➖", "अन्य", "Other", 0xFF94A3B8.toInt(), TxType.DEBIT)

        val SALE = Cat("sale", "💰", "बिक्री", "Sale", 0xFF16A34A.toInt(), TxType.CREDIT)
        val SALARY = Cat("salary", "💼", "सैलरी", "Salary", 0xFF2563EB.toInt(), TxType.CREDIT)
        val BACK = Cat("back", "↩️", "उधार वापस मिला", "Got back", 0xFF0891B2.toInt(), TxType.CREDIT)
        val BORROW = Cat("borrow", "🤝", "उधार लिया", "Borrowed", 0xFF9333EA.toInt(), TxType.CREDIT)
        val IN_OTHER = Cat("in_other", "➕", "अन्य", "Other", 0xFF94A3B8.toInt(), TxType.CREDIT)

        private val builtins = listOf(
            FOOD, GROCERY, STOCK, TRAVEL, BILL, MEDICAL, HOME, SHOP, LENT, REPAID, ATM, OUT_OTHER,
            SALE, SALARY, BACK, BORROW, IN_OTHER
        )
        private val palette = listOf(
            0xFF0D9488, 0xFFD97706, 0xFF4F46E5, 0xFFBE185D, 0xFF65A30D, 0xFF0284C7, 0xFFB91C1C, 0xFF7E22CE
        ).map { it.toInt() }

        private var custom: List<Cat> = emptyList()

        val customList: List<Cat> get() = custom
        fun all(): List<Cat> = builtins + custom
        fun byKey(k: String): Cat? = all().firstOrNull { it.key == k }

        /** Built-ins, then the user's own, with "Other" always last. */
        fun forType(t: TxType): List<Cat> = all().filter { it.type == t }.sortedBy { if (it.key.endsWith("_other")) 1 else 0 }
        fun other(t: TxType): Cat = if (t == TxType.DEBIT) OUT_OTHER else IN_OTHER

        private fun prefs(ctx: Context) = ctx.getSharedPreferences("kharcha_cats", Context.MODE_PRIVATE)

        fun loadCustom(ctx: Context) {
            val s = prefs(ctx).getString("list", "") ?: ""
            custom = s.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val p = line.split("|")
                if (p.size < 5) null
                else Cat(p[0], p[1], p[2], p[2], p[3].toIntOrNull() ?: 0xFF94A3B8.toInt(),
                    if (p[4] == "0") TxType.DEBIT else TxType.CREDIT)
            }
        }

        private fun saveCustom(ctx: Context) {
            prefs(ctx).edit().putString(
                "list", custom.joinToString("\n") { "${it.key}|${it.emoji}|${it.hi}|${it.color}|${it.type.ordinal}" }
            ).apply()
        }

        fun addCustom(ctx: Context, name: String, emoji: String, type: TxType): Cat {
            val clean = name.replace("|", " ").replace("\n", " ").trim()
            val c = Cat("c_${System.currentTimeMillis()}", emoji, clean, clean, palette[custom.size % palette.size], type)
            custom = custom + c
            saveCustom(ctx)
            return c
        }

        fun removeCustom(ctx: Context, key: String) {
            custom = custom.filter { it.key != key }
            saveCustom(ctx)
        }
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
