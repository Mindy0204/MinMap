package com.mindyhsu.minmap.chat

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.ChatRoom

class DialogViewModel(private val chatRoomDetail: ChatRoom) : ViewModel() {
    private val db = Firebase.firestore

    private val selfName = "Mindy"
    private val users = chatRoomDetail.users.filter { it.name != selfName }
    var roomTitle = ""

    //        val chatList = mutableListOf<Message>()
//        val message1 = Message(id = "GnGMAzxQq3xLrBpYA6rP", senderId = "Wayne", text = "Hi, how are you")
//        val message2 = Message(id = "JvotWfs0w81xQyBUNC1s", senderId = "Mindy", text = "i'm fine, thank you, and you")
//        chatList.add(message1)
//        chatList.add(message2)
//        adapter.submitList(chatList)

    init {
        getTitleName()
        getMessage()
    }

    private fun getTitleName() {
        for ((index, user) in users.withIndex()) {
            for (participant in chatRoomDetail.participants) {
                if (user.id == participant) {
                    if (index != 0) {
                        roomTitle += ", "
                    }
                }
            }
            roomTitle += user.name
        }
    }

    private fun getMessage() {

    }
}