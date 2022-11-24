package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import timber.log.Timber

data class ChatRoomUiState(
    val onClick: (chatRoomId: String) -> Unit,
    val roomTitleDisplay: (id: List<String>) -> String,
    val roomMessageDisplay: (message: String) -> String,
    val roomPicDisplay: (image: List<User>) -> String
)

private const val groupPic =
    "https://memeprod.ap-south-1.linodeobjects.com/user-template/596519cf22dbc8cc2506add9a4a7d3c1.png"

class ChatRoomViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    val getLiveChatRoom = repository.getLiveChatRoom(UserManager.id ?: "")
    val liveChatRoom = MutableLiveData<List<ChatRoom>>()

    private val _navigateToDialog = MutableLiveData<ChatRoom?>()
    val navigateToDialog: LiveData<ChatRoom?>
        get() = _navigateToDialog

    private val usersIds = mutableListOf<String>()
    private var userList = emptyList<User>()

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
        },
        roomMessageDisplay = { message ->
            val messageLength = 15
            if (message.length > messageLength) {
                message.take(messageLength) + "..."
            } else {
                message
            }
        },
        roomPicDisplay = {
            val users = it.filter { it.id != UserManager.id }
            val userImageList = mutableSetOf<String>()
            for (user in users) {
                userImageList.add(user.image)
            }

            // If there's only one friend in this chatroom, show him/ her pic
            return@ChatRoomUiState if (userImageList.size == 1) {
                userImageList.first()
            } else {
                groupPic
            }
        }
    )

    fun checkUsersExist(chatRooms: List<ChatRoom>) {
        // Current chatRoom participants' ids
        for (chatRoom in chatRooms) {
            usersIds.addAll(chatRoom.participants)
        }

        coroutineScope.launch {
            // Get users by ids
            val result = repository.getUserById(usersIds)
            userList = when (result) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
            }

            // Add users into listMapOf<id, name>
            for (user in userList) {
                userNameListWithIds[user.id] = user.name
            }

            // Add participants' user data into chatRooms
            for (chatRoom in chatRooms) {
                val userData = mutableListOf<User>()
                for (participant in chatRoom.participants) {
                    userList.let { users ->
                        for (user in users) {
                            if (participant == user.id) {
                                userData.add(user)
                            }
                        }
                    }
                }
                chatRoom.users = userData
                chatRoomListWithUser.add(chatRoom)
            }
            liveChatRoom.value = chatRooms
        }
    }

    fun completeNavigateToDialog() {
        _navigateToDialog.value = null
    }
}