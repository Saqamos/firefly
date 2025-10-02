package com.saqamos.fireflysms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val prefs = context.getSharedPreferences("ff", Context.MODE_PRIVATE)
        val auto = prefs.getBoolean("auto", false)
        val url = prefs.getString("url", "") ?: ""
        val token = prefs.getString("token", "")

        if (!auto || url.isBlank()) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val full = msgs.joinToString("") { it.messageBody }
        val from = msgs.firstOrNull()?.originatingAddress ?: "unknown"
        val id = System.currentTimeMillis().toString()

        if (!from.contains("HSBC", true) && !full.contains("HSBC", true)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                WebhookClient.send(url, token, from, id, full)
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Forward failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
