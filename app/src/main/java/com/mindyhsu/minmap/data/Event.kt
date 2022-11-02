package com.mindyhsu.minmap.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Event(
    val id: String,
    val status: String,
    val participants: List<String>,
    val geoHash: @RawValue GeoPoint,
    val place: String,
    val time: Timestamp
): Parcelable