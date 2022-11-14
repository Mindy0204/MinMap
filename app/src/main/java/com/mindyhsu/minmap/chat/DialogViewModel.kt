package com.mindyhsu.minmap.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.ChatRoom
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
import java.text.SimpleDateFormat

data class DialogUiState(
    val getSenderName: (senderId: String) -> String
)

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
    private val usersDistinct = users.distinct()
    var roomTitle = ""

    private val getLiveMessages =
        UserManager.id?.let { repository.getMessage(chatRoomDetail.id, it) }
    val messages = getLiveMessages?.let { Transformations.map(getLiveMessages) { getMessages(it) } }

    private val _midPoint = MutableLiveData<LatLng>()
    val midPoint: LiveData<LatLng>
        get() = _midPoint

    val uiState = DialogUiState(
        getSenderName = { senderId ->
            val sender = (chatRoomDetail.users.filter { it.id == senderId }).distinct()
            sender[0].name
        }
    )

    init {
        getTitleName()
    }

    private fun getTitleName() {
        for ((index, user) in usersDistinct.withIndex()) {
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

    private fun getMessages(messages: List<Message>): List<DialogItem> {
        val dataList = mutableListOf<DialogItem>()
        messages.let {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            var lastDate = "2022-11-11"
            for (message in it) {
                val getDate = message.time?.toDate()?.let { date -> dateFormat.format(date) }
                if (getDate != lastDate) {
                    dataList.add(DialogItem.DialogDate(message))
                    getDate?.let {
                        lastDate = getDate
                    }
                }

                if (message.senderId != UserManager.id) {
                    dataList.add(DialogItem.FriendDialog(message))
                } else {
                    dataList.add(DialogItem.MyDialog(message))
                }
            }
        }
        return dataList
    }

    fun sendMessage(text: String, time: Timestamp) {
        coroutineScope.launch {
            UserManager.id?.let { userId ->
                val message = Message(
                    senderId = userId,
                    text = text,
                    time = time
                )

                status.value = LoadApiStatus.LOADING

                when (val result =
                    repository.sendMessage(chatRoomId = chatRoomDetail.id, message = message)) {
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

    fun getMidPoint() {
        coroutineScope.launch {
            val result = repository.getUsersById(chatRoomDetail.participants)
            val userList = MutableLiveData<List<User>>()
            userList.value = when (result) {
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

            val locationList = mutableListOf<LatLng>()
            userList.value?.let {
                for (user in it) {
                    user.geoHash?.let { geo ->
                        locationList.add(LatLng(geo.latitude, geo.longitude))
                    }
                }
            }

            var totalLat = 0.0
            var totalLon = 0.0
            val listSize = locationList.size
            for (location in locationList) {
                totalLat += location.latitude
                totalLon += location.longitude
            }
            _midPoint.value = LatLng(totalLat / listSize, totalLon / listSize)
        }
    }
}