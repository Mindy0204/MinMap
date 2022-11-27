package com.mindyhsu.minmap.ar

import android.Manifest
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.model.LatLng
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ux.ArFragment
import com.google.maps.android.data.Geometry
import timber.log.Timber

class PlacesArFragment : ArFragment() {

    override fun getAdditionalPermissions(): Array<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            .toTypedArray()

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setOnTapArPlaneListener { hitResult, _, _ ->
//
//            val latLng = LatLng(25.0377771, 121.5326929)
//            val schoolLatLng = LatLng(25.0380789, 121.5326495)
//
//            val anchor = hitResult.createAnchor()
//            val anchorNode = AnchorNode(anchor)
//            anchorNode?.setParent(this.arSceneView.scene)
//            val place = Place(
//                "id",
//                "icon",
//                "name",
//                Geometry(GeometryLocation(latLng.latitude, latLng.longitude))
//            )
//            val placeNode = PlaceNode(requireContext(), place)
//            placeNode.setParent(anchorNode)
//            placeNode.localPosition =
//                place.getPositionVector(0f, LatLng(schoolLatLng.latitude, schoolLatLng.longitude))
//        }
//    }

}