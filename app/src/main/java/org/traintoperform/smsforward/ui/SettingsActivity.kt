package org.traintoperform.smsforward.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.traintoperform.smsforward.R
import org.traintoperform.smsforward.data.Store
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

class SettingsActivity : ComponentActivity() {
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }
    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etWebhook = findViewById<EditText>(R.id.etWebhook)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val tvHint = findViewById<TextView>(R.id.tvHint)

        etWebhook.setText(Store.getWebhook(this))
        tvHint.text = "Example: https://script.google.com/macros/s/.../exec?token=Saqa%401122"

        btnSave.setOnClickListener {
            val url = etWebhook.text.toString().trim()
            if (!url.startsWith("http")) {
                Snackbar.make(etWebhook, "Invalid URL", Snackbar.LENGTH_LONG).show(); return@setOnClickListener
            }
            Store.setWebhook(this, url)
            Snackbar.make(etWebhook, "Saved", Snackbar.LENGTH_SHORT).show()
        }

        btnTest.setOnClickListener {
            val url = etWebhook.text.toString().trim()
            val body = gson.toJson(mapOf(
                "from" to "Settings Test",
                "sms_id" to "test-" + System.currentTimeMillis().toString().takeLast(6),
                "message" to "This is a test from the manual app."
            )).toRequestBody(json)
            val req = Request.Builder().url(url).post(body).addHeader("Accept","application/json").build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) Snackbar.make(etWebhook, "Test sent ✔", Snackbar.LENGTH_LONG).show()
                    else Snackbar.make(etWebhook, "Test failed: HTTP " + resp.code, Snackbar.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                Snackbar.make(etWebhook, "Test failed: " + t.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
