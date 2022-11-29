package com.mindyhsu.minmap.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager

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


}