package com.mindyhsu.minmap.data

import com.squareup.moshi.Json

data class WayPoint(
    @Json(name = "geocoder_status")val geocoderStatus: String,
    @Json(name = "place_id")val placeId: String,
    val types: List<String>
)
