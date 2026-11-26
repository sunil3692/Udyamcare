package com.udyamcare.kharcha

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

fun formatRupees(paise: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
        maximumFractionDigits = 2
    }
    return "₹" + nf.format(paise / 100.0)
}

fun categoriesFor(context: Context, type: TxType): Array<String> =
    context.resources.getStringArray(if (type == TxType.DEBIT) R.array.cat_debit else R.array.cat_credit)

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
