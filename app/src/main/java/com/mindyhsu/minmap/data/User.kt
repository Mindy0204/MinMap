package com.mindyhsu.minmap.data

import com.google.firebase.firestore.GeoPoint

data class User(
    val currentEvent: String = "",
    val friends: List<String> = emptyList(),
    val geoHash: GeoPoint? = null,
    val id: String = "",
    val image: String = "",
    val name: String = "",
    val fcmToken: String = ""
)