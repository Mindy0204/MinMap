package com.mindyhsu.minmap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mindyhsu.minmap.BuildConfig.MAPS_API_KEY
import com.mindyhsu.minmap.databinding.FragmentMapSearchBinding

class MapSearchFragment : Fragment() {

    private lateinit var binding: FragmentMapSearchBinding
    private lateinit var selectedLocation: Place

    private val AUTOCOMPLETE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapSearchBinding.inflate(inflater, container, false)

        binding.checkLocationButton.setOnClickListener {
            findNavController().navigate(MapsFragmentDirections.navigateToMapFragment().apply {
                endLocation = selectedLocation
            })
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Places SDK
        context?.let { Places.initialize(it, MAPS_API_KEY) }

        // Initialize the AutocompleteSupportFragment
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        // Specify the types of place data to return
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("Mindy", "Place: ${place.name}, ${place.id}, ${place.latLng}")
                selectedLocation = place
            }

            override fun onError(status: Status) {
                Log.i("Mindy", "An error occurred: $status")
            }
        })


        // Set the fields to specify which types of place data to return after the user has made a selection
//        val fields = listOf(Place.Field.ID, Place.Field.NAME)
//
//        // Start the autocomplete intent
//        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
//            .build(context)
//        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
//            when (resultCode) {
//                Activity.RESULT_OK -> {
//                    data?.let {
//                        val place = Autocomplete.getPlaceFromIntent(data)
//                        Log.i("Mindy", "Place: ${place.name}, ${place.id}")
//                    }
//                }
//                AutocompleteActivity.RESULT_ERROR -> {
//                    data?.let {
//                        val status = Autocomplete.getStatusFromIntent(data)
//                        Log.i("Mindy", status.statusMessage ?: "")
//                    }
//                }
//                Activity.RESULT_CANCELED -> {
//                    // The user canceled the operation.
//                }
//            }
//            return
//        }
//        super.onActivityResult(requestCode, resultCode, data)
//    }
}