package com.udyamcare.kharcha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (msgs.isEmpty()) return
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        val parsed = SmsParser.parse(body) ?: return

        val db = Db(context)
        val id = db.insertIfNew(parsed, body, System.currentTimeMillis())
        if (id > 0) db.get(id)?.let { Notifier.show(context, it) }
    }
}

class QuickActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1)
        val idx = intent.getIntExtra("cat", 0)
        val db = Db(context)
        val t = db.get(id) ?: return
        val cats = categoriesFor(context, t.type)
        db.save(id, t.party, cats.getOrElse(idx) { cats.last() })
        Notifier.cancel(context, id)
    }
}
