
package com.example.smsforwarder

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var urlEdit: EditText
    private lateinit var tokenEdit: EditText
    private lateinit var senderFilterEdit: EditText
    private lateinit var autoSwitch: Switch
    private lateinit var saveBtn: Button
    private lateinit var sendSelectedBtn: Button
    private lateinit var list: RecyclerView
    private val adapter = SmsAdapter()

    private val perms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEdit = findViewById(R.id.webhookUrl)
        tokenEdit = findViewById(R.id.urlToken)
        senderFilterEdit = findViewById(R.id.senderFilter)
        autoSwitch = findViewById(R.id.autoSwitch)
        saveBtn = findViewById(R.id.saveBtn)
        sendSelectedBtn = findViewById(R.id.sendSelectedBtn)
        list = findViewById(R.id.smsList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        // load prefs
        val p = getSharedPreferences("prefs", MODE_PRIVATE)
        urlEdit.setText(p.getString("url", ""))
        tokenEdit.setText(p.getString("token", ""))
        senderFilterEdit.setText(p.getString("sender", ""))
        autoSwitch.isChecked = p.getBoolean("auto", false)

        saveBtn.setOnClickListener {
            p.edit().apply {
                putString("url", urlEdit.text.toString().trim())
                putString("token", tokenEdit.text.toString().trim())
                putString("sender", senderFilterEdit.text.toString().trim())
                putBoolean("auto", autoSwitch.isChecked)
            }.apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        sendSelectedBtn.setOnClickListener {
            val items = adapter.getSelected()
            val url = urlEdit.text.toString().trim()
            val token = tokenEdit.text.toString().trim()
            for (sms in items) {
                Webhook.send(url, token, sms.address, sms.body, "manual-${sms.id}", this)
            }
            Toast.makeText(this, "Sent "+items.size+" message(s)", Toast.LENGTH_SHORT).show()
        }

        requestPerms()
        loadSms()
    }

    private fun requestPerms() {
        val needed = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_SMS)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECEIVE_SMS)
        }
        if (needed.isNotEmpty()) perms.launch(needed.toTypedArray())
    }

    private fun loadSms() {
        val cr: ContentResolver = contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val cursor = cr.query(uri, arrayOf("_id","address","date","body"), null, null, "date DESC LIMIT 100")
        val items = mutableListOf<SmsItem>()
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val address = it.getString(1) ?: ""
                val body = it.getString(3) ?: ""
                items.add(SmsItem(id, address, body))
            }
        }
        adapter.submit(items)
    }
}

data class SmsItem(val id: Long, val address: String, val body: String, var checked:Boolean=false)

class SmsAdapter: RecyclerView.Adapter<SmsVH>() {
    private val items = mutableListOf<SmsItem>()
    fun submit(list: List<SmsItem>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    fun getSelected(): List<SmsItem> = items.filter { it.checked }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SmsVH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false) as CheckedTextView
        return SmsVH(v)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: SmsVH, position: Int) {
        val sms = items[position]
        holder.view.text = "${sms.address}: ${sms.body.take(120)}"
        holder.view.isChecked = sms.checked
        holder.view.setOnClickListener {
            sms.checked = !sms.checked
            holder.view.isChecked = sms.checked
        }
    }
}
class SmsVH(val view: CheckedTextView): RecyclerView.ViewHolder(view)

object Webhook {
    private val client = OkHttpClient()

    fun send(url: String, token: String, from: String, message: String, smsId: String, ctx: android.content.Context) {
        if (url.isBlank()) return
        val obj = JSONObject()
        obj.put("from", from)
        obj.put("sms_id", smsId)
        obj.put("message", message)
        val u = if (token.isNotBlank()) {
            if (url.contains("?")) "$url&token=$token" else "$url?token=$token"
        } else url
        val body = obj.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder().url(u).post(body).build()
        client.newCall(req).enqueue(object: okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }
}
