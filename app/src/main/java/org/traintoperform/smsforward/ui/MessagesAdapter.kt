package org.traintoperform.smsforward.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.traintoperform.smsforward.R
import org.traintoperform.smsforward.data.MessageItem
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(private val onChange: () -> Unit) : RecyclerView.Adapter<MessagesAdapter.VH>() {

    val current = mutableListOf<MessageItem>()
    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun submit(list: MutableList<MessageItem>) {
        current.clear()
        current.addAll(list)
        notifyDataSetChanged()
    }

    fun selectAll() { current.forEach { it.selected = true }; notifyDataSetChanged(); onChange() }
    fun selectNone() { current.forEach { it.selected = false }; notifyDataSetChanged(); onChange() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = current[pos]
        h.cb.setOnCheckedChangeListener(null)
        h.cb.isChecked = m.selected
        h.cb.setOnCheckedChangeListener { _, b -> m.selected = b; onChange() }
        h.tvTitle.text = m.title + if (m.forwarded) "  •  ✅ sent" else ""
        h.tvTime.text = df.format(Date(m.ts))
        h.tvBody.text = m.text
        h.itemView.setOnClickListener { m.selected = !m.selected; notifyItemChanged(pos); onChange() }
    }

    override fun getItemCount(): Int = current.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cb: CheckBox = v.findViewById(R.id.cb)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
        val tvBody: TextView = v.findViewById(R.id.tvBody)
    }
}
