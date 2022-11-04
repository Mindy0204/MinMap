package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Message

data class ChatRoomUiState(
    val onClick: (position: Int) -> Unit,
    val roomTitleDisplay: (id: List<String>) -> String
)

class ChatRoomViewModel : ViewModel() {
    private val db = Firebase.firestore

    private val _chatRoom = MutableLiveData<List<ChatRoom>>()
    val chatRoom: LiveData<List<ChatRoom>>
        get() = _chatRoom

    private val _navigateToDialog = MutableLiveData<List<Message>?>()
    val navigateToDialog: LiveData<List<Message>?>
        get() = _navigateToDialog

    private val userNames = mutableMapOf<String, String>()

    val uiState = ChatRoomUiState(
        onClick = { position ->

        },
        roomTitleDisplay = {
            val selfId = "D7uCAaCvEsUSM5hl5yeK"
            val ids = it.filter { it != selfId }
            var nameDisplay = ""

            for ((index, id) in ids.withIndex()) {
                if (index != 0) {
                    nameDisplay += ", "
                }
                nameDisplay += userNames[id]
            }
            return@ChatRoomUiState nameDisplay
        }
    )

    init {
        getMyChatRoomList()
    }

    private fun getMyChatRoomList() {
        val dataList = mutableListOf<ChatRoom>()

        db.collection("chatRooms").whereArrayContains("participants", "D7uCAaCvEsUSM5hl5yeK")
            .get().addOnSuccessListener { documents ->

                val allUserIds = mutableListOf<String>()
                for (chatRoom in documents) {
                    val data = chatRoom.toObject(ChatRoom::class.java)
                    dataList.add(data)
                    allUserIds.addAll(chatRoom.data["participants"] as List<String>)
                }

                db.collection("users").whereIn("id", allUserIds)
                    .get().addOnSuccessListener { allUsers ->
                        for (user in allUsers) {
                            userNames[user.data["id"] as String] = user.data["name"] as String
                        }
                        _chatRoom.value = dataList
                    }
            }
    }

    fun displayDialog(message: List<Message>) {
        _navigateToDialog.value = message
    }

    fun displayDialogComplete() {
        _navigateToDialog.value = null
    }
}