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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

class MapViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val status = MutableLiveData<LoadApiStatus>()
    private val error = MutableLiveData<String?>()

    private val getCurrentEventId = repository.getLiveEventId(UserManager.id)
    val currentEventId = Transformations.map(getCurrentEventId) { it }

    private val _currentEventDetail = MutableLiveData<Event>()
    val currentEventDetail: LiveData<Event>
        get() = _currentEventDetail

    var isStartNavigation: Boolean = false
    private var routeSteps = listOf<Step>()
    var step = 0

    private var _navigationInstruction = MutableLiveData<String>()
    val navigationInstruction: LiveData<String>
        get() = _navigationInstruction

    private var _isFinishNavigation = MutableLiveData<Boolean>()
    val isFinishNavigation: LiveData<Boolean>
        get() = _isFinishNavigation

    private var _isOnInvitation = MutableLiveData<Boolean>()
    val isOnInvitation: LiveData<Boolean>
        get() = _isOnInvitation

    private val locationManager = MinMapApplication.instance
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun getCurrentEventLocation(map: GoogleMap) {
        coroutineScope.launch {
            currentEventId.value?.let {
                val result = repository.getCurrentEvent(it)
                _currentEventDetail.value = when (result) {
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

                _currentEventDetail.value?.geoHash?.let { geo ->
                    val location = LatLng(geo.latitude, geo.longitude)
                    map.addMarker(MarkerOptions().position(location))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
                }
            }
        }
    }

    fun onPlanningLocation(map: GoogleMap, latLng: LatLng) {
        val location = LatLng(latLng.latitude, latLng.longitude)
        map.addMarker(MarkerOptions().position(location))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
        _isOnInvitation.value = true
    }

    fun getRoute(map: GoogleMap, myLocation: LatLng) {
        coroutineScope.launch {
            currentEventId.value?.let {
                val result = repository.getCurrentEvent(it)
                _currentEventDetail.value = when (result) {
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
            }
            getDirection(map, myLocation)
        }
    }

    private fun getDirection(map: GoogleMap, myLocation: LatLng) {
        coroutineScope.launch {
            _currentEventDetail.value?.geoHash?.let {
                val eventLocationLat = it.latitude
                val eventLocationLng = it.longitude

                val result = repository.getDirection(
                    startLocation = "${myLocation.latitude}, ${myLocation.longitude}",
                    endLocation = "$eventLocationLat, $eventLocationLng",
                    apiKey = BuildConfig.MAPS_API_KEY,
                    mode = "walking"
                )

                map.addMarker(MarkerOptions().position(LatLng(eventLocationLat, eventLocationLng)))

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
                            }
                        }
                    }
                }
                map.addPolyline(polylineOptions)
            }
            isStartNavigation = false
        }
    }

    private val locationListener = LocationListener {
        showRouteGuide(it)
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
            repository.updateMyLocation(UserManager.id, myGeo)
        }
    }

    private fun showRouteGuide(myLocation: Location) {
        val stepEndLocation = Location("stepEndLocation")
        stepEndLocation.latitude = routeSteps[step].endLocation.lat
        stepEndLocation.longitude = routeSteps[step].endLocation.lng

        if (myLocation.distanceTo(stepEndLocation) <= 20) { // 20 meter
            if (step != routeSteps.size - 1) {
                step += 1
            } else {
                locationManager.removeUpdates(locationListener)
                _isFinishNavigation.value = true
            }

            var direction = ""
            routeSteps[step].maneuver?.let {
                direction = "Direction: $it"
            }
            _navigationInstruction.value =
                direction + "\nDuration: " + routeSteps[step].duration.text
        }
    }

    fun sendEvent(latLng: LatLng) {
        coroutineScope.launch {
            val event = Event(
                status = 0, // not finish
                participants = listOf("Mindy", "Wayne"),
                geoHash = GeoPoint(latLng.latitude, latLng.longitude)
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