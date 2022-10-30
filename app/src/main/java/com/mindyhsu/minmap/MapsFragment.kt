package com.mindyhsu.minmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mindyhsu.minmap.databinding.FragmentMapsBinding


class MapsFragment : Fragment(),
    OnMyLocationButtonClickListener,
    OnRequestPermissionsResultCallback {

    private lateinit var binding: FragmentMapsBinding

    private var permissionDenied = false
    private lateinit var map: GoogleMap

//    private var viewModelJob = Job()
//    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val callback = OnMapReadyCallback { googleMap ->
        val taipei = LatLng(25.0330, 121.5654)
        googleMap.addMarker(MarkerOptions().position(taipei).title("Marker in Taipei"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(taipei))
        map = googleMap
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapsBinding.inflate(inflater, container, false)



        binding.backToPosition.setOnClickListener {
//            coroutineScope.launch {
//                MinMapApi.retrofitService.getDirection()
//            }
            enableMyLocation()
            getMyLocation()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    @SuppressLint("MissingPermission")
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
                activity?.let { fragmentActivity ->
                    ActivityCompat.requestPermissions(
                        fragmentActivity, arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ), LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(context, "We don't have permission to use your location.", Toast.LENGTH_SHORT).show()
//            }
//        }



//        Toast.makeText(context, "We don't have permission to use your location.", Toast.LENGTH_SHORT).show()
//        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//
//        }
//        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
//            super.onRequestPermissionsResult(
//                requestCode,
//                permissions,
//                grantResults
//            )
//            return
//        }
//
//        if (isPermissionGranted(
//                permissions,
//                grantResults,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) || isPermissionGranted(
//                permissions,
//                grantResults,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//        ) {
//            // Enable the my location layer if the permission has been granted.
//            enableMyLocation()
//        } else {
//            // Permission was denied. Display an error message
//            // Display the missing permission error dialog when the fragments resume.
//            permissionDenied = true
//        }
//    }

    private fun getMyLocation() {

        val service = context?.getSystemService(LOCATION_SERVICE) as LocationManager?
        val provider = service?.getBestProvider(Criteria(), false)
        val location = provider?.let {
            context?.let { context ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                } else {
                    enableMyLocation()
                }
            }
            service.getLastKnownLocation(it) }
//        location?.let { getMyLocation(it.latitude, location.longitude) }

        val latLng = location?.let { LatLng(it.latitude, location.longitude) }
        val cameraUpdate = latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 15F) }
        if (cameraUpdate != null) {
            map.animateCamera(cameraUpdate)
        }
    }


    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

//class MyLocationDemoActivity : AppCompatActivity(){
//
//
//
//
//    override fun onMyLocationButtonClick(): Boolean {
//        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT)
//            .show()
//        // Return false so that we don't consume the event and the default behavior still occurs
//        // (the camera animates to the user's current position).
//        return false
//    }
//
//    override fun onMyLocationClick(location: Location) {
//        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG)
//            .show()
//    }
//

//
//    override fun onResumeFragments() {
//        super.onResumeFragments()
//        if (permissionDenied) {
//            // Permission was not granted, display error dialog.
//            showMissingPermissionError()
//            permissionDenied = false
//        }
//    }
//
//    /**
//     * Displays a dialog with error message explaining that the location permission is missing.
//     */
//    private fun showMissingPermissionError() {
//        newInstance(true).show(supportFragmentManager, "dialog")
//    }
//
//}

