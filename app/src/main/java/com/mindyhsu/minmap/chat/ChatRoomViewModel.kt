package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Message

class ChatRoomViewModel : ViewModel() {
    private val _friendList = MutableLiveData<ChatRoom>()
    val friendList: LiveData<ChatRoom>
        get() = _friendList

    private val _navigateToDialog = MutableLiveData<List<Message>?>()
    val navigateToDialog: LiveData<List<Message>?>
        get() = _navigateToDialog

    fun displayDialog(message: List<Message>) {
        _navigateToDialog.value = message
    }

    fun displayDialogComplete() {
        _navigateToDialog.value = null
    }
}