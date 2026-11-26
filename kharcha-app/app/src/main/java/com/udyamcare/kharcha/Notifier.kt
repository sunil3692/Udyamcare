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
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(context, t.id.toInt(), PopupActivity.intent(context, t.id), flags)

        val debit = t.type == TxType.DEBIT
        val title = if (debit) "${formatRupees(t.amountPaise)} निकले (Paid)" else "${formatRupees(t.amountPaise)} आए (Received)"
        val who = if (t.party.isNotBlank()) t.party else "?"
        val text = (if (debit) "किसको / To: " else "किससे / From: ") + who + "  — टैप करो / tap"

        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL)
        else @Suppress("DEPRECATION") Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)

        b.setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE)

        // One-tap quick save buttons with the two most common categories.
        categoriesFor(context, t.type).take(2).forEachIndexed { i, label ->
            val i2 = Intent(context, QuickActionReceiver::class.java)
                .putExtra("id", t.id).putExtra("cat", i)
            val pi = PendingIntent.getBroadcast(context, (t.id * 10 + i).toInt(), i2, flags)
            b.addAction(Notification.Action.Builder(null, label.substringBefore(' '), pi).build())
        }

        nm.notify(t.id.toInt(), b.build())
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(id.toInt())
    }
}
