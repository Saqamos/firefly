package org.traintoperform.smsforward.data

data class MessageItem(
    val id: String,
    val ts: Long,
    val title: String,
    val text: String,
    var selected: Boolean = false,
    var forwarded: Boolean = false
)
