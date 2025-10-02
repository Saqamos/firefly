package com.saqamos.fireflysms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.saqamos.fireflysms.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val adapter = SmsAdapter()

    private val perms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.rvSms.adapter = adapter

        val p = getSharedPreferences("ff", MODE_PRIVATE)
        b.etUrl.setText(p.getString("url", ""))
        b.etToken.setText(p.getString("token", ""))
        b.cbAuto.isChecked = p.getBoolean("auto", false)

        b.btnSave.setOnClickListener {
            p.edit().putString("url", b.etUrl.text.toString().trim())
                .putString("token", b.etToken.text.toString().trim())
                .putBoolean("auto", b.cbAuto.isChecked)
                .apply()
        }
        b.btnRefresh.setOnClickListener { ensurePerms { refresh() } }
        b.btnSendSelected.setOnClickListener {
            val url = b.etUrl.text.toString().trim()
            val token = b.etToken.text.toString().trim().ifBlank { null }
            if (url.isBlank()) return@setOnClickListener
            val chosen = adapter.items.filter { it.checked }
            CoroutineScope(Dispatchers.IO).launch {
                chosen.forEach {
                    WebhookClient.send(url, token, it.address, it.id, it.body)
                }
            }
        }
        ensurePerms { refresh() }
    }

    private fun ensurePerms(block: () -> Unit) {
        val needed = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) perms.launch(missing.toTypedArray()) else block()
    }

    private fun refresh() {
        val list = queryLastSms(contentResolver, 50)
        adapter.submit(list)
    }
}

data class SmsSelectable(
    val id: String, val address: String, val body: String, val date: Long, var checked: Boolean = false
)

class SmsAdapter : RecyclerView.Adapter<SmsVH>() {
    val items = mutableListOf<SmsSelectable>()
    fun submit(rows: List<SmsRow>) {
        items.clear(); items.addAll(rows.map { SmsSelectable(it.id, it.address, it.body, it.date) })
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(p: ViewGroup, v: Int): SmsVH {
        val view = LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_multiple_choice, p, false)
        return SmsVH(view as android.widget.CheckedTextView)
    }
    override fun onBindViewHolder(h: SmsVH, i: Int) = h.bind(items[i]) { notifyItemChanged(i) }
    override fun getItemCount() = items.size
}

class SmsVH(private val view: android.widget.CheckedTextView) : RecyclerView.ViewHolder(view) {
    fun bind(row: SmsSelectable, onChanged: () -> Unit) {
        view.text = "[${row.address}] ${row.body.take(120)}"
        view.isChecked = row.checked
        view.setOnClickListener {
            row.checked = !row.checked
            view.isChecked = row.checked
            onChanged()
        }
    }
}
