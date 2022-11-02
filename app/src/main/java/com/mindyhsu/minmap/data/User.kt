package com.mindyhsu.minmap.data

import com.google.firebase.firestore.GeoPoint

data class User(
    val currentEvent: List<String>,
    val friends: List<String>,
    val geoHash: GeoPoint,
    val id: String,
    val image: String,
    val name: String
)