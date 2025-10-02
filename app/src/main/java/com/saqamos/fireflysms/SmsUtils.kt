package com.saqamos.fireflysms

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony

data class SmsRow(val id: String, val address: String, val body: String, val date: Long)

fun queryLastSms(cr: ContentResolver, max: Int = 50): List<SmsRow> {
    val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
    val cols = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
    val out = mutableListOf<SmsRow>()
    cr.query(uri, cols, null, null, "${Telephony.Sms.DATE} DESC LIMIT $max")?.use { c ->
        val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
        val iAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
        while (c.moveToNext()) {
            out += SmsRow(
                c.getString(iId),
                c.getString(iAddr) ?: "",
                c.getString(iBody) ?: "",
                c.getLong(iDate)
            )
        }
    }
    return out
}
