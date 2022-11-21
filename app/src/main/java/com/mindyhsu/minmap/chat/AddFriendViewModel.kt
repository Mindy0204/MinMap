package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class AddFriendViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    var friendId = ""

    private val _friend = MutableLiveData<User>()
    val friend: LiveData<User>
        get() = _friend

    private val _hasThisFriend = MutableLiveData<Boolean>(false)
    val hasThisFriend: LiveData<Boolean>
        get() = _hasThisFriend

    fun getUserById(friend: String) {
        coroutineScope.launch {
            friendId = friend
            status.value = LoadApiStatus.LOADING

            // Check if has this friend
            var friendList =
                when (val myFriendListResult = repository.getFriend(UserManager.id ?: "")) {
                    is Result.Success -> {
                        error.value = null
                        status.value = LoadApiStatus.DONE
                        myFriendListResult.data
                    }
                    is Result.Fail -> {
                        error.value = myFriendListResult.error
                        status.value = LoadApiStatus.ERROR
                        emptyList()
                    }
                    is Result.Error -> {
                        error.value = myFriendListResult.exception.toString()
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

            for (friend in friendList) {
                if (friend == friendId) {
                    _hasThisFriend.value = true
                }
            }

            if (!_hasThisFriend.value!!) {
                val user = when (val result = repository.getUserById(listOf(friend))) {
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
                _friend.value = user[0]
            }
        }
    }

    fun setFriend() {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            val newChatRoom = when (val result =
                _friend.value?.let { repository.setFriend(UserManager.id ?: "", it.id) }) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    ""
                }
            }
            if (newChatRoom != "") {
                setFirstMessage(newChatRoom)
            }
        }
    }

    private fun setFirstMessage(chatRoomId: String) {
        coroutineScope.launch {
            val time = Timestamp(Calendar.getInstance().time)
            val message = Message(
                senderId = friendId,
                text = MinMapApplication.instance.getString(R.string.add_friend_success_message),
                time = time
            )

            status.value = LoadApiStatus.LOADING

            when (val result =
                repository.sendMessage(chatRoomId = chatRoomId, message = message)) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                }
                else -> {
                    error.value =
                        MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                }
            }
        }
    }
}