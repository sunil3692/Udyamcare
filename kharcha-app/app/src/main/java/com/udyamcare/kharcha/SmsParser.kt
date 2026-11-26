package com.udyamcare.kharcha

import java.math.BigDecimal

enum class TxType { DEBIT, CREDIT }

data class ParsedTx(
    val amountPaise: Long,
    val type: TxType,
    val party: String,
    val ref: String?,
)

/** Pure Kotlin (no Android classes) so it can be unit-tested on the JVM. */
object SmsParser {

    private val amountRe = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val debitRe = Regex("""(?i)\b(debited|spent|sent|paid|withdrawn|purchase)\b""")
    private val creditRe = Regex("""(?i)\b(credited|received|deposited)\b""")
    private val skipRe = Regex(
        """(?i)\b(otp|one time password|collect request|requested|will be debited|will be credited|""" +
            """pre-?approved|apply now|offer|cashback offer|due on|due by|unsuccessful|failed|declined)\b"""
    )
    private val balanceHintRe = Regex("""(?i)(bal|avl|available|limit|outstanding)[a-z. :]*$""")

    private val vpaRe = Regex("""(?i)\b(to|from|by)\s+(?:vpa\s+)?([a-z0-9._-]{2,}@[a-z0-9]{2,})""")
    private val nameRe = Regex(
        """(?i)\b(to|from|by|at)\s+(?!a/c\b|acct\b|account\b|your\b|vpa\b|card\b|xx|\d|bank\b|the\b)""" +
            """([a-z][a-z0-9 .&'-]{1,30}?)""" +
            """(?=\s+(?:on|via|using|ref|upi|txn|trf|avl|bal|through|is|has|dated|from|for)\b|[.,(;]|\s+-|$)"""
    )
    private val refRe = Regex(
        """(?i)(?:ref(?:erence)?\.?\s*(?:no\.?|number|id)?|rrn|txn\s*(?:id|no)|utr)\s*[:#.\-]?\s*([a-z0-9]{8,})"""
    )

    fun parse(body: String): ParsedTx? {
        val text = body.replace('\n', ' ').replace(Regex("""\s+"""), " ").trim()
        if (text.isEmpty() || skipRe.containsMatchIn(text)) return null

        val amount = amountRe.findAll(text).firstOrNull { m ->
            val before = text.substring(maxOf(0, m.range.first - 16), m.range.first)
            !balanceHintRe.containsMatchIn(before)
        } ?: return null

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
            BigDecimal(amount.groupValues[1].replace(",", "")).movePointRight(2).toLong()
        } catch (e: NumberFormatException) {
            return null
        }
        if (paise <= 0) return null

        return ParsedTx(paise, type, extractParty(text, type), extractRef(text))
    }

    private fun extractParty(text: String, type: TxType): String {
        val wanted = if (type == TxType.DEBIT) setOf("to", "at") else setOf("from", "by")
        vpaRe.findAll(text).forEach { m ->
            if (m.groupValues[1].lowercase() in wanted) return m.groupValues[2]
        }
        nameRe.findAll(text).forEach { m ->
            if (m.groupValues[1].lowercase() in wanted) {
                val name = m.groupValues[2].trim().trimEnd('.', '-', ' ')
                if (name.length >= 2 && name.any { it.isLetter() }) return name
            }
        }
        return ""
    }

    private fun extractRef(text: String): String? =
        refRe.findAll(text).map { it.groupValues[1] }.firstOrNull { r -> r.any { it.isDigit() } }
}
