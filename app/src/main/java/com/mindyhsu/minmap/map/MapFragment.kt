package com.mindyhsu.minmap.map

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.chat.ChatRoomFragmentDirections
import com.mindyhsu.minmap.databinding.FragmentMapBinding
import com.mindyhsu.minmap.dialog.MID_POINT_EVENT_LAT_LNG
import com.mindyhsu.minmap.dialog.MID_POINT_EVENT_PARTICIPANTS
import com.mindyhsu.minmap.dialog.MID_POINT_EVENT_REQUEST_KEY
import com.mindyhsu.minmap.ext.emptyString
import com.mindyhsu.minmap.ext.getVmFactory
import com.mindyhsu.minmap.main.MainViewModel
import com.mindyhsu.minmap.navigationsuccess.NavigationSuccessFragmentDirections
import timber.log.Timber

class MapFragment : Fragment(), OnRequestPermissionsResultCallback, OnMapReadyCallback,
    OnMapClickListener {

    private lateinit var binding: FragmentMapBinding
    private val viewModel by viewModels<MapViewModel> { getVmFactory() }
    private var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocation: LatLng? = null
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleMidPointEventResult()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.apply {
            uiSettings.setAllGesturesEnabled(true)
            uiSettings.isMapToolbarEnabled = false
            enableMyLocation()
        }

        viewModel.apply {
            if (getMidPointEventLatLng() != null) {
                sendMidPointEvent()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
        initFriendLocationRecyclerView()
    }

    private fun handleMidPointEventResult() {
        setFragmentResultListener(MID_POINT_EVENT_REQUEST_KEY) { _, bundle ->
            (bundle.get(MID_POINT_EVENT_LAT_LNG) as? LatLng)?.let { latLng ->
                Timber.d("[handleMidPointEventResult] MID_POINT_EVENT_LAT_LNG = $latLng")
                (bundle.get(MID_POINT_EVENT_PARTICIPANTS) as? List<*>)?.let { participants ->
                    Timber.d("[handleMidPointEventResult] MID_POINT_EVENT_PARTICIPANTS = $participants")

                    viewModel.setMidPointEventInfo(
                        latLng = latLng,
                        participants = mutableListOf<String>().apply {
                            participants.forEach { add(it.toString()) }
                        })
                } ?: Timber.d("[handleMidPointEventResult] MID_POINT_EVENT_PARTICIPANTS is null")
            } ?: Timber.d("[handleMidPointEventResult] MID_POINT_EVENT_LAT_LNG is null")
        }
    }

    private fun initView() {
        binding.apply {
            viewModel.apply {
                createEventButton.setOnClickListener {
                    if (myLocation != null) {
                        searchPlace()
                    } else {
                        enableMyLocation()
                    }
                }

                backToPositionButton.setOnClickListener {
                    enableMyLocation()
                }

                startNavigationButton.setOnClickListener {
                    it.visibility = View.GONE
                    cardViewText.text = getString(R.string.start_navigation_button)
                    viewModel.apply {
                        startNavigation()
                        onNavigation()
                        updateFriendsLocation()
                    }
                }

                meetingLocationButton.setOnClickListener {
                    moveCameraToLocation(currentEventDetail.value?.geoHash)
                }

                sendInvitationButton.setOnClickListener {
                    findNavController().navigate(
                        NavigationSuccessFragmentDirections.navigateToSendEventToFriendFragment(
                            viewModel.planningLocation,
                            viewModel.planningLocationName
                        )
                    )
                }

                chatButton.setOnClickListener {
                    viewModel.apply {
                        if (getOperationStatus() == OperationStatus.NAVI_ING) onNavigationPause()
                    }
                    findNavController().navigate(ChatRoomFragmentDirections.navigateToChatRoomFragment())
                }
            }
        }
    }

    private fun initObserver() {
        viewModel.apply {
            currentEventId.observe(viewLifecycleOwner) {
                Timber.d("[initObserver] observe currentEventId = $it")
                Timber.d("[initObserver] getOperationStatus= ${getOperationStatus()}")

                if (it.isNotEmpty()) {
                    when (getOperationStatus()) {
                        OperationStatus.NAVI_ING,
                        OperationStatus.NAVI_PAUSE -> onNavigation()
                        else -> {
                            onNavigationInit()
                            getCurrentEventLocation()
                        }
                    }
                } else {
                    onResetStatus()
                    map?.clear()
                }

                setupView(
                    hasCurrentEvent = it.isNotEmpty(),
                    currentNavigationStatus = viewModel.getOperationStatus()
                )
            }

            currentEventDetail.observe(viewLifecycleOwner) { event ->
                Timber.d("[initObserver] observe currentEventDetail = $event")
                binding.cardViewText.text = event.place
                moveCameraToLocation(event.geoHash)
                map?.let { getDirection(it, myLocation) }
            }

            navigationInstruction.observe(viewLifecycleOwner) {
                Timber.d("[initObserver] observe navigationInstruction = $it")
                setupNavigationInstruction(
                    direction = it[DIRECTION] ?: emptyString(),
                    distanceDuration = it[DISTANCE_DURATION] ?: emptyString()
                )
            }

            checkFriendLocation.observe(viewLifecycleOwner) {
                Timber.d("[initObserver] observe checkFriendLocation = $it")
                moveCameraToLocation(it)
            }

            isFinishNavigation.observe(viewLifecycleOwner) {
                Timber.d("[initObserver] observe isFinishNavigation = $it")
                if (it == true) {
                    setFinishEvent()
                    findNavController().navigate(NavigationSuccessFragmentDirections.navigateToNavigationSuccessFragment())
                }
            }

            isOnInvitation.observe(viewLifecycleOwner) {
                Timber.d("[initObserver] observe isOnInvitation = $it")
                if (it) {
                    binding.sendInvitationButton.visibility = View.VISIBLE
                } else {
                    map?.clear()
                    binding.sendInvitationButton.visibility = View.GONE
                }
            }

            operationStatus.observe(viewLifecycleOwner) {
                Timber.d("[initObserver] observe operationStatus = $it")
                setupView(hasCurrentEvent = hasCurrentEvent(), currentNavigationStatus = it)
            }

            error.observe(viewLifecycleOwner) {
                if (it != null) {
                    Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
                }
            }

            /**
             * Communicate between MainViewModel and MapViewModel
             * Exit navigation of foreground service
             * */
            val mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            mainViewModel.foregroundStop.observe(requireActivity()) {
                if (it == true) {
                    removeLocationManager()
                    onNavigationInit()
                    mainViewModel.onForegroundUpdateStopped()
                }
            }
        }
    }

    /** Friends' location show on the map and recyclerView */
    private fun initFriendLocationRecyclerView() {
        viewModel.apply {
            val adapter = FriendLocationAdapter(uiState)
            binding.friendsLocationRecyclerView.adapter = adapter

            onFriendsLiveReady.observe(viewLifecycleOwner) { ready ->
                if (ready) {
                    friendList.observe(viewLifecycleOwner) { participants ->
                        map?.let { markFriendsLocation(it, participants) }
                        adapter.submitList(participants)
                        binding.friendsCardView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupView(
        hasCurrentEvent: Boolean,
        currentNavigationStatus: OperationStatus
    ) {
        Timber.d("[setupView]")
        map?.setOnMapClickListener(if (hasCurrentEvent) null else this)
        binding.apply {
            createEventButton.isVisible = !hasCurrentEvent
            meetingLocationButton.isVisible = hasCurrentEvent
            sendInvitationButton.isVisible = currentNavigationStatus == OperationStatus.ON_INVITE
            startNavigationButton.isVisible = currentNavigationStatus == OperationStatus.NAVI_INIT
            cardView.isVisible = hasCurrentEvent
            cardViewIcon.apply {
                isVisible = hasCurrentEvent
                setImageResource(if (currentNavigationStatus == OperationStatus.NAVI_ING) R.mipmap.icon_go_straight else R.mipmap.icon_meeting_point_color)
            }
            cardViewText.isVisible = hasCurrentEvent
            cardViewText2.isVisible = currentNavigationStatus == OperationStatus.NAVI_ING
            cardViewNextDirectionIcon.isVisible =
                currentNavigationStatus == OperationStatus.NAVI_ING
            friendsCardView.isVisible =
                hasCurrentEvent && currentNavigationStatus == OperationStatus.NAVI_ING
        }
    }

    /**
     * Navigation instruction:
     * Text, direction image and TTS instruction
     * */
    private fun setupNavigationInstruction(direction: String, distanceDuration: String) {
        binding.apply {
            cardViewText.text = direction
            cardViewText2.visibility = View.VISIBLE
            cardViewText2.text = distanceDuration
            cardViewIcon.setImageResource(R.mipmap.icon_go_straight)
            cardViewNextDirectionIcon.visibility = View.VISIBLE

            // TTS
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    viewModel.startTextToSpeech(textToSpeech)
                }
            }

            // Direction image
            cardViewNextDirectionIcon.setImageResource(
                when (viewModel.direction) {
                    DIRECTION_GO_STRAIGHT -> {
                        R.mipmap.icon_go_straight
                    }
                    DIRECTION_TURN_RIGHT -> {
                        R.mipmap.icon_turn_right
                    }
                    DIRECTION_TURN_LEFT -> {
                        R.mipmap.icon_turn_left
                    }
                    else -> {
                        R.mipmap.icon_go_straight
                    }
                }
            )
        }
    }

    private fun setFinishEvent() {
        viewModel.finishEvent()
        viewModel.onResetStatus()
        binding.friendsCardView.visibility = View.GONE
        map?.clear()
    }

    /** Initialize Google Map Places SDK */
    private fun searchPlace() {
        context?.let {
            Places.initialize(it, decodedString)
            // Set the fields to specify which types of place data to return after the user has made a selection
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

            // Start the autocomplete intent
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(it)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        map?.let { map ->
                            map.clear()
                            val place = Autocomplete.getPlaceFromIntent(data)
                            place.latLng?.let { latLng ->
                                viewModel.onPlanningLocation(map, latLng, place.name)
                            }
                            Timber.i("AutoComplete search place=${place.name}")
                            viewModel.onInvite()
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    Timber.i("AutoComplete search place error")
                }
                Activity.RESULT_CANCELED -> {
                    Timber.i("AutoComplete search place canceled")
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun enableMyLocation() {
        Timber.d("[enableMyLocation]")
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                Timber.d("[enableMyLocation] get user's location")
                map?.apply {
                    isMyLocationEnabled = true
                    uiSettings.isMyLocationButtonEnabled = false
                    updateMyLocation()
                    return
                }
            } else {
                Timber.d("[enableMyLocation] request user's location permission")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.d("[onRequestPermissionsResult]")

        // If request is cancelled, the result arrays are empty
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("[onRequestPermissionsResult] location permission is granted")
            updateMyLocation()
        } else {
            Timber.d("[onRequestPermissionsResult] location permission is not granted")
            showRequestPermissionDialog()
        }
    }

    private fun showRequestPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.request_location_permission_title))
            .setMessage(getString(R.string.request_location_permission_description))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun updateMyLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener {
            val latitude = it.latitude
            val longitude = it.longitude
            val cameraUpdate =
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), DEFAULT_ZOOM)
            viewModel.updateMyLocation(GeoPoint(latitude, longitude))

            myLocation = LatLng(latitude, longitude)
            map?.animateCamera(cameraUpdate)
        }
    }

    private fun moveCameraToLocation(location: GeoPoint?) {
        location?.apply {
            val locationLatLng = LatLng(latitude, longitude)
            val cameraPosition = CameraPosition.Builder()
                .target(locationLatLng)
                .zoom(DEFAULT_ZOOM) // TODO: optimize with user operation zoom scale
                .build()
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    override fun onMapClick(latLng: LatLng) {
        map?.let { map ->
            map.clear()
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

            viewModel.onInvite()
            viewModel.onPlanningLocation(map, latLng, emptyString())
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val AUTOCOMPLETE_REQUEST_CODE = 2
    }
}