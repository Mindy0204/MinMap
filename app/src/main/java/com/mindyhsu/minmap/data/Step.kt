package com.mindyhsu.minmap.data

import com.squareup.moshi.Json

data class Step(
    val distance: DistanceAndDuration,
    val duration: DistanceAndDuration,
    @Json(name = "end_location")val endLocation: Direction,
    @Json(name = "html_instructions")val htmlInstructions: String,
    val maneuver: String?,
    val polyline: Point,
    @Json(name = "start_location")val startLocation: Direction,
    @Json(name = "travel_mode")val travelMode: String
)
