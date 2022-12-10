package com.mindyhsu.minmap.map

import android.Manifest
import android.app.Activity
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
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
import com.mindyhsu.minmap.ext.getVmFactory
import com.mindyhsu.minmap.main.MainViewModel
import com.mindyhsu.minmap.navigationsuccess.NavigationSuccessFragmentDirections
import timber.log.Timber

class MapFragment : Fragment(), OnRequestPermissionsResultCallback, OnMapClickListener {

    private lateinit var binding: FragmentMapBinding
    private val viewModel by viewModels<MapViewModel> { getVmFactory() }

    private lateinit var map: GoogleMap

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** Mid point event passed from chatRoom */
        setFragmentResultListener(MID_POINT_EVENT_REQUEST_KEY) { _, bundle ->
            viewModel.sendEvent(
                bundle.get(MID_POINT_EVENT_LAT_LNG) as LatLng,
                bundle.get(MID_POINT_EVENT_PARTICIPANTS) as List<String>
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        /** Initialize Map */
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        /**
         * After click navigation button, the status will be changed to NAVIGATION_ING
         * When user operate back from chat room, UI and flow need to stay as same as NAVIGATION_ING status
         * */
        if (viewModel.navigationStatus.value != NAVIGATION_INIT) {
            viewModel.onNavigationPause()
        }

        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            map.uiSettings.setAllGesturesEnabled(true)
            map.uiSettings.isMapToolbarEnabled = false
            binding.sendInvitationButton.visibility = View.GONE

            enableMyLocation()
            getDeviceLocation()
            showCurrentEvent()
        }

        // Back to device location
        binding.backToPositionButton.setOnClickListener {
            enableMyLocation()
            getDeviceLocation()
        }

        /**
         * Start navigation:
         * update UI, change navigation status, update user's and friends' location
         * */
        binding.startNavigationButton.setOnClickListener {
            binding.startNavigationButton.visibility = View.GONE
            binding.cardViewText.text = getString(R.string.start_navigation_button)
            viewModel.startNavigation()
            viewModel.onNavigation()
            viewModel.updateFriendsLocation()
        }

        binding.meetingLocationButton.setOnClickListener {
            viewModel.focusOnMeetingPoint(map)
        }

        /** Friends' location show on the map and recyclerView */
        val adapter = FriendLocationAdapter(viewModel.uiState)
        binding.friendsLocationRecyclerView.adapter = adapter
        viewModel.onFriendsLiveReady.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                binding.friendsLocationRecyclerView.visibility = View.VISIBLE
                viewModel.friendList.observe(viewLifecycleOwner) {
                    viewModel.markFriendsLocation(map, it)
                    adapter.submitList(it)
                    binding.friendsCardView.visibility = View.VISIBLE
                }
            }
        }

        /**
         * Navigation instruction:
         * Text, direction image and TTS instruction
         * */
        viewModel.navigationInstruction.observe(viewLifecycleOwner) {
            binding.cardViewText.text = it[DIRECTION]
            binding.cardViewText2.visibility = View.VISIBLE
            binding.cardViewText2.text = it[DISTANCE_DURATION]
            binding.cardViewIcon.setImageResource(R.mipmap.icon_go_straight)
            binding.cardViewNextDirectionIcon.visibility = View.VISIBLE

            // TTS
            textToSpeech = TextToSpeech(
                context,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        viewModel.startTextToSpeech(textToSpeech)
                    }
                }
            )

            // Direction image
            when (viewModel.direction) {
                DIRECTION_GO_STRAIGHT -> {
                    binding.cardViewNextDirectionIcon.setImageResource(R.mipmap.icon_go_straight)
                }
                DIRECTION_TURN_RIGHT -> {
                    binding.cardViewNextDirectionIcon.setImageResource(R.mipmap.icon_turn_right)
                }
                DIRECTION_TURN_LEFT -> {
                    binding.cardViewNextDirectionIcon.setImageResource(R.mipmap.icon_turn_left)
                }
                else -> {
                    binding.cardViewNextDirectionIcon.setImageResource(R.mipmap.icon_go_straight)
                }
            }
        }

        /** Click friend's picture in recyclerView */
        viewModel.checkFriendLocation.observe(viewLifecycleOwner) {
            // Move smoothly
            val cameraPosition = CameraPosition.Builder()
                .target(it)
                .zoom(FOCUS_ZOOM)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

        viewModel.isFinishNavigation.observe(viewLifecycleOwner) {
            if (it == true) {
                viewModel.finishEvent()
                binding.friendsCardView.visibility = View.GONE
                map.clear()
                viewModel.onNavigationInit()
                findNavController().navigate(NavigationSuccessFragmentDirections.navigateToNavigationSuccessFragment())
            }
        }

        binding.sendInvitationButton.setOnClickListener {
            findNavController().navigate(
                NavigationSuccessFragmentDirections.navigateToSendEventToFriendFragment(
                    viewModel.planningLocation,
                    viewModel.planningLocationName
                )
            )
        }

        /** Planning meeting point */
        viewModel.isOnInvitation.observe(viewLifecycleOwner) {
            if (it) {
                binding.sendInvitationButton.visibility = View.VISIBLE
            } else {
                map.clear()
                binding.sendInvitationButton.visibility = View.GONE
            }
        }

        /**
         * Communicate between MainViewModel and MapViewModel
         * Exit navigation of foreground service
         * */
        val mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.foregroundStop.observe(requireActivity()) {
            if (it == true) {
                viewModel.removeLocationManager()
                mainViewModel.onForegroundUpdateStopped()
                viewModel.onNavigationInit()
            }
        }

        /** Every time navigation status change, check UI and flow */
        viewModel.navigationStatus.observe(viewLifecycleOwner) {
            showCurrentEvent()
        }

        binding.chatButton.setOnClickListener {
            findNavController().navigate(ChatRoomFragmentDirections.navigateToChatRoomFragment())
        }

        viewModel.error.observe(viewLifecycleOwner) {
            if (it != null) {
                Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun enableMyLocation() {
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = false
                return
            } else {
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
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {

                // If request is cancelled, the result arrays are empty
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getDeviceLocation()
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.location_permission),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun getDeviceLocation(): LatLng? {
        val service = context?.getSystemService(LOCATION_SERVICE) as LocationManager?
        val provider = service?.getBestProvider(Criteria(), false)
        val location = provider?.let {
            context?.let { context ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
//                    return
                } else {
                    enableMyLocation()
                    Toast.makeText(
                        context,
                        getString(R.string.location_permission),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            service.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        val latLng = location?.let { LatLng(it.latitude, it.longitude) }
        val cameraUpdate = latLng?.let {
            viewModel.updateMyLocation(GeoPoint(it.latitude, it.longitude))
            CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM)
        }
        if (cameraUpdate != null) {
            map.animateCamera(cameraUpdate)
        }
        if (latLng != null) {
            viewModel.setDeviceLocation(latLng)
        }

        return latLng
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val AUTOCOMPLETE_REQUEST_CODE = 2
    }

    /** Main entry UI and flow */
    private fun showCurrentEvent() {
        viewModel.getCurrentEventId.observe(viewLifecycleOwner) {
            if (it == "") {
                binding.createEventButton.visibility = View.VISIBLE
                binding.cardView.visibility = View.GONE
                binding.startNavigationButton.visibility = View.GONE
                binding.meetingLocationButton.visibility = View.GONE
                binding.friendsCardView.visibility = View.GONE
                binding.friendsLocationRecyclerView.visibility = View.GONE
                map.setOnMapClickListener(this)
                binding.createEventButton.setOnClickListener {
                    if (getDeviceLocation() != null) {
                        searchPlace()
                    } else {
                        enableMyLocation()
                    }
                }
                viewModel.destination.removeObservers(viewLifecycleOwner)
            } else {
                map.setOnMapClickListener(null)
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
                            viewModel.getCurrentEventLocation(map, myLocation)
                        }
                    }
                }
            }
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
                        map.clear()
                        val place = Autocomplete.getPlaceFromIntent(data)
                        place.latLng?.let { latLng ->
                            viewModel.onPlanningLocation(map, latLng, place.name)
                        }
                        Timber.i("AutoComplete search place=${place.name}")
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

    override fun onMapClick(latLng: LatLng) {
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
