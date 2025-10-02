package org.traintoperform.smsforward

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.traintoperform.smsforward.data.MessageItem
import org.traintoperform.smsforward.data.Store
import java.security.MessageDigest

class SmsNotifListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            val looksLikeSmsApp = pkg.contains("messag", true) || pkg.contains("sms", true)
            if (!looksLikeSmsApp) return

            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString().orEmpty()
            val text = extras.getCharSequence("android.text")?.toString().orEmpty()
            val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
            val body = listOf(bigText, text).maxByOrNull { it.length } ?: ""
            if (body.isBlank()) return

            val msg = MessageItem(
                id = hash8(body),
                ts = System.currentTimeMillis(),
                title = if (title.isBlank()) "SMS" else title,
                text = body
            )
            val list = Store.loadList(this)
            list.add(msg)
            Store.saveList(this, list)
            sendBroadcast(Intent("org.traintoperform.smsforward.REFRESH"))
        } catch (t: Throwable) { Log.e("SMSFWD", "onNotificationPosted error", t) }
    }
    private fun hash8(s: String): String {
        val sha = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return sha.take(8).joinToString("") { "%02x".format(it) }
    }
}
