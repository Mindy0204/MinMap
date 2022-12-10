package com.mindyhsu.minmap.sendinvitation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.Event
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.data.Result
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import com.mindyhsu.minmap.util.Util.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

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

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>>
        get() = _userList

    private val _isInvitationSuccess = MutableLiveData<Boolean>()
    val isInvitationSuccess: LiveData<Boolean>
        get() = _isInvitationSuccess

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

    /** Query user's friend list */
    private fun getFriendList() {
        coroutineScope.launch {

            status.value = LoadApiStatus.LOADING

            var friendList = emptyList<String>()
            val result = repository.getFriend(UserManager.id ?: "")
            friendList = when (result) {
                is Result.Success -> {
                    _error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
            }
            getUserById(friendList)
        }
    }

    /** Query information of user's friends */
    private fun getUserById(friendList: List<String>) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            val result = repository.getUserById(friendList)
            _userList.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    status.value = LoadApiStatus.ERROR
                    emptyList()
                }
            }
        }
    }

    /** Create new event to firebase */
    fun sendEvent() {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            if (userIdList.isNotEmpty()) {
                userIdList.add(UserManager.id ?: "")

                if (eventLocationName == "") {
                    eventLocationName = getString(R.string.custom_location)
                }

                val event = Event(
                    participants = userIdList,
                    geoHash = GeoPoint(eventLocation.latitude, eventLocation.longitude),
                    place = eventLocationName
                )

                val currentEventId = when (val result = repository.sendEvent(event)) {
                    is Result.Success -> {
                        _error.value = null
                        status.value = LoadApiStatus.DONE
                        result.data
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        status.value = LoadApiStatus.ERROR
                        ""
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        status.value = LoadApiStatus.ERROR
                        ""
                    }
                    else -> {
                        _error.value = getString(R.string.firebase_operation_failed)
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

    /** Update all participants' current event */
    private fun updateUserCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            when (val result = repository.updateUserCurrentEvent(userIdList, currentEventId)) {
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
                    _error.value = getString(R.string.firebase_operation_failed)
                    status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    /**
     * Group meeting -> create chatRoom
     * 1 on 1 meeting -> only update chatRoom's current event
     * */
    private fun updateChatRoomCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            if (userIdList.size == 2) { // 1 on 1 chat room
                userIdList.sort()
            }

            val chatRoomId =
                when (val result = repository.updateChatRoomCurrentEvent(userIdList, currentEventId))
            {
                is Result.Success -> {
                    _error.value = null
                    status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    status.value = LoadApiStatus.ERROR
                    ""
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    status.value = LoadApiStatus.ERROR
                    ""
                }
            }
            sendEventMessage(chatRoomId)
        }
    }

    /** Send message to notify new event */
    private fun sendEventMessage(chatRoomId: String) {
        coroutineScope.launch {
            val time = Timestamp(Calendar.getInstance().time)
            val message = Message(
                senderId = UserManager.id ?: "",
                text = getString(R.string.send_invitation_success_message),
                time = time
            )

            status.value = LoadApiStatus.LOADING

            when (val result =
                repository.sendMessage(chatRoomId = chatRoomId, message = message)) {
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
                    _error.value = getString(R.string.firebase_operation_failed)
                    status.value = LoadApiStatus.ERROR
                }
            }
            _isInvitationSuccess.value = true
        }
    }
}
