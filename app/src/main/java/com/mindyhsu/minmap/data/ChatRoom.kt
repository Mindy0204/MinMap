package com.mindyhsu.minmap.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatRoom(
    val eventId: String = "",
    val id: String = "",
    var participants: List<String> = emptyList(),
    var lastUpdate: Timestamp? = null,
    var lastMessage: String = ""
) : Parcelable {
    var users: List<User> = emptyList()
}
