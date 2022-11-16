package com.mindyhsu.minmap.map

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
    private val eventLocationName: String
) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>>
        get() = _userList

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
            var friendList = emptyList<String>()
            val result = UserManager.id?.let { repository.getFriend(it) }
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
            val result = UserManager.id?.let { repository.getUserById(friendList) }
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
        // TODO: update firebase chatRooms & users
        coroutineScope.launch {
            val event = Event(
                participants = userIdList,
                geoHash = GeoPoint(eventLocation.latitude, eventLocation.longitude),
                place = eventLocationName
            )

            status.value = LoadApiStatus.LOADING

            when (val result = repository.sendEvent(event)) {
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
//            _isOnInvitation.value = false
        }
    }
}