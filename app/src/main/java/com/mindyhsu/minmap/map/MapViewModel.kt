package com.mindyhsu.minmap.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.speech.tts.TextToSpeech
import android.text.Html
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import com.mindyhsu.minmap.network.LoadApiStatus
import com.mindyhsu.minmap.util.Util.getString
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

data class MapUiState(
    val onFriendPicClick: (friendId: String) -> Unit
)

private const val encodedString = BuildConfig.APIKEY_MAP
val decodedString: String = String(Base64.getDecoder().decode(encodedString))

class MapViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _isMapAsync = MutableLiveData<Boolean>()
    val isMapAsync: LiveData<Boolean>
        get() = _isMapAsync

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    val getCurrentEventId = repository.getLiveEventId(UserManager.id ?: "")

    private val _deviceLocation = MutableLiveData<LatLng>()
    val deviceLocation: LiveData<LatLng>
        get() = _deviceLocation

    private val currentEventDetail = MutableLiveData<Event>()

    private val _destination = MutableLiveData<String>()
    val destination: LiveData<String>
        get() = _destination

    private var _friendList = MutableLiveData<List<User>>()
    val friendList: LiveData<List<User>>
        get() = _friendList

    val onFriendsLiveReady = MutableLiveData<Boolean>(false)

    private val _checkFriendLocation = MutableLiveData<LatLng>()

    val checkFriendLocation: LiveData<LatLng>
        get() = _checkFriendLocation

    private val markerList = mutableListOf<Marker>()

    private var participantIdList = emptyList<String>()

    private val _navigationStatus = MutableLiveData<Int>(NAVIGATION_INIT)
    val navigationStatus: LiveData<Int>
        get() = _navigationStatus

    private var routeSteps = listOf<Step>()
    private var step = 0

    private var _direction = DIRECTION_GO_STRAIGHT
    val direction: String
        get() = _direction

    private var _navigationInstruction = MutableLiveData<HashMap<String, String>>()
    val navigationInstruction: LiveData<HashMap<String, String>>
        get() = _navigationInstruction

    private var foregroundDistanceAndDuration = ""
    private var distance = -1

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
        onFriendPicClick = { friendId ->
            checkFriendsLocation(friendId)
        }
    )

    /**
     * When the [ViewModel] is finished, we cancel our coroutine [viewModelJob], which tells the
     * Retrofit service to stop.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun onMapAsync() {
        _isMapAsync.value = true
        Timber.i("onMapAsync")
    }

    fun cancelMapAsync() {
        _isMapAsync.value = false
        Timber.i("cancelMapAsync")
    }

    /**
     * Set device location after getting device location
     * When observe this location -> use this parameter to query Google Map Direction Api
     * */
    fun setDeviceLocation(latLng: LatLng) {
        _deviceLocation.value = latLng
    }

    /** Query current event information */
    fun getCurrentEventLocation(map: GoogleMap, myLocation: LatLng) {
        coroutineScope.launch {
            getCurrentEventId.value?.let {
                val result = repository.getCurrentEvent(it)
                currentEventDetail.value = when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE
                        result.data
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                    else -> {
                        _error.value =
                            getString(R.string.firebase_operation_failed)
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                }

                // Move camera to destination
                currentEventDetail.value?.geoHash?.let { destinationGeo ->
                    val location = LatLng(destinationGeo.latitude, destinationGeo.longitude)
                    map.addMarker(MarkerOptions().position(location))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
                }

                // When status is ing or pause -> continue to update friends' location
                if (_navigationStatus.value != NAVIGATION_INIT) {
                    updateFriendsLocation()
                }

                // Finish invite process
                _isOnInvitation.value = false

                _destination.value = currentEventDetail.value?.place
                getDirection(map, myLocation)
            }
        }
    }

    /** Move camera focus to meeting point */
    fun focusOnMeetingPoint(map: GoogleMap) {
        currentEventDetail.value?.geoHash?.let { geo ->
            val location = LatLng(geo.latitude, geo.longitude)
            val cameraPosition = CameraPosition.Builder()
                .target(location)
                .zoom(FOCUS_ZOOM)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    /** Query Google Map Direction Api */
    private fun getDirection(map: GoogleMap, myLocation: LatLng) {
        coroutineScope.launch {
            currentEventDetail.value?.geoHash?.let {
                val eventLocationLat = it.latitude
                val eventLocationLng = it.longitude

                val result = repository.getDirection(
                    startLocation = "${myLocation.latitude}, ${myLocation.longitude}",
                    endLocation = "$eventLocationLat, $eventLocationLng",
                    apiKey = decodedString,
                    mode = WALKING_MODE
                )

                // Mark meeting point
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(eventLocationLat, eventLocationLng))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_meeting_point_color))
                )

                val directionResult = when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE
                        result.data
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                    else -> {
                        _error.value =
                            getString(R.string.firebase_operation_failed)
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                }

                // draw the route
                if (directionResult != null) {
                    drawRoute(map, directionResult)
                }
            }

            // Set navigation status to ing
            if (_navigationStatus.value != NAVIGATION_INIT) {
                onNavigation()
            }
        }
    }

    /**
     * Draw the route
     * According to Direction Api data structure
     * */
    private fun drawRoute(map: GoogleMap, direction: MapDirection) {
        val polylineOptions = PolylineOptions()
        direction.routes.let {
            for (routeItem in it) {
                for (legItem in routeItem.legs) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                legItem.startLocation.lat,
                                legItem.startLocation.lng
                            ),
                            DEFAULT_ZOOM
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

        // Setting polyline style
        val polyline = map.addPolyline(polylineOptions)
        polyline.color = MinMapApplication.instance.getColor(R.color.lake_blue)
        polyline.startCap = RoundCap()
        polyline.endCap = RoundCap()
        polyline.jointType = JointType.ROUND
        polyline.width = POLYLINE_WIDTH
    }

    /** Open location listener */
    fun startNavigation() {
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            LOCATION_MANAGER_TIME,
            LOCATION_MANAGER_DISTANCE,
            locationListener
        )
    }

    fun updateMyLocation(myGeo: GeoPoint) {
        coroutineScope.launch {
            UserManager.id?.let { repository.updateMyLocation(it, myGeo) }
        }
    }

    /** Start navigation foreground service */
    private fun startNavigationForegroundService(
        instructionTitle: String,
        instructionContent: String
    ) {
        val serviceIntent = Intent(MinMapApplication.instance, ForegroundService::class.java)
        serviceIntent.putExtra(INSTRUCTION_TITLE, instructionTitle)
        serviceIntent.putExtra(INSTRUCTION_CONTENT, instructionContent)
        ContextCompat.startForegroundService(MinMapApplication.instance, serviceIntent)
    }

    /** End navigation foreground service */
    private fun exitNavigationForegroundService() {
        val serviceIntent = Intent(MinMapApplication.instance, ForegroundService::class.java)
        serviceIntent.putExtra(NAVIGATION_COMPLETE, NAVIGATION_COMPLETE)
        ContextCompat.startForegroundService(MinMapApplication.instance, serviceIntent)
    }

    /** Remove location listener */
    fun removeLocationManager() {
        locationManager.removeUpdates(locationListener)
    }

    /**
     * Show navigation instruction
     * According to Direction Api data structure
     * */
    private fun showNavigationInstruction(myLocation: Location) {
        if (routeSteps.isNotEmpty()) {

            // End location of every step
            val stepEndLocation = Location(STEP_END_LOCATION)
            stepEndLocation.latitude = routeSteps[step].endLocation.lat
            stepEndLocation.longitude = routeSteps[step].endLocation.lng

            /**
             * Next step instruction
             * The distance and duration need to show the information of next step
             * */
            if (step + 1 < routeSteps.size) {
                routeSteps[step + 1].maneuver?.let {
                    _direction = it

                    _direction = if (direction.contains(DIRECTION_RIGHT)) {
                        DIRECTION_TURN_RIGHT
                    } else if (direction.contains(DIRECTION_LEFT)) {
                        DIRECTION_TURN_LEFT
                    } else {
                        DIRECTION_GO_STRAIGHT
                    }
                }
            } else {
                _direction = DIRECTION_GO_STRAIGHT
            }

            /** Road instruction */
            var instruction =
                Html.fromHtml(routeSteps[step].htmlInstructions).toString().replace("\n", "")

            if (instruction.split(INSTRUCTION_SPLIT_ON).size != 1) {
                instruction = instruction.split(INSTRUCTION_SPLIT_ON).last()
            } else if (instruction.split(INSTRUCTION_SPLIT_ONTO).size != 1) {
                instruction = instruction.split(INSTRUCTION_SPLIT_ONTO).last()
            }

            if (step == routeSteps.size - 1 && instruction.split(INSTRUCTION_SPLIT_FINAL_DIRECTION)
                .isNotEmpty()
            ) {
                instruction = MinMapApplication.instance.getString(
                    R.string.destination,
                    instruction.split(INSTRUCTION_SPLIT_FINAL_DIRECTION).last()
                )
            }

            distance = myLocation.distanceTo(stepEndLocation).toInt()

            /**
             * Navigation instruction is including:
             * Road instruction and next step instruction
             * */
            _navigationInstruction.value = hashMapOf(
                DIRECTION to instruction,
                DISTANCE_DURATION to MinMapApplication.instance.getString(
                    R.string.navigation_distance_and_duration,
                    myLocation.distanceTo(stepEndLocation).toInt().toString(),
                    routeSteps[step].duration.text
                )
            )

            /**
             * Foreground service notification is including:
             * Road instruction and next step instruction
             * */
            foregroundDistanceAndDuration = MinMapApplication.instance.getString(
                R.string.navigation_foreground,
                myLocation.distanceTo(stepEndLocation).toInt().toString(),
                routeSteps[step].duration.text,
                direction
            )
            startNavigationForegroundService(
                instruction,
                foregroundDistanceAndDuration
            )

            /**
             * Last step:
             * Remove location manager, foreground service
             * */
            val finalStepLocation = Location(FINAL_STEP_LOCATION)
            finalStepLocation.latitude = routeSteps[routeSteps.size - 1].endLocation.lat
            finalStepLocation.longitude = routeSteps[routeSteps.size - 1].endLocation.lng

            if (step == routeSteps.size - 1 && myLocation.distanceTo(stepEndLocation)
                .toInt() <= METERS_FROM_THE_LAST_STEP
            ) {
                locationManager.removeUpdates(locationListener)
                _isFinishNavigation.value = true
                exitNavigationForegroundService()
            } else if (myLocation.distanceTo(finalStepLocation).toInt() <= METERS_FROM_THE_LAST_STEP) {
                locationManager.removeUpdates(locationListener)
                _isFinishNavigation.value = true
                exitNavigationForegroundService()
            }

            /**
             * Other step:
             * step++ after user arrive the end location of the step
             * */
            if (myLocation.distanceTo(stepEndLocation).toInt() == 0) {
                if (step < routeSteps.size - 1) {
                    step++
                }
            }
        }
    }

    /** Google text to speech */
    fun startTextToSpeech(textToSpeech: TextToSpeech) {
        textToSpeech.stop()

        // Voice reminder every x meters
        if ((distance % REMINDER_DURATION) == 0) {
            Timber.d(TEXT_TO_SPEECH_MESSAGE + "$distance")
            textToSpeech.language = Locale.ENGLISH
            textToSpeech.setSpeechRate(SPEECH_RATE)
            textToSpeech.speak(foregroundDistanceAndDuration, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun markFriendsLocation(map: GoogleMap, users: List<User>) {
        /** Remove old marker before draw new */
        if (markerList.isNotEmpty()) {
            for (i in 0 until markerList.size) {
                markerList[0].remove()
                markerList.removeAt(0)
            }
        }

        /** Add bitmap marker on the map */
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

                            marker?.let { markerList.add(marker) }
                        }
                    })
            }
        }
    }

    /** Update friends' location */
    fun updateFriendsLocation() {
        currentEventDetail.value?.participants?.let {
            val friendListWithoutMe = it.filter { it != UserManager.id }
            _friendList = repository.updateFriendLocation(friendListWithoutMe)
            onFriendsLiveReady.value = true
        }
    }

    private fun checkFriendsLocation(friendId: String) {
        _friendList.value?.let { users ->
            for (user in users) {
                if (user.id == friendId) {
                    user.geoHash?.let {
                        _checkFriendLocation.value = LatLng(it.latitude, it.longitude)
                    }
                }
            }
        }
    }

    /**
     * Clear participants' and chatRoom's current event
     * */
    fun finishEvent() {
        coroutineScope.launch {
            _isFinishNavigation.value = false

            val chatRoomId = when (
                val result =
                    repository.getChatRoomByCurrentEventId(getCurrentEventId.value ?: "")
            ) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    ""
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    ""
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    _status.value = LoadApiStatus.ERROR
                    ""
                }
            }

            repository.finishEvent(UserManager.id ?: "", getCurrentEventId.value ?: "", chatRoomId)
        }
    }

    /** Store planning location and location name in order to pass to send invitation fragment */
    fun onPlanningLocation(map: GoogleMap, latLng: LatLng, placeName: String?) {
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_meeting_point_color))
        )

        val cameraPosition = CameraPosition.Builder()
            .target(latLng)
            .zoom(FOCUS_ZOOM)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        planningLocation = latLng
        placeName?.let {
            planningLocationName = it
        }
        _isOnInvitation.value = true
        Timber.d("Planning location=$planningLocation, name=$planningLocationName")
    }

    /**
     * Connect with find mid point event from chat room
     * After receive data -> create new event
     * */
    fun sendEvent(midPointLocation: LatLng, participantList: List<String>) {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            participantIdList = participantList

            val event = Event(
                participants = participantList,
                geoHash = GeoPoint(midPointLocation.latitude, midPointLocation.longitude)
            )

            val currentEventId = when (val result = repository.sendEvent(event)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    ""
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    ""
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    _status.value = LoadApiStatus.ERROR
                    ""
                }
            }
            updateUserCurrentEvent(currentEventId)
            updateChatRoomCurrentEvent(currentEventId)
        }
    }

    /** Update all participants' current event */
    private fun updateUserCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            when (
                val result =
                    repository.updateUserCurrentEvent(participantIdList, currentEventId)
            ) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    /**
     * Update known chatRoom's current event
     * */
    private fun updateChatRoomCurrentEvent(currentEventId: String) {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            when (
                val result =
                    repository.updateChatRoomCurrentEvent(participantIdList, currentEventId)
            ) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                }
                else -> {
                    _error.value = getString(R.string.firebase_operation_failed)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    fun onNavigationInit() {
        _navigationStatus.value = NAVIGATION_INIT
    }
    fun onNavigation() {
        _navigationStatus.value = NAVIGATION_ING
    }
    fun onNavigationPause() {
        _navigationStatus.value = NAVIGATION_PAUSE
    }
}
