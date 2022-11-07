package com.mindyhsu.minmap.data

import com.squareup.moshi.Json

data class Route(
    val bounds: Bound,
    val copyrights: String,
    val legs: List<Leg>,
    @Json(name = "overview_polyline")val overviewPolyline: Point,
    val summary: String
)

data class Bound(
    val northeast: Direction,
    val southwest: Direction
)

data class Point(
    val points: String
)