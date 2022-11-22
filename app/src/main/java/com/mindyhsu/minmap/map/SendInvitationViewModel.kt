package com.mindyhsu.minmap.map

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.Event
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

data class SendInvitationUiState(
    val onClick: (userId: String) -> Unit,
    val onClickRemove: (userId: String) -> Unit
)

class SendInvitationViewModel(
    private val repository: MinMapRepository,
    private val eventLocation: LatLng,
    private var eventLocationName: String
) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>>
        get() = _userList

    private val _isInvitationSuccess = MutableLiveData<Boolean>()
    val isInvitationSuccess: LiveData<Boolean>
        get() =_isInvitationSuccess

    private val userIdList = mutableListOf<String>()
    val sendInvitationUiState = SendInvitationUiState(
        onClick = { userId ->
            userIdList.add(userId)
        },
        onClickRemove = { userId ->
            userIdList.remove(userId)
        }
    )

    init {
        getFriendList()
    }

    private fun getFriendList() {
        coroutineScope.launch {

            status.value = LoadApiStatus.LOADING

            var friendList = emptyList<String>()
            val result = repository.getFriend(UserManager.id ?: "")
            friendList = when (result) {
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
            getUserById(friendList)
        }
    }

    private fun getUserById(friendList: List<String>) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            val result = repository.getUserById(friendList)
            _userList.value = when (result) {
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
        }
    }

    fun sendEvent() {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            if (userIdList.isNotEmpty()) {
                userIdList.add(UserManager.id ?: "")

                if (eventLocationName == "") {
                    eventLocationName = MinMapApplication.instance.getString(R.string.custom_location)
                }

                val event = Event(
                    participants = userIdList,
                    geoHash = GeoPoint(eventLocation.latitude, eventLocation.longitude),
                    place = eventLocationName
                )

                val currentEventId = when (val result = repository.sendEvent(event)) {
                    is Result.Success -> {
                        error.value = null
                        status.value = LoadApiStatus.DONE
                        result.data
                    }
                    is Result.Fail -> {
                        error.value = result.error
                        status.value = LoadApiStatus.ERROR
                        ""
                    }
                    is Result.Error -> {
                        error.value = result.exception.toString()
                        status.value = LoadApiStatus.ERROR
                        ""
                    }
                    else -> {
                        error.value = MinMapApplication.instance.getString(R.string.you_know_nothing)
                        status.value = LoadApiStatus.ERROR
                        ""
                    }
                }
                updateUserCurrentEvent(currentEventId)
                updateChatRoomCurrentEvent(currentEventId)
            } else {
                _isInvitationSuccess.value = false
            }
        }
    }

    private fun updateUserCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            when (val result = repository.updateUserCurrentEvent(userIdList, currentEventId)) {
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
                    error.value = MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    private fun updateChatRoomCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            if (userIdList.size == 2) { // 1 on 1 chat room
                userIdList.sort()
            }

            when (val result =
                repository.updateChatRoomCurrentEvent(userIdList, currentEventId)) {
                is Result.Success -> {
                    error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                is Result.Error -> {
                    error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                else -> {
                    error.value = MinMapApplication.instance.getString(R.string.you_know_nothing)
                    status.value = LoadApiStatus.ERROR
                    ""
                }
            }
            _isInvitationSuccess.value = true
        }
    }
}