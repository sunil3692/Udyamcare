package com.udyamcare.kharcha

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Notifier {
    private const val CHANNEL = "payments"

    fun show(context: Context, t: Txn) {
        L.load(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, tr("पेमेंट अलर्ट", "Payment alerts"), NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(context, t.id.toInt(), PopupActivity.intent(context, t.id), flags)

        val debit = t.type == TxType.DEBIT
        val title = if (debit) tr("${formatRupees(t.amountPaise)} गए", "${formatRupees(t.amountPaise)} paid")
        else tr("${formatRupees(t.amountPaise)} आए", "${formatRupees(t.amountPaise)} received")
        val shown = Alias.get(context, t.party) ?: t.party
        val who = if (shown.isNotBlank()) shown else "?"
        val text = (if (debit) tr("किसको: ", "To: ") else tr("किससे: ", "From: ")) + who +
            tr("  — टैप करके बताओ", "  — tap to answer")

        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL)
        else @Suppress("DEPRECATION") Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)

        b.setSmallIcon(R.drawable.ic_stat)
            .setColor(C.PRIMARY)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE)

        // Quick-save buttons: same category as last time with this person first, then common ones.
        val db = Db(context)
        val suggested = db.lastCategory(shown, t.type)?.let { Cat.byKey(it) }
        val quick = (listOfNotNull(suggested) + Cat.forType(t.type)).distinct().take(2)
        quick.forEachIndexed { i, cat ->
            val i2 = Intent(context, QuickActionReceiver::class.java)
                .putExtra("id", t.id).putExtra("cat", cat.key)
            val pi = PendingIntent.getBroadcast(context, (t.id * 10 + i).toInt(), i2, flags)
            b.addAction(Notification.Action.Builder(null, cat.label, pi).build())
        }

        nm.notify(t.id.toInt(), b.build())
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(id.toInt())
    }
}
