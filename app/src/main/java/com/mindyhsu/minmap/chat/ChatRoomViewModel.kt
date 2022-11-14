package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ChatRoomUiState(
    val onClick: (chatRoomId: String) -> Unit,
    val roomTitleDisplay: (id: List<String>) -> String
)

class ChatRoomViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    private val getLiveChatRoom = UserManager.id?.let { repository.getLiveChatRoom(it) }
    val liveChatRoom = getLiveChatRoom?.let { Transformations.map(getLiveChatRoom) { it } }
    private val lastChatRoomLastUpdate = mutableListOf<Timestamp>()
    private val lastChatRoomLastMessage = mutableListOf<String>()

    private val _isUpdate = MutableLiveData<Boolean>()
    val isUpdate: LiveData<Boolean>
        get() = _isUpdate

    private val _chatRoom = MutableLiveData<List<ChatRoom>>()
    val chatRoom: LiveData<List<ChatRoom>>
        get() = _chatRoom

    private val _navigateToDialog = MutableLiveData<ChatRoom?>()
    val navigateToDialog: LiveData<ChatRoom?>
        get() = _navigateToDialog

    private var chatRoomList = MutableLiveData<List<ChatRoom>?>()
    private val usersIds = mutableListOf<String>()
    private val userList = MutableLiveData<List<User>>()

    private val userNameListWithIds = mutableMapOf<String, String>()
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
        getChatRoom()
    }

    private val lastChatRoomParticipants = mutableListOf<List<String>>()


    fun getChatRoomLastUpdateChange() {
        liveChatRoom?.value?.let {
            lastChatRoomLastUpdate.clear()
            lastChatRoomLastMessage.clear()
            for (liveChatRoom in it) {
                liveChatRoom.lastUpdate?.let { time -> lastChatRoomLastUpdate.add(time) }
                lastChatRoomLastMessage.add(liveChatRoom.lastMessage)
            }

            _chatRoom.value?.let {
                for ((index, chatRoom) in it.withIndex()) {
                    chatRoom.lastUpdate = lastChatRoomLastUpdate[index]
                    chatRoom.lastMessage = lastChatRoomLastMessage[index]
                }
                _isUpdate.value = true
            }
        }
    }

    fun getChatRoomParticipantsChange() {
        liveChatRoom?.value?.let {
            lastChatRoomParticipants.clear()
            val updateUsersIds = mutableListOf<String>()

            for (liveChatRoom in it) {
                lastChatRoomParticipants.add(liveChatRoom.participants)
            }

            _chatRoom.value?.let {
                for ((index, chatRoom) in it.withIndex()) {
                    chatRoom.participants = lastChatRoomParticipants[index]
                }

//                _isUpdate.value = true
            }
        }
    }

    private fun getChatRoom() {
        coroutineScope.launch {
            val result = UserManager.id?.let { repository.getChatRoom(it) }
            chatRoomList.value = when (result) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    null
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    null
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    null
                }
            }

            chatRoomList.value?.let {
                for (chatRoom in it) {
                    usersIds.addAll(chatRoom.participants)
                }
            }
            getUsersById(usersIds)
        }
    }

    private fun getUsersById(usersIds: List<String>) {
        coroutineScope.launch {
            val result = repository.getUsersById(usersIds)
            userList.value = when (result) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    null
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    null
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    null
                }
            }

            userList.value?.let {
                for (user in it) {
                    userNameListWithIds[user.id] = user.name
                }
            }

            addNamesInChatRoom()
        }
    }

    private fun addNamesInChatRoom() {
        chatRoomList.value?.let { chatRooms ->
            for (chatRoom in chatRooms) {

                val userData = mutableListOf<User>()
//            Log.d("Minddddddy","chatRoom=${chatRoom}, participants=${chatRoom.participants}")

                for (participant in chatRoom.participants) {

                    userList.value?.let { users ->
                        for (user in users) {
                            if (participant == user.id) {
                                userData.add(user)
                            }
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
        _chatRoom.value = chatRoomListWithUser
    }

    fun completeNavigateToDialog() {
        _navigateToDialog.value = null
    }
}