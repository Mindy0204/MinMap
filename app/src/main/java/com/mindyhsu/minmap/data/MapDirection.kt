package com.mindyhsu.minmap.data

import com.squareup.moshi.Json

data class MapDirection(
    @Json(name = "geocoded_waypoints")val geocodedWaypoints: List<WayPoint>,
    val routes: List<Route>,
    val status: String
)
