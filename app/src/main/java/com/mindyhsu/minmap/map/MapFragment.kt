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
import java.util.HashMap

class MapFragment : Fragment(), OnRequestPermissionsResultCallback, OnMapReadyCallback,
    OnMapClickListener {

    private lateinit var binding: FragmentMapBinding
    private val viewModel by viewModels<MapViewModel> { getVmFactory() }
    private var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
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

        /**
         * After click navigation button, the status will be changed to NAVIGATION_ING
         * When user operate back from chat room, UI and flow need to stay as same as NAVIGATION_ING status
         * */
        if (viewModel.navigationStatus.value != NAVIGATION_INIT) {
            viewModel.onNavigationPause()
        }

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
                (bundle.get(MID_POINT_EVENT_PARTICIPANTS) as? List<*>)?.let { participants ->
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
        showCurrentEvent()
        binding.apply {
            sendInvitationButton.visibility = View.GONE

            createEventButton.setOnClickListener {
                if (viewModel.deviceLocation.value != null) {
                    searchPlace()
                } else {
                    enableMyLocation()
                }
            }

            backToPositionButton.setOnClickListener {
                enableMyLocation()
            }

            /**
             * Start navigation:
             * update UI, change navigation status, update user's and friends' location
             * */
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
                map?.let { map -> viewModel.focusOnMeetingPoint(map) }
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
                findNavController().navigate(ChatRoomFragmentDirections.navigateToChatRoomFragment())
            }
        }
    }

    private fun initObserver() {
        viewModel.apply {
            navigationInstruction.observe(viewLifecycleOwner) {
                setupNavigationInstruction(
                    direction = it[DIRECTION] ?: emptyString(),
                    distanceDuration = it[DISTANCE_DURATION] ?: emptyString()
                )
            }

            checkFriendLocation.observe(viewLifecycleOwner) {
                val cameraPosition = CameraPosition.Builder()
                    .target(it)
                    .zoom(FOCUS_ZOOM)
                    .build()
                map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }

            isFinishNavigation.observe(viewLifecycleOwner) {
                if (it == true) {
                    finishEvent()
                    onNavigationInit()
                    binding.friendsCardView.visibility = View.GONE
                    map?.clear()
                    findNavController().navigate(NavigationSuccessFragmentDirections.navigateToNavigationSuccessFragment())
                }
            }

            isOnInvitation.observe(viewLifecycleOwner) {
                if (it) {
                    binding.sendInvitationButton.visibility = View.VISIBLE
                } else {
                    map?.clear()
                    binding.sendInvitationButton.visibility = View.GONE
                }
            }

            // Every time navigation status change, check UI and flow
            navigationStatus.observe(viewLifecycleOwner) {
                showCurrentEvent()
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
        val adapter = FriendLocationAdapter(viewModel.uiState)
        binding.friendsLocationRecyclerView.adapter = adapter
        viewModel.onFriendsLiveReady.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                binding.friendsLocationRecyclerView.visibility = View.VISIBLE
                viewModel.friendList.observe(viewLifecycleOwner) {
                    map?.let { map ->
                        viewModel.markFriendsLocation(map, it)
                    }
                    adapter.submitList(it)
                    binding.friendsCardView.visibility = View.VISIBLE
                }
            }
        }
    }

    /** Main entry UI and flow */
    private fun showCurrentEvent() {
        Timber.d("[showCurrentEvent]")
        viewModel.getCurrentEventId.observe(viewLifecycleOwner) {
            if (it == "") {
                binding.createEventButton.visibility = View.VISIBLE
                binding.cardView.visibility = View.GONE
                binding.startNavigationButton.visibility = View.GONE
                binding.meetingLocationButton.visibility = View.GONE
                binding.friendsCardView.visibility = View.GONE
                binding.friendsLocationRecyclerView.visibility = View.GONE
                map?.setOnMapClickListener(this)
                viewModel.destination.removeObservers(viewLifecycleOwner)
            } else {
                map?.setOnMapClickListener(null)
                binding.createEventButton.visibility = View.GONE

                if (viewModel.navigationStatus.value == NAVIGATION_INIT ||
                    viewModel.navigationStatus.value == NAVIGATION_PAUSE
                ) {
                    viewModel.destination.observe(viewLifecycleOwner) { destination ->
                        binding.startNavigationButton.visibility =
                            if (viewModel.navigationStatus.value == NAVIGATION_INIT) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                        binding.meetingLocationButton.visibility = View.VISIBLE
                        binding.cardViewIcon.visibility = View.VISIBLE
                        binding.cardViewIcon.setImageResource(R.mipmap.icon_meeting_point_color)
                        binding.cardView.visibility = View.VISIBLE
                        binding.cardViewText.text = destination
                        binding.cardViewText2.visibility = View.GONE
                        binding.cardViewNextDirectionIcon.visibility = View.GONE
                    }

                    /** According to user's location to draw the route */
                    viewModel.deviceLocation.observe(viewLifecycleOwner) { myLocation ->
                        if (myLocation != null && viewModel.getCurrentEventId.value != "") {
                            map?.let { map -> viewModel.getCurrentEventLocation(map, myLocation) }
                        }
                    }
                }
            }
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
            viewModel.setDeviceLocation(LatLng(latitude, longitude))
            map?.animateCamera(cameraUpdate)
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

            viewModel.onPlanningLocation(map, latLng, "")
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val AUTOCOMPLETE_REQUEST_CODE = 2
    }
}
