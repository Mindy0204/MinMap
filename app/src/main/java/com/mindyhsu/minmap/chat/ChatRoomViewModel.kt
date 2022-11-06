package com.mindyhsu.minmap.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.login.UserManager

data class ChatRoomUiState(
    val onClick: (chatRoomId: String) -> Unit,
    val roomTitleDisplay: (id: List<String>) -> String
)

class ChatRoomViewModel : ViewModel() {
    private val db = Firebase.firestore

    private val _chatRoom = MutableLiveData<List<ChatRoom>>()
    val chatRoom: LiveData<List<ChatRoom>>
        get() = _chatRoom

    private val _navigateToDialog = MutableLiveData<ChatRoom>()
    val navigateToDialog: LiveData<ChatRoom>
        get() = _navigateToDialog

    private val userNameListWithIds = mutableMapOf<String, String>()
    private val chatRoomList = mutableListOf<ChatRoom>()
    private val userList = mutableListOf<User>()
    private val chatRoomListWithUser = mutableListOf<ChatRoom>()

    val uiState = ChatRoomUiState(
        onClick = { chatRoomId ->
            _navigateToDialog.value = chatRoomListWithUser.filter { it.id == chatRoomId }[0]
        },
        roomTitleDisplay = {
            val selfId = UserManager.id
            val ids = it.filter { it != selfId }
            var nameDisplay = ""

            for ((index, id) in ids.withIndex()) {
                if (index != 0) {
                    nameDisplay += ", "
                }
                nameDisplay += userNameListWithIds[id]
            }
            return@ChatRoomUiState nameDisplay
        }
    )

    init {
        getMyChatRoomList()
    }

    private fun getMyChatRoomList() {
        var chatRoomData = ChatRoom()
        val usersIds = mutableListOf<String>()

        db.collection("chatRooms").whereArrayContains("participants", UserManager.id)
            .get().addOnSuccessListener { chatRooms ->

                for (chatRoom in chatRooms) {
                    chatRoomData = chatRoom.toObject(ChatRoom::class.java)
                    chatRoomList.add(chatRoomData)
                    usersIds.addAll(chatRoom.data["participants"] as List<String>)
                }

//                Log.d("Minddddddy", "chatRoomList=${chatRoomList}")

                db.collection("users").whereIn("id", usersIds)
                    .get().addOnSuccessListener { users ->

                        for (user in users) {
                            userNameListWithIds[user.data["id"] as String] =
                                user.data["name"] as String
                            val userList = user.toObject(User::class.java)
                            this.userList.add(userList)
                        }
                        _chatRoom.value = chatRoomList

//                        Log.d("Minddddddy", "userList=${userList}")
                        addNamesInChatRoom()
                    }
            }
    }

    private fun addNamesInChatRoom() {
        for (chatRoom in chatRoomList) {

            val userData = mutableListOf<User>()
//            Log.d("Minddddddy","chatRoom=${chatRoom}, participants=${chatRoom.participants}")

            for (participant in chatRoom.participants) {
                for (user in userList) {
                    if (participant == user.id) {
                        userData.add(user)
                    }
                }
            }
//            Log.d("Minddddddy","userData=${userData}")
            chatRoom.users = userData
            chatRoomListWithUser.add(chatRoom)
        }
//        Log.d("Minddddddy", "userData=${chatRoomListWithUser[0].users}")
//        Log.d("Minddddddy", "userData=${chatRoomListWithUser[1].users}")
    }
}