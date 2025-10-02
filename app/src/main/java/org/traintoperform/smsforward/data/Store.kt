package org.traintoperform.smsforward.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Store {
    private const val PREF = "sms_store_manual"
    private const val KEY_LIST = "list"
    private const val KEY_WEBHOOK = "webhook"
    private val gson = Gson()
    private val T = object : TypeToken<MutableList<MessageItem>>() {}.type

    private const val DEFAULT_WEBHOOK = "https://script.google.com/macros/s/AKfycbzNJFGWn8HWi-y630IeDbDpDQr2lOpViIC-QmIQg6lAZC9sYpClCRiYQhzuxWa4ic7Oyg/exec?token=Saqa%401122"

    fun loadList(ctx: Context): MutableList<MessageItem> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_LIST, "[]")
        return try { gson.fromJson(json, T) } catch (_: Throwable) { mutableListOf() }
    }

    fun saveList(ctx: Context, list: List<MessageItem>) {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LIST, gson.toJson(list.takeLast(200))).apply()
    }

    fun getWebhook(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getString(KEY_WEBHOOK, DEFAULT_WEBHOOK) ?: DEFAULT_WEBHOOK
    }
    fun setWebhook(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_WEBHOOK, url.trim()).apply()
    }

    fun prettyWebhook(ctx: Context): String {
        return try {
            val u = getWebhook(ctx)
            val uri = Uri.parse(u)
            "${uri.scheme}://${uri.host}${(uri.path ?: "").split('/').take(4).joinToString("/") }…"
        } catch (_: Throwable) { getWebhook(ctx) }
    }
}
