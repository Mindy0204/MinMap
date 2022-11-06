package com.mindyhsu.minmap.map

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.login.UserManager

class NavigationSuccessViewModel : ViewModel() {
    private val db = Firebase.firestore

    fun finishEvent() {
        val docRef = db.collection("users").document(UserManager.id)
        val remove = hashMapOf<String, Any>(
            "currentEvent" to FieldValue.arrayRemove("374mZOTabC32lMBXJFLn")
        )
        docRef.update(remove)
    }
}