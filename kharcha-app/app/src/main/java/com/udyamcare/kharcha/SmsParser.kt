package com.udyamcare.kharcha

import java.math.BigDecimal

enum class TxType { DEBIT, CREDIT }

data class ParsedTx(
    val amountPaise: Long,
    val type: TxType,
    val party: String,
    val ref: String?,
    val balancePaise: Long? = null,
)

/** Pure Kotlin (no Android classes) so it can be unit-tested on the JVM. */
object SmsParser {

    private val amountRe = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val amountByRe = Regex(
        """(?i)\b(?:debited|credited)\s+(?:by|for|with)\s+(?:rs\.?|inr|₹)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )
    private val infoRe = Regex("""(?i)\bUPI/[A-Za-z0-9]+/\d+/([^/\s][^/]{1,30})/""")
    // "Dr." / "Cr." (Bank of Baroda etc.) only count when followed by from/to/by/at, so "Dr. Sharma" is safe.
    private val debitRe = Regex(
        """(?i)\b(debited|debit of|spent|sent|paid|withdrawn|purchase)\b|\bdr\b\.?(?=\s+(?:from|to|by|at)\b)"""
    )
    private val creditRe = Regex(
        """(?i)\b(credited|credit of|received|deposited)\b|\bcr\b\.?(?=\s+(?:to|from|by)\b)"""
    )
    private val maskedRe = Regex("""(?i)\b(to|from|by)\s+(?:a/c\s+|acct?\.?\s+|account\s+)?(x{2,}\d{3,6})\b""")
    private val skipRe = Regex(
        """(?i)\b(otp|one time password|collect request|requested|will be debited|will be credited|""" +
            """pre-?approved|apply now|offer|cashback offer|due on|due by|unsuccessful|failed|declined)\b"""
    )
    private val balanceHintRe = Regex("""(?i)(bal|avl|available|limit|outstanding)[a-z. :]*$""")

    private val vpaRe = Regex("""(?i)\b(to|from|by)\s+(?:vpa\s+)?([a-z0-9._-]{2,}@[a-z0-9]{2,})""")
    private val nameRe = Regex(
        """(?i)\b(to|from|by|at)\s+(?!a/c\b|acct\b|account\b|your\b|vpa\b|card\b|xx|\d|bank\b|the\b)""" +
            """([a-z][a-z0-9 .&'-]{1,30}?)""" +
            """(?=\s+(?:on|via|using|ref\w*|upi|txn|trf|avl|bal|through|is|has|dated|from|for)\b|[.,(;]|\s+-|$)"""
    )
    private val balRe = Regex(
        """(?i)\b(?:avl\.?\s*bal(?:ance)?|available\s+bal(?:ance)?|bal(?:ance)?)\s*[:\-]?\s*(?:rs\.?|inr|₹)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )
    private val refRe = Regex(
        """(?i)(?:ref(?:erence)?\.?\s*(?:no\.?|number|id)?|rrn|txn\s*(?:id|no)|utr)\s*[:#.\-]?\s*([a-z0-9]{8,})"""
    )

    fun parse(body: String): ParsedTx? {
        val text = body.replace('\n', ' ').replace(Regex("""\s+"""), " ").trim()
        if (text.isEmpty() || skipRe.containsMatchIn(text)) return null

        val amountText = (amountRe.findAll(text).firstOrNull { m ->
            val before = text.substring(maxOf(0, m.range.first - 16), m.range.first)
            !balanceHintRe.containsMatchIn(before)
        } ?: amountByRe.find(text))?.groupValues?.get(1) ?: return null

        val dm = debitRe.find(text)
        val cm = creditRe.find(text)
        val type = when {
            dm == null && cm == null -> return null
            dm == null -> TxType.CREDIT
            cm == null -> TxType.DEBIT
            dm.range.first < cm.range.first -> TxType.DEBIT
            else -> TxType.CREDIT
        }

        val paise = try {
            BigDecimal(amountText.replace(",", "")).movePointRight(2).toLong()
        } catch (e: NumberFormatException) {
            return null
        }
        if (paise <= 0) return null

        return ParsedTx(paise, type, extractParty(text, type), extractRef(text), extractBalance(text))
    }

    private fun extractParty(text: String, type: TxType): String {
        val wanted = if (type == TxType.DEBIT) setOf("to", "at") else setOf("from", "by")
        infoRe.find(text)?.let { m ->
            val n = m.groupValues[1].trim()
            if (n.any { it.isLetter() }) return n
        }
        vpaRe.findAll(text).forEach { m ->
            if (m.groupValues[1].lowercase() in wanted) return m.groupValues[2]
        }
        nameRe.findAll(text).forEach { m ->
            if (m.groupValues[1].lowercase() in wanted) {
                val name = m.groupValues[2].trim().trimEnd('.', '-', ' ')
                if (name.length >= 2 && name.any { it.isLetter() }) return name
            }
        }
        maskedRe.findAll(text).forEach { m ->
            if (m.groupValues[1].lowercase() in wanted) return "A/C " + m.groupValues[2].uppercase()
        }
        return ""
    }

    private fun extractBalance(text: String): Long? {
        val g = balRe.find(text)?.groupValues?.get(1) ?: return null
        return try {
            BigDecimal(g.replace(",", "")).movePointRight(2).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun extractRef(text: String): String? =
        refRe.findAll(text).map { it.groupValues[1] }.firstOrNull { r -> r.any { it.isDigit() } }
}
