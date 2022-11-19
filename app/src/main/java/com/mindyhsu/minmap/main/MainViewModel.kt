package com.mindyhsu.minmap.main

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.chat.DialogItem
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    private var chatRoomList = listOf<ChatRoom>()

    private val sharedPreferences =
        MinMapApplication.instance.getSharedPreferences(KEY_CHAT_ROOM, Context.MODE_PRIVATE)

    var chatRoomNum: Int?
        set(value) {
            if (value != null) {
                sharedPreferences.edit().putInt(KEY_CHAT_ROOM, value).apply()
            }
        }
        get() {
            return sharedPreferences.getInt(KEY_CHAT_ROOM, 0)
        }

//    private val getLiveMessages =
//        UserManager.id?.let { repository.getMessage(chatRoomDetail.id, it) }
//    val messages = getLiveMessages?.let {
//        Transformations.map(getLiveMessages) { getMessages(it) }
//    }

    init {
        getCurrentChatRoom()
    }

    private fun getCurrentChatRoom() {
        coroutineScope.launch {
            val result = repository.getChatRoom(UserManager.id ?: "")
            chatRoomList = when (result) {
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

            if (chatRoomNum != chatRoomList.size) {
                Intent().also { intent ->
                    intent.action = CHAT_ROOM_INTENT_FILTER
                    MinMapApplication.instance.sendBroadcast(
                        intent.putExtra(KEY_CHAT_ROOM, (chatRoomList.size - chatRoomNum!!).toString())
                    )
                }
                chatRoomNum = chatRoomList.size
            }
        }
    }


    private fun getMessages(messages: List<Message>): List<DialogItem> {
//        val dataList = mutableListOf<DialogItem>()
//        messages.let {
//            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
//            var lastDate = "2022-11-11"
//            for (message in it) {
//                val getDate = message.time?.toDate()?.let { date -> dateFormat.format(date) }
//                if (getDate != lastDate) {
//                    dataList.add(DialogItem.DialogDate(message))
//                    getDate?.let {
//                        lastDate = getDate
//                    }
//                }
//
//                if (message.senderId != UserManager.id) {
//                    dataList.add(DialogItem.FriendDialog(message))
//                } else {
//                    dataList.add(DialogItem.MyDialog(message))
//                }
//            }
//        }
//        return dataList
        return listOf()
    }
}