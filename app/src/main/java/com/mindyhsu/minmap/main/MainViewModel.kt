package com.mindyhsu.minmap.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(private val repository: MinMapRepository) : ViewModel() {

    private val _getLiveChatRoom = repository.getLiveChatRoom(UserManager.id ?: "")
    val getLiveChatRoom: LiveData<List<ChatRoom>>
        get() = _getLiveChatRoom

    private val _getChatRoomIds = MutableLiveData<List<String>>()
    val getChatRoomIds: LiveData<List<String>>
        get() = _getChatRoomIds

    private val _foregroundStop = MutableLiveData<Boolean?>()
    val foregroundStop: LiveData<Boolean?>
        get() = _foregroundStop

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    init {
        getFCMToken()
    }

    fun getChatRoomIds(chatRooms: List<ChatRoom>) {
        val chatRoomIds = mutableListOf<String>()
        getLiveChatRoom.value?.let {
            for (chatRoom in chatRooms) {
                chatRoomIds.add(chatRoom.id)
            }
            _getChatRoomIds.value = chatRoomIds
        }
    }

    fun getLiveMessage(chatRoomIds: List<String>) {
        for (chatRoomId in chatRoomIds) {
            repository.getMessage(chatRoomId, UserManager.id ?: "")
        }
    }

    fun stopForegroundUpdate() {
        _foregroundStop.value = true
    }

    fun onForegroundUpdateStopped() {
        _foregroundStop.value = null
    }

    private fun getFCMToken() {
        coroutineScope.launch {
            val token = when (val result = repository.getFCMToken()) {
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
            Timber.d("minddddy, FCM token=${token}")
        }
    }
}