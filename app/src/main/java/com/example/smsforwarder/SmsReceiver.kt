
package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val p = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (!p.getBoolean("auto", false)) return

        val token = p.getString("token", "") ?: ""
        val url = p.getString("url", "") ?: ""
        val senderFilter = p.getString("sender", "") ?: ""

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in msgs) {
            val from = msg.displayOriginatingAddress ?: ""
            val body = msg.displayMessageBody ?: ""
            if (senderFilter.isNotBlank() && !from.contains(senderFilter, ignoreCase = true)) continue
            val id = System.currentTimeMillis().toString()
            Webhook.send(url, token, from, body, id, context)
        }
    }
}
