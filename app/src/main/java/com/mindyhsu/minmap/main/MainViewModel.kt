package com.mindyhsu.minmap.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.chat.DialogItem
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import timber.log.Timber

class MainViewModel(private val repository: MinMapRepository) : ViewModel() {

    private val _getLiveChatRoom = repository.getLiveChatRoom(UserManager.id ?: "")
    val getLiveChatRoom : LiveData<List<ChatRoom>>
    get() = _getLiveChatRoom

    private val _getChatRoomIds = MutableLiveData<List<String>>()
    val getChatRoomIds: LiveData<List<String>>
        get() = _getChatRoomIds

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


}