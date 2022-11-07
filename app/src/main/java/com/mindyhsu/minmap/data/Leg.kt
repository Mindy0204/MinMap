package com.mindyhsu.minmap.data

import com.squareup.moshi.Json

data class Leg(
    val distance: DistanceAndDuration,
    val duration: DistanceAndDuration,
    @Json(name = "end_address")val endAddress: String,
    @Json(name = "end_location")val endLocation: Direction,
    @Json(name = "start_address")val startAddress: String,
    @Json(name = "start_location")val startLocation: Direction,
    val steps: List<Step>,
//    @Json(name = "traffic_speed_entry")val trafficSpeedEntry: List<>,
//    @Json(name = "via_waypoint")val viaWaypoint: List<>,
)