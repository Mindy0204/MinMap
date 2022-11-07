package com.mindyhsu.minmap.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatRoom(
    val eventId: String = "",
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastUpdate: Timestamp? = null,
    val lastMessage: String = ""
): Parcelable {
    var users: List<User> = emptyList()
}