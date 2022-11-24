package com.mindyhsu.minmap.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.text.Html
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.*
import com.mindyhsu.minmap.data.*
import com.mindyhsu.minmap.data.Step
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.mindyhsu.minmap.network.LoadApiStatus
import timber.log.Timber

data class MapUiState(
    val onClick: (friendId: String) -> Unit
)

const val NAVIGATION_INIT = 0
const val NAVIGATION_ING = 1
const val NAVIGATION_PAUSE = 2

const val DIRECTION_GO_STRAIGHT = "Go Straight"
const val DIRECTION_TURN_RIGHT = "Turn Right"
const val DIRECTION_TURN_LEFT = "Turn Left"

class MapViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    val deviceLocation = MutableLiveData<LatLng>()

    private val getCurrentEventId = UserManager.id?.let { repository.getLiveEventId(it) }
    val currentEventId = getCurrentEventId?.let { Transformations.map(getCurrentEventId) { it } }

    private val currentEventDetail = MutableLiveData<Event>()

    val currentEventDisplay = MutableLiveData<HashMap<String, String>>()

    private val _userList = MutableLiveData<List<User>>()

    var friends = MutableLiveData<List<User>>()
    val onFriendsLiveReady = MutableLiveData<Boolean>(false)

    private val _checkFriendLocation = MutableLiveData<LatLng>()
    val checkFriendLocation: LiveData<LatLng>
        get() = _checkFriendLocation

    private val markerList = mutableListOf<Marker>()

    private var participantIdList = emptyList<String>()

    var navigationStatus: Int = NAVIGATION_INIT

    private var routeSteps = listOf<Step>()
    private var step = 0
    var direction = DIRECTION_GO_STRAIGHT

    private var _navigationInstruction = MutableLiveData<HashMap<String, String>>()
    val navigationInstruction: LiveData<HashMap<String, String>>
        get() = _navigationInstruction

    private var _isFinishNavigation = MutableLiveData<Boolean>()
    val isFinishNavigation: LiveData<Boolean>
        get() = _isFinishNavigation

    private var _isOnInvitation = MutableLiveData<Boolean>()
    val isOnInvitation: LiveData<Boolean>
        get() = _isOnInvitation

    var planningLocation = LatLng(0.0, 0.0)
    var planningLocationName = ""

    private val locationManager = MinMapApplication.instance
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationListener = LocationListener {
        showNavigationInstruction(it)
        updateMyLocation(GeoPoint(it.latitude, it.longitude))
    }

    val uiState = MapUiState(
        onClick = { friendId ->
            checkFriendsLocation(friendId)
        }
    )

    fun getCurrentEventLocation(map: GoogleMap, myLocation: LatLng) {
        coroutineScope.launch {
            currentEventId?.value?.let {
                val result = repository.getCurrentEvent(it)
                currentEventDetail.value = when (result) {
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

                currentEventDetail.value?.geoHash?.let { geo ->
                    val location = LatLng(geo.latitude, geo.longitude)
                    map.addMarker(MarkerOptions().position(location))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
                }

                if (navigationStatus != NAVIGATION_INIT) {
                    updateFriendsLocation()
                }

                _isOnInvitation.value = false
                displayEventDetail()
                getDirection(map, myLocation)
            }
        }
    }

    fun focusOnMeetingPoint(map: GoogleMap) {
        currentEventDetail.value?.geoHash?.let { geo ->
            val location = LatLng(geo.latitude, geo.longitude)
            val cameraPosition = CameraPosition.Builder()
                .target(location)
                .zoom(16F)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun displayEventDetail() {
        val participantsIds =
            currentEventDetail.value?.participants?.filter { it != UserManager.id }

        val userNameList = mutableListOf<String>()
        coroutineScope.launch {
            val result = participantsIds?.let { repository.getUserById(it) }
            _userList.value = when (result) {
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

            _userList.value?.let {
                for (user in it) {
                    userNameList.add(user.name)
                }
            }

            var currentEventParticipants = ""
            for ((index, userName) in userNameList.withIndex()) {
                if (index != 0) {
                    currentEventParticipants += ", "
                }
                currentEventParticipants += userName
            }

            currentEventDetail.value?.let {
                val data = hashMapOf(
                    "place" to it.place,
                    "participants" to currentEventParticipants
                )
                currentEventDisplay.value = data
            }
        }
    }

    private fun getDirection(map: GoogleMap, myLocation: LatLng) {
        coroutineScope.launch {
            currentEventDetail.value?.geoHash?.let {
                val eventLocationLat = it.latitude
                val eventLocationLng = it.longitude

                val result = repository.getDirection(
                    startLocation = "${myLocation.latitude}, ${myLocation.longitude}",
                    endLocation = "$eventLocationLat, $eventLocationLng",
                    apiKey = BuildConfig.APIKEY_MAP,
                    mode = "walking"
                )

                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(eventLocationLat, eventLocationLng))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_meeting_point_color))
                )

                val directionResult = when (result) {
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

                // draw the route
                val polylineOptions = PolylineOptions()
                directionResult?.routes?.let {
                    for (routeItem in it) {
                        for (legItem in routeItem.legs) {
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        legItem.startLocation.lat,
                                        legItem.startLocation.lng
                                    ), 15F
                                )
                            )
                            routeSteps = legItem.steps

                            for (stepItem in legItem.steps) {
                                polylineOptions.add(
                                    LatLng(
                                        stepItem.startLocation.lat,
                                        stepItem.startLocation.lng
                                    )
                                )
                                if (stepItem == legItem.steps.last()) {
                                    polylineOptions.add(
                                        LatLng(
                                            stepItem.endLocation.lat,
                                            stepItem.endLocation.lng
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                map.addPolyline(polylineOptions)
            }
            if (navigationStatus != NAVIGATION_INIT) {
                navigationStatus = NAVIGATION_ING
            }
        }

    }

    fun startNavigation() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0F,
            locationListener
        )
    }

    fun updateMyLocation(myGeo: GeoPoint) {
        coroutineScope.launch {
            UserManager.id?.let { repository.updateMyLocation(it, myGeo) }
        }
    }

    private fun startNavigationForegroundService(
        instructionTitle: String,
        instructionContent: String
    ) {
        val serviceIntent = Intent(MinMapApplication.instance, ForegroundService::class.java)
        serviceIntent.putExtra("instructionTitle", instructionTitle)
        serviceIntent.putExtra("instructionContent", instructionContent)
        ContextCompat.startForegroundService(MinMapApplication.instance, serviceIntent)
    }

    private fun exitNavigationForegroundService() {
        val serviceIntent = Intent(MinMapApplication.instance, ForegroundService::class.java)
        serviceIntent.putExtra("navigationComplete", "Navigation Complete")
        ContextCompat.startForegroundService(MinMapApplication.instance, serviceIntent)
    }

    private fun showNavigationInstruction(myLocation: Location) {
        if (routeSteps.isNotEmpty()) {
            val stepEndLocation = Location("stepEndLocation")
            stepEndLocation.latitude = routeSteps[step].endLocation.lat
            stepEndLocation.longitude = routeSteps[step].endLocation.lng

            // Show next direction
            if (step + 1 < routeSteps.size) {
                routeSteps[step + 1].maneuver?.let {
                    direction = it

                    direction = if (direction.contains("right")) {
                        DIRECTION_TURN_RIGHT
                    } else if (direction.contains("left")) {
                        DIRECTION_TURN_LEFT
                    } else {
                        DIRECTION_GO_STRAIGHT
                    }
                }
            } else {
                direction = DIRECTION_GO_STRAIGHT
            }

            var instruction = Html.fromHtml(routeSteps[step].htmlInstructions).toString()

            if (instruction.split("on ").size != 1) {
                instruction = instruction.split("on ").last()
            } else if (instruction.split("onto ").size != 1) {
                instruction = instruction.split("onto ").last()
            }

            if (step == routeSteps.size - 1 && instruction.split("Destination will be on the ")
                    .isNotEmpty()
            ) {
                instruction = MinMapApplication.instance.getString(
                    R.string.destination,
                    instruction.split("Destination will be on the ").last()
                )
            }

            _navigationInstruction.value = hashMapOf(
                "direction" to instruction,
                "distanceAndDuration" to MinMapApplication.instance.getString(
                    R.string.navigation_distance_and_duration,
                    myLocation.distanceTo(stepEndLocation).toInt().toString(),
                    routeSteps[step].duration.text
                )
            )

            startNavigationForegroundService(
                instruction,
                MinMapApplication.instance.getString(
                    R.string.navigation_foreground,
                    myLocation.distanceTo(stepEndLocation).toInt().toString(),
                    routeSteps[step].duration.text,
                    direction
                )
            )

            val finalStepLocation = Location("finalStepLocation")
            finalStepLocation.latitude = routeSteps[routeSteps.size - 1].endLocation.lat
            finalStepLocation.longitude = routeSteps[routeSteps.size - 1].endLocation.lng

            // Last step
            if (step == routeSteps.size - 1 && myLocation.distanceTo(stepEndLocation)
                    .toInt() <= 15
            ) {
                locationManager.removeUpdates(locationListener)
                _isFinishNavigation.value = true
                exitNavigationForegroundService()
            } else if (myLocation.distanceTo(finalStepLocation).toInt() <= 15) {
                locationManager.removeUpdates(locationListener)
                _isFinishNavigation.value = true
                exitNavigationForegroundService()
            }

            // Other steps
            if (myLocation.distanceTo(stepEndLocation).toInt() == 0) {
                if (step < routeSteps.size - 1) {
                    step++
                }
            }
        }
    }

    fun markFriendsLocation(map: GoogleMap, users: List<User>) {
        if (markerList.size != 0) {
            for (i in 0 until markerList.size) {
                markerList[0].remove()
                markerList.removeAt(0)
            }
        }

        for (user in users) {
            user.geoHash?.let {

                Glide.with(MinMapApplication.instance)
                    .asBitmap()
                    .load(user.image).circleCrop()
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {

                            val marker = map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(it.latitude, it.longitude))
                                    .icon(
                                        BitmapDescriptorFactory.fromBitmap(resource)
                                    )
                            )

                            marker?.let { marker -> markerList.add(marker) }
                        }
                    })
            }
        }
    }

    fun updateFriendsLocation() {
        currentEventDetail.value?.participants?.let {
            val friendsList = it.filter { it != UserManager.id }
            friends = repository.updateFriendLocation(friendsList)
            onFriendsLiveReady.value = true
        }
    }

    private fun checkFriendsLocation(friendId: String) {
        friends.value?.let { user ->
            for (user in user) {
                if (user.id == friendId) {
                    user.geoHash?.let {
                        _checkFriendLocation.value = LatLng(it.latitude, it.longitude)
                    }
                }
            }
        }
    }

    fun finishEvent() {
        coroutineScope.launch {
            _isFinishNavigation.value = false

            val chatRoomId = when (val result =
                repository.getChatRoomByCurrentEventId(getCurrentEventId?.value ?: "")) {
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

            repository.finishEvent(UserManager.id ?: "", getCurrentEventId?.value ?: "", chatRoomId)
        }
    }

    fun onPlanningLocation(map: GoogleMap, latLng: LatLng, placeName: String?) {
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_meeting_point_color))
        )

        val cameraPosition = CameraPosition.Builder()
            .target(latLng)
            .zoom(15F)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        planningLocation = latLng
        placeName?.let {
            planningLocationName = it
        }
        _isOnInvitation.value = true
        Timber.d("planningLocation=$planningLocation, planningLocationName=$planningLocationName")
    }

    fun sendEvent(midPointLocation: LatLng, participantList: List<String>) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            participantIdList = participantList

            val event = Event(
                participants = participantList,
                geoHash = GeoPoint(midPointLocation.latitude, midPointLocation.longitude)
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
        }
    }

    private fun updateUserCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            status.value = LoadApiStatus.LOADING

            when (val result =
                repository.updateUserCurrentEvent(participantIdList, currentEventId)) {
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

            when (val result =
                repository.updateChatRoomCurrentEvent(participantIdList, currentEventId)) {
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
        }
    }
}