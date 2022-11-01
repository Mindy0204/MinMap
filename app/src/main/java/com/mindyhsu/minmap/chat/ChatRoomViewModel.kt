package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.data.ChatRoom

class ChatRoomViewModel : ViewModel() {
    private val _friendList = MutableLiveData<ChatRoom>()
    val friendList: LiveData<ChatRoom>
        get() = _friendList
}