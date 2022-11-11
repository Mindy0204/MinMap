package com.mindyhsu.minmap.map

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.BuildConfig
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
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
import kotlin.math.roundToInt

data class MapUiState(
    val onClick: (friendId: String) -> Unit
)

class MapViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

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

    var isStartNavigation: Boolean = false
    private var routeSteps = listOf<Step>()
    private var step = 0
    private var direction = "go-straight"

    private var _navigationInstruction = MutableLiveData<String>()
    val navigationInstruction: LiveData<String>
        get() = _navigationInstruction

    private var _isFinishNavigation = MutableLiveData<Boolean>()
    val isFinishNavigation: LiveData<Boolean>
        get() = _isFinishNavigation

    private var _isOnInvitation = MutableLiveData<Boolean>()
    val isOnInvitation: LiveData<Boolean>
        get() = _isOnInvitation

    private var planningLocation = LatLng(0.0, 0.0)
    private var planningLocationName = ""

    private val locationManager = MinMapApplication.instance
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
            val result = participantsIds?.let { repository.getUsersById(it) }
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
                    apiKey = BuildConfig.MAPS_API_KEY,
                    mode = "walking"
                )

                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(eventLocationLat, eventLocationLng))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_meeting_point))
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
            isStartNavigation = true
        }

    }

    private val locationListener = LocationListener {
        showNavigationInstruction(it)
        updateMyLocation(GeoPoint(it.latitude, it.longitude))
    }

    fun startNavigation() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0F,
            locationListener
        )
    }

    private fun updateMyLocation(myGeo: GeoPoint) {
        coroutineScope.launch {
            UserManager.id?.let { repository.updateMyLocation(it, myGeo) }
        }
    }

    private fun showNavigationInstruction(myLocation: Location) {
        val stepEndLocation = Location("stepEndLocation")
        stepEndLocation.latitude = routeSteps[step].endLocation.lat
        stepEndLocation.longitude = routeSteps[step].endLocation.lng

        if (step + 1 < routeSteps.size) {
            routeSteps[step + 1].maneuver?.let {
                direction = it
            }
        } else {
            direction = "go-straight"
        }

        _navigationInstruction.value =
            MinMapApplication.instance.getString(
                R.string.navigation_instruction,
                direction,
                myLocation.distanceTo(stepEndLocation).toInt().toString(),
                routeSteps[step].duration.text
            )

        // Last step
        if (step == routeSteps.size - 1 && myLocation.distanceTo(stepEndLocation).toInt() <= 15) {
            locationManager.removeUpdates(locationListener)
            _isFinishNavigation.value = true
        }

        // Other steps
        if (myLocation.distanceTo(stepEndLocation).toInt() == 0) {
            if (step < routeSteps.size - 1) {
                step++
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
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.latitude, it.longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_profile))
                        .alpha(0.7F)
                )

                marker?.let { marker -> markerList.add(marker) }
            }
        }
    }

    fun updateFriendsLocation() {
        currentEventDetail.value?.participants?.let {
            val friendsList = it.filter { it != UserManager.id }
            friends = repository.updateFriendsLocation(friendsList)
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

    fun onPlanningLocation(map: GoogleMap, latLng: LatLng, placeName: String?) {
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_meeting_point))
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

    fun sendEvent() {
        // TODO: update firebase chatRooms & users
        // TODO: Check whether need fields: time & status in Event()
        coroutineScope.launch {
            val event = Event(
                status = 0, // not finish
                participants = listOf(UserManager.id!!, "pq4eXE9vKfjZC3p37HsG"), // mock
                geoHash = GeoPoint(planningLocation.latitude, planningLocation.longitude),
                place = planningLocationName
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
            _isOnInvitation.value = false
        }
    }

    fun getMidPoint(locationList: MutableList<LatLng>): LatLng {
        var totalLat = 0.0
        var totalLon = 0.0
        val listSize = locationList.size
        for (location in locationList) {
            totalLat += location.latitude
            totalLon += location.longitude
        }
        return LatLng(totalLat / listSize, totalLon / listSize)
    }
}