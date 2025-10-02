package org.traintoperform.smsforward.ui

import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.traintoperform.smsforward.R
import org.traintoperform.smsforward.data.MessageItem
import org.traintoperform.smsforward.data.Store
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }
    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var btnForward: Button
    private lateinit var btnSelectAll: Button
    private lateinit var btnSelectNone: Button
    private lateinit var tvWebhook: TextView

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { reload() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv = findViewById(R.id.rvMessages)
        btnForward = findViewById(R.id.btnForwardSelected)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnSelectNone = findViewById(R.id.btnSelectNone)
        tvWebhook = findViewById(R.id.tvWebhook)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = MessagesAdapter { }
        rv.adapter = adapter

        btnSelectAll.setOnClickListener { adapter.selectAll() }
        btnSelectNone.setOnClickListener { adapter.selectNone() }
        btnForward.setOnClickListener { forwardSelected() }

        registerReceiver(refreshReceiver, IntentFilter("org.traintoperform.smsforward.REFRESH"))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }

    override fun onStart() {
        super.onStart()
        reload()
    }

    private fun reload() {
        tvWebhook.text = "Webhook: " + Store.prettyWebhook(this)
        adapter.submit(Store.loadList(this).sortedByDescending { it.ts }.toMutableList())
    }

    private fun forwardSelected() {
        val selected = adapter.current.filter { it.selected }
        if (selected.isEmpty()) { Toast.makeText(this, "No messages selected", Toast.LENGTH_SHORT).show(); return }
        val url = Store.getWebhook(this)
        var ok = 0; var fail = 0
        selected.forEach { m ->
            try {
                val body = gson.toJson(mapOf("from" to m.title, "sms_id" to m.id, "message" to m.text)).toRequestBody(json)
                val req = Request.Builder().url(url).post(body).addHeader("Accept","application/json").build()
                client.newCall(req).execute().use { resp -> if (resp.isSuccessful) { ok++; m.forwarded = true } else fail++ }
            } catch (_: Throwable) { fail++ }
        }
        Store.saveList(this, adapter.current)
        reload()
        Snackbar.make(rv, "Forwarded: $ok, Failed: $fail", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Notification Access")
        menu.add(0, 2, 1, "Settings")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); true }
            2 -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
