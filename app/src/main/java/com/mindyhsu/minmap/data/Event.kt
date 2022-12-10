package com.mindyhsu.minmap.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Event(
    var id: String = "",
    val status: Int = 0,
    val participants: List<String> = emptyList(),
    val geoHash: @RawValue GeoPoint? = null,
    val place: String = MinMapApplication.instance.getString(R.string.custom_location),
    val time: Timestamp? = null
) : Parcelable
