package com.saqamos.fireflysms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WebhookClient {
    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun send(url: String, token: String?, from: String, id: String?, message: String) =
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("from", from)
                put("sms_id", id ?: "")
                put("message", message)
            }.toString()
            val final = if (!token.isNullOrBlank()) {
                val t = URLEncoder.encode(token, "UTF-8")
                if (url.contains("?")) "$url&token=$t" else "$url?token=$t"
            } else url
            val req = Request.Builder()
                .url(final)
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()
            val resp = client.newCall(req).execute()
            val code = resp.code
            resp.close()
            code
        }
}
