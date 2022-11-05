package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Message

class DialogViewModel(private val chatRoomDetail: ChatRoom) : ViewModel() {
    private val db = Firebase.firestore

    private val selfName = "Mindy"
    private val users = chatRoomDetail.users.filter { it.name != selfName }
    var roomTitle = ""

    private val _dialogs = MutableLiveData<List<DialogItem>>()
    val dialogs: LiveData<List<DialogItem>>
        get() = _dialogs

    init {
        getTitleName()
        getDialogs()
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

    private fun getDialogs() {
        val dataList = mutableListOf<DialogItem>()
        db.collection("chatRooms").document(chatRoomDetail.id).collection("messages")
            .orderBy("time")
            .get().addOnSuccessListener { dialogs ->
                for (dialog in dialogs) {
                    val data = dialog.toObject(Message::class.java)
                    if (dialog.data["senderId"] != "D7uCAaCvEsUSM5hl5yeK") {
                        dataList.add(DialogItem.FriendDialog(data))
                    } else {
                        dataList.add(DialogItem.MyDialog(data))
                    }
                }
                _dialogs.value = dataList
            }
    }
}