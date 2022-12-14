package com.mindyhsu.minmap.data

import com.google.firebase.Timestamp

data class Message(
    var id: String = "",
    val senderId: String = "",
    val text: String = "",
    val time: Timestamp? = null
)
