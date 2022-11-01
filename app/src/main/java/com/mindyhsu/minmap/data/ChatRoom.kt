package com.mindyhsu.minmap.data

import com.google.firebase.Timestamp

data class ChatRoom(
    val eventId: String,
    val id: String,
    val participants: List<String>,
    val messages: List<Message>
)