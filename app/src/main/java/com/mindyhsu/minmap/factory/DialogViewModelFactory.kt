package com.mindyhsu.minmap.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindyhsu.minmap.chat.DialogViewModel
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.source.MinMapRepository

class DialogViewModelFactory(
    private val repository: MinMapRepository,
    private val chatRoomDetail: ChatRoom
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DialogViewModel::class.java)) {
            return DialogViewModel(repository, chatRoomDetail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}