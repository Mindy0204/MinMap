package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DialogViewModel(
    private val repository: MinMapRepository,
    private val chatRoomDetail: ChatRoom
) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    private val selfName = UserManager.name
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
        coroutineScope.launch {
            var messages = MutableLiveData<List<Message>>()
            val dataList = mutableListOf<DialogItem>()

            val result = UserManager.id?.let { repository.getMessages(chatRoomDetail.id, it) }
            messages.value = when (result) {
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

            messages.value?.let {
                for (message in it) {
                    if (message.senderId != UserManager.id) {
                        dataList.add(DialogItem.FriendDialog(message))
                    } else {
                        dataList.add(DialogItem.MyDialog(message))
                    }
                }
                _dialogs.value = dataList
            }

        }
    }
}