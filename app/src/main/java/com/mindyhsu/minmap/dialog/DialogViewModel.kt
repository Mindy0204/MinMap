package com.mindyhsu.minmap.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import com.mindyhsu.minmap.util.Util.getString
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class DialogUiState(
    val getSenderName: (senderId: String) -> String,
    val clickUrl: (String) -> Unit
)

class DialogViewModel(
    private val repository: MinMapRepository,
    private val chatRoomDetail: ChatRoom
) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private val selfName = UserManager.name
    private val users = chatRoomDetail.users.filter { it.name != selfName }
    private val usersDistinct = users.distinct()

    private var _roomTitle: String = ""
    val roomTitle: String
        get() = _roomTitle

    private val getLiveMessages =
        UserManager.id?.let { repository.getMessage(chatRoomDetail.id, it) }
    val messages = getLiveMessages?.let { Transformations.map(getLiveMessages) { getMessages(it) } }

    private val _midPoint = MutableLiveData<LatLng>()
    val midPoint: LiveData<LatLng>
        get() = _midPoint

    val participants = chatRoomDetail.participants

    val uiState = DialogUiState(
        getSenderName = { senderId ->
            val sender = (chatRoomDetail.users.filter { it.id == senderId }).distinct()
            sender[0].name
        },
        clickUrl = { url ->
            if (showUrl(url)) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                MinMapApplication.instance.startActivity(intent)
            }
        }
    )

    init {
        getTitleName()
    }

    /**
     * When the [ViewModel] is finished, we cancel our coroutine [viewModelJob], which tells the
     * Retrofit service to stop.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private fun getTitleName() {
        for ((index, user) in usersDistinct.withIndex()) {
            for (participant in chatRoomDetail.participants) {
                if (user.id == participant) {
                    if (index != 0) {
                        _roomTitle += ", "
                    }
                }
            }
            _roomTitle += user.name
        }
    }

    /** Get messages with text, senderId and time */
    @SuppressLint("SimpleDateFormat")
    private fun getMessages(messages: List<Message>): List<DialogItem> {
        val dataList = mutableListOf<DialogItem>()
        messages.let {
            val dateFormat = SimpleDateFormat(DATE_FORMAT)
            var lastDate = "2022-11-11"
            for (message in it) {

                // Show date
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

    fun sendMessage(text: String) {
        coroutineScope.launch {
            val time = Timestamp(Calendar.getInstance().time)
            UserManager.id?.let { userId ->
                val message = Message(
                    senderId = userId,
                    text = text,
                    time = time
                )

                status.value = LoadApiStatus.LOADING

                when (
                    val result =
                        repository.sendMessage(chatRoomId = chatRoomDetail.id, message = message)
                ) {
                    is Result.Success -> {
                        _error.value = null
                        status.value = LoadApiStatus.DONE
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        status.value = LoadApiStatus.ERROR
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        status.value = LoadApiStatus.ERROR
                    }
                    else -> {
                        _error.value =
                            getString(R.string.firebase_operation_failed)
                        status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }

    /** Query every participants' geo */
    fun getMidPoint() {
        coroutineScope.launch {
            val result = repository.getUserById(chatRoomDetail.participants)
            val userList = MutableLiveData<List<User>>()
            userList.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    null
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    null
                }
                else -> {
                    _error.value =
                        getString(R.string.firebase_operation_failed)
                    status.value = LoadApiStatus.ERROR
                    null
                }
            }

            // Participants' location
            val locationList = mutableListOf<LatLng>()
            userList.value?.let {
                for (user in it) {
                    user.geoHash?.let { geo ->
                        locationList.add(LatLng(geo.latitude, geo.longitude))
                    }
                }
            }
            calculateMidPoint(locationList)
        }
    }

    private fun calculateMidPoint(locationList: List<LatLng>) {
        var totalLat = 0.0
        var totalLon = 0.0
        val listSize = locationList.size
        for (location in locationList) {
            totalLat += location.latitude
            totalLon += location.longitude
        }
        _midPoint.value = LatLng(totalLat / listSize, totalLon / listSize)
    }

    private fun showUrl(url: String): Boolean {
        val urlFormat = URL_FORMAT
        val pattern = Pattern.compile(urlFormat)
        val matcher = pattern.matcher(url)
        return matcher.find()
    }
}
