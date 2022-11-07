package com.mindyhsu.minmap.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.Event
import com.mindyhsu.minmap.login.UserManager
import java.text.SimpleDateFormat

class CheckEventViewModel(private val eventDetail: Event) : ViewModel() {
    private val db = Firebase.firestore

    val eventLocation = eventDetail.place
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val eventTime: String = dateFormat.format(eventDetail.time?.toDate())

    private val _eventParticipant = MutableLiveData<String>()
    val eventParticipant: LiveData<String>
        get() = _eventParticipant

    init {
        getEventParticipantsName()
    }

    private fun getEventParticipantsName() {
        val userNameListWithIds = mutableMapOf<String, String>()
        var participants = ""

        db.collection("events").document(eventDetail.id).get().addOnSuccessListener { event ->
            val data = event["participants"] as List<String>
            val participantsIds = data.filter { it != UserManager.id }

                db.collection("users").whereIn("id", participantsIds)
                    .get().addOnSuccessListener { users ->
                        for (user in users) {
                            userNameListWithIds[user.data["id"] as String] =
                                user.data["name"] as String
                        }

                        for ((index, id) in participantsIds.withIndex()) {
                            if (index != 0) {
                                participants += ", "
                            }
                            participants += userNameListWithIds[id]
                        }
                        _eventParticipant.value = participants
                    }
            }
    }
}